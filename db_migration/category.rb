require './helpers.rb'

class Category

  EN_MAIN_CATEGORY_IDS = [ 4892515, 690747, 696603, 1633936, 695027, 8017451, 771152, 694871, 3103170, 1004110, 691928,
    693800, 1013214, 956054, 2389032, 691182, 691008, 693708, 692348, 751381, 696763, 696648, 694861, 24980271, 693555, 1784082 ]
  ZH_MAIN_CATEGORY_IDS = [ 33578, 33580, 34616, 41219, 41425, 42318, 42322, 42326, 42334, 42370, 42371, 42492, 43939,
    51575, 56002, 65708, 65994, 194858, 258453, 320998, 414585, 454712, 455423, 464578, 770007 ]

  TOTAL_LEVELS = 7
  def self.do force
    add_level_column(force)
    populate_main_category_subcategories_table(force)
  end

  def self.add_level_column force
    puts "Add level column to pages"
    # create level field in the page table
    dest_db = SQL.connect("dest")
    has_level_column = false
    dest_db.query(%[ SHOW COLUMNS FROM pages ]).each do |row|
      if row["Field"] == "level"
        has_level_column = true
        break
      end
    end
    if has_level_column
      if !force
        puts "  -- skipped. Pages table already has level column."
        return
      else
        puts "  -- drop column pages.level"
        dest_db.query(%[ ALTER TABLE pages DROP COLUMN `level`; ])
      end
    end

    puts "  -- add column pages.level"
    dest_db.query(%[ ALTER TABLE pages ADD `level` int(3) unsigned, ADD INDEX `page_level` (`level`); ])

    # find levels for the main categories
    [{ids: EN_MAIN_CATEGORY_IDS, language: 0}, {ids: ZH_MAIN_CATEGORY_IDS, language: 1}].each do |data|
      category_ids = data[:ids]
      language = data[:language]
      for level in 1..TOTAL_LEVELS
        puts "  -- Update #{category_ids.count} categories for language #{language} to level #{level}"
        dest_db.query(%[ UPDATE pages SET level = #{level}
          WHERE level IS NULL and language = #{language} AND id IN (#{category_ids.join(",")})])
        subcategory_ids = dest_db.query(%[ SELECT subcategory_id from category_subcategories
          WHERE language = #{language} AND category_id IN (#{category_ids.join(",")}) ]).map{|row| row["subcategory_id"]}
        category_ids = subcategory_ids
      end
    end
    # find next 3 levels
  end

  EN_NAME_OF_HIDDEN_CATEGORY = "Hidden_categories"
  ZH_NAME_OF_HIDDEN_CATEGORY = "隐藏分类"
  def self.hidden_category_ids lang
    @@hidden_category_ids ||= {}
    @@hidden_category_ids[lang] ||= begin
      name, db = {
        0 => [EN_NAME_OF_HIDDEN_CATEGORY, SQL.connect("en_source")],
        1 => [ZH_NAME_OF_HIDDEN_CATEGORY, SQL.connect("zh_source")]
      }[lang]
      db.query(%[ SELECT cl_from from categorylinks WHERE cl_to = "#{name}";]).map{|r| r["cl_from"]}
    end

    return @@hidden_category_ids[lang]
  end

  def self.populate_main_category_subcategories_table force
    dest_db = SQL.connect("dest")
    if SQL.check_table_existence(["main_category_subcategories"], dest_db)
      if !force
        puts "  -- skipped. Dest db already has these tables main_category_subcategories"
        return
      else
        SQL.drop_tables(["main_category_subcategories"], dest_db)
      end
    end

    # create table
    dest_db.query("CREATE TABLE main_category_subcategories(
      `l1_category_id` int(10) unsigned NOT NULL,
      `l2_category_id` int(10) unsigned NOT NULL,
      `language` int(3) unsigned NOT NULL DEFAULT '0',
      `level` int(3) unsigned NOT NULL,
      `subcategory_id` int(10) unsigned NOT NULL,
      UNIQUE KEY `category_id_and_subcategory_id_language_level` (`l2_category_id`, `subcategory_id`, `language`),
      KEY `subcategory_level_language_subcategory_id` (`subcategory_id`, `language`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;")

    en_l2_category_ids = dest_db.query("SELECT id FROM pages WHERE level = 2 AND language = 0").map{|p| p["id"]}
    zh_l2_category_ids = dest_db.query("SELECT id FROM pages WHERE level = 2 AND language = 1").map{|p| p["id"]}

    # create for two level
    [[EN_MAIN_CATEGORY_IDS, en_l2_category_ids, 0, 1], [ZH_MAIN_CATEGORY_IDS, zh_l2_category_ids, 1, 1]].each do |data|
      main_category_ids, category_ids, language, main_category_level = data

      l2_category_map_l1_cateogry_id = {}
      dest_db.query(%[ SELECT category_id, subcategory_id FROM category_subcategories
        WHERE category_id IN (#{main_category_ids.join(", ")})
      ]).each do |cs|
        l2_category_map_l1_cateogry_id[cs["subcategory_id"]] = cs["category_id"]
      end

      subcategory_map_l2_category_id = category_ids.inject({}){|h, id| h[id] = id; h}
      for subcategory_level in 3..TOTAL_LEVELS
        subcategories = dest_db.query(%[
          SELECT category_id, subcategory_id FROM category_subcategories cs
          RIGHT JOIN pages ON pages.id = cs.subcategory_id AND pages.type = 1 AND
                             pages.language = #{language} AND pages.level = #{subcategory_level}
          WHERE cs.category_id IN (#{category_ids.join(", ")}) AND cs.language = #{language}
        ])

        category_ids = []
        category_value_sql = subcategories.collect do |subcategory|
          l2_category_id = subcategory_map_l2_category_id[subcategory["category_id"]]
          l1_category_id = l2_category_map_l1_cateogry_id[l2_category_id]
          subcategory_id = subcategory["subcategory_id"]
          subcategory_map_l2_category_id[subcategory_id] = l2_category_id
          category_ids << subcategory_id

          "(#{l1_category_id}, #{l2_category_id}, #{language}, #{subcategory_level}, #{subcategory_id})"
        end.join(", ")

        puts "  -- Inserting level #{subcategory_level}, count #{subcategories.count}, language #{language}"

        dest_db.query(%[ INSERT IGNORE INTO main_category_subcategories (l1_category_id, l2_category_id, language, level, subcategory_id)
          VALUES #{category_value_sql};
        ])
      end
    end
  end
end

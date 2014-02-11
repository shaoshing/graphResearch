require './helpers.rb'
require './category.rb'

class Conversion
  EN_SOURCE_TABLE_NAMES = %w[ category categorylinks langlinks page redirect ]
  ZH_SOURCE_TABLE_NAMES = %w[ categorylinks page redirect ]
  DEST_TABLE_NAMES = %w[ pages page_categories category_subcategories ]

  PAGE_LANG_ENG = 0
  PAGE_LANG_CHN = 1
  PAGE_TYPE_PAG = 0
  PAGE_TYPE_CAT = 1
  NAMESPACE_MAP_TYPES = { 0 => PAGE_TYPE_PAG, 14 => PAGE_TYPE_CAT }

  BATCH_SIZE = 4000
  MAX_TITLE_LENGTH = 191

  def self.do force
    puts "Wiki Table conversion"

    en_source_db = SQL.connect("en_source")
    zh_source_db = SQL.connect("zh_source")
    dest_db = SQL.connect("dest")

    if !SQL.check_table_existence(EN_SOURCE_TABLE_NAMES, en_source_db)
      puts "This script requires these tables, #{EN_SOURCE_TABLE_NAMES}, in the source database.
      Please download themfrom http://dumps.wikimedia.org/enwiki/latest/"
      exit 1
    end

    if !SQL.check_table_existence(ZH_SOURCE_TABLE_NAMES, zh_source_db)
      puts "This script requires these tables, #{ZH_SOURCE_TABLE_NAMES}, in the source database.
      Please download from http://dumps.wikimedia.org/zhwiki/latest/"
      exit 1
    end

    # drop dest database
    if SQL.check_table_existence(DEST_TABLE_NAMES, dest_db)
      if !force
        puts "  -- skipped. Dest db already has these tables #{DEST_TABLE_NAMES}"
        return
      else
        SQL.drop_tables(DEST_TABLE_NAMES, dest_db)
      end
    end

    # DB schema
    # pages:  id, title, type (page, category), language(en, zh)
    dest_db.query("CREATE TABLE pages(
      `id` int(10) unsigned NOT NULL,
      `language` int(3) unsigned NOT NULL DEFAULT '0',
      `type` int(3) unsigned NOT NULL DEFAULT '0',
      `title` varchar(#{MAX_TITLE_LENGTH}) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
      `translation` varchar(#{MAX_TITLE_LENGTH}) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
      KEY `page_title` (`title`),
      KEY `page_id_language` (`id`, `language`),
      UNIQUE KEY `page_id_language_type` (`id`, `type`, `language`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;")

    # page_categories: page_id, category_id
    dest_db.query("CREATE TABLE page_categories(
      `page_id` int(10) unsigned NOT NULL,
      `language` int(3) unsigned NOT NULL DEFAULT '0',
      `category_id` int(10) unsigned NOT NULL,
      UNIQUE KEY `page_id_and_category_id_language` (`page_id`, `category_id`, `language`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;")

    # category_subcategories category_id, subcategory_id
    dest_db.query("CREATE TABLE category_subcategories(
      `category_id` int(10) unsigned NOT NULL,
      `language` int(3) unsigned NOT NULL DEFAULT '0',
      `subcategory_id` int(10) unsigned NOT NULL,
      UNIQUE KEY `category_id_and_subcategory_id_language` (`category_id`, `subcategory_id`, `language`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;")

    # Find page ids with both en and zh
    # -> page_ids -> generate the pages table
    page_length = en_source_db.query('SELECT COUNT(*) FROM langlinks where ll_lang = "zh"').first["COUNT(*)"]
    i = 0
    batch_count = (page_length / BATCH_SIZE) + (page_length % BATCH_SIZE == 0 ? 0 : 1)
    timing = Timing.new(batch_count)

    begin
      puts "#{BATCH_SIZE*(i+1)} of #{page_length} pages"
      timing.track do

        pages = en_source_db.query(%[ SELECT ll.ll_from, ll.ll_title, p.page_namespace, p.page_title FROM langlinks ll
          RIGHT JOIN page p ON ll.ll_from = p.page_id
          WHERE ll.ll_lang = "zh" AND ll.ll_title <> "" LIMIT #{BATCH_SIZE*i}, #{BATCH_SIZE}])
        english_all_ids = pages.collect{|p|  p["ll_from"] }
        english_page_ids = pages.reject{|p| p["page_namespace"] != 0}.collect{|p|  p["ll_from"] }
        english_category_title_map_ids = {}
        pages.reject{|p| p["page_namespace"] != 14}.each{|p| english_category_title_map_ids[p["page_title"]] = p["ll_from"] }
        id_map_chinese = {}
        pages.each do |p|
          # remove unnecessary prefix "Category:" of category name in the langlinks table. Otherwise we won't be able to
          # find the category in the Chinese database.
          id_map_chinese[p["ll_from"]] = p["ll_title"].sub("Category:","")
        end

        # Insert for English pages
        [0, 14].each do |namespace|
          english_pages = en_source_db.query(%[ SELECT page_id, page_title, page_namespace FROM page
            WHERE page_namespace = #{namespace} AND page_id in (#{english_all_ids.join(", ")})])

          chinese_map_english = {}
          english_pages.each{|p| chinese_map_english[id_map_chinese[p["page_id"]]] = p["page_title"] }
          chinese_page_titles = chinese_map_english.keys.map{|t| SQL.escape(t)}

          Wiki.create_pages(en_source_db, english_pages, PAGE_LANG_ENG, dest_db, Category.hidden_category_ids(PAGE_LANG_ENG)) do |page|
            [id_map_chinese[page["page_id"]] || "", NAMESPACE_MAP_TYPES[page["page_namespace"]]]
          end

          # Insert for Chinese pages
          chinese_pages = zh_source_db.query(%[ SELECT page_id, page_title, page_namespace FROM page
            WHERE page_namespace = #{namespace} AND page_title in ("#{chinese_page_titles.join("\", \"")}")])
          chinese_page_ids = chinese_pages.reject{|p| p["page_namespace"] != 0}.collect{|p|  p["page_id"] }
          chinese_category_title_map_ids = {}
          chinese_pages.reject{|p| p["page_namespace"] != 14}.each{|p| chinese_category_title_map_ids[p["page_title"]] = p["page_id"] }
          Wiki.create_pages(zh_source_db, chinese_pages, PAGE_LANG_CHN, dest_db, Category.hidden_category_ids(PAGE_LANG_CHN)) do |page|
            [chinese_map_english[page["page_title"]] || "", NAMESPACE_MAP_TYPES[page["page_namespace"]]]
          end

          # Generate page categories
          Wiki.create_page_categories(en_source_db, english_page_ids, PAGE_LANG_ENG, dest_db,
            Category.hidden_category_ids(PAGE_LANG_ENG))
          Wiki.create_page_categories(zh_source_db, chinese_page_ids, PAGE_LANG_CHN, dest_db,
            Category.hidden_category_ids(PAGE_LANG_CHN))

          # Generate category subcategories
          Wiki.create_category_subcategories(en_source_db, dest_db, english_category_title_map_ids,
            PAGE_LANG_ENG, Category.hidden_category_ids(PAGE_LANG_ENG))
          Wiki.create_category_subcategories(zh_source_db, dest_db, chinese_category_title_map_ids,
            PAGE_LANG_CHN, Category.hidden_category_ids(PAGE_LANG_CHN))
        end
      end

      i += 1
    end until BATCH_SIZE*i >= page_length

    # Cleanup duplicate entries in pages, page_categories, and category_subcategories
  end
end

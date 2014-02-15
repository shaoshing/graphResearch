require './helpers.rb'

class Redirection

  BATCH_SIZE = 2000
  def self.do force
    dest_db = SQL.connect("dest")
    en_db = SQL.connect("en_source")
    zh_db = SQL.connect("zh_source")


    puts "Create page redirections table."
    if SQL.check_table_existence(%w[redirections], dest_db)
      if force
        SQL.drop_tables(%w[redirections], dest_db)
      else
        puts "  -- redirections table already existed."
        return
      end
    end
    dest_db.query("CREATE TABLE redirections (
      `id` int(10) unsigned NOT NULL,
      `language` int(3) unsigned NOT NULL DEFAULT '0',
      `title` varchar(#{Conversion::MAX_TITLE_LENGTH}) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
      `redirect_title` varchar(#{Conversion::MAX_TITLE_LENGTH}) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
      `redirect_id` int(10) unsigned NOT NULL,
      KEY `redirection_title` (`title`),
      KEY `redirection_redirect_id_language` (`redirect_id`, `language`),
      KEY `redirection_title_language` (`title`, `language`),
      UNIQUE KEY `redirection_id_language` (`id`, `language`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
    ")

    # en and zh find from dest table, find redirected, add entry
    [[Conversion::PAGE_LANG_ENG, en_db], [Conversion::PAGE_LANG_CHN, zh_db]].each do |data|
      lang_code, db = data
      i = 0
      page_length = dest_db.query("SELECT COUNT(*) FROM pages where language = #{lang_code}").first["COUNT(*)"]
      batch_count = (page_length / BATCH_SIZE) + (page_length % BATCH_SIZE == 0 ? 0 : 1)
      timing = Timing.new(batch_count)
      begin
        puts "#{BATCH_SIZE*(i+1)} of #{page_length} pages for language #{lang_code}"
        timing.track do
          pages = dest_db.query(%[ SELECT id, title FROM pages
          WHERE language = #{lang_code}
          LIMIT #{BATCH_SIZE*i}, #{BATCH_SIZE}])

          title_map_id = pages.inject({}){|r, page| r[page["title"].force_encoding('UTF-8')] = page["id"]; r }
          id_map_redirection = pages.inject({}) do |r, page|
            r[page["id"]] = {
              title: page["title"],
              redirect_title: page["title"],
              redirect_id: page["id"]
            }
            r
          end

          redirected_pages = db.query(%[ SELECT redirect.rd_from, page.page_title, redirect.rd_title from redirect
            RIGHT JOIN page ON page.page_id = redirect.rd_from
            WHERE redirect.rd_title IN ("#{title_map_id.keys.map{|t| SQL.escape(t)}.join("\", \"")}")
          ])
          redirected_pages.each do |page|
            if title_map_id[page["rd_title"].force_encoding('UTF-8')] == nil
              puts "-"*100
              puts title_map_id
              puts page["rd_title"]
              raise "Empty redirect id"
            end
            id_map_redirection[page["rd_from"]] = {
              title: page["page_title"],
              redirect_title: page["rd_title"],
              redirect_id: title_map_id[page["rd_title"].force_encoding('UTF-8')]
            }
          end

          page_values_sql = id_map_redirection.inject([]) do |sql, page|
            page_id, page_data = page
            if page_data[:title].length > Conversion::MAX_TITLE_LENGTH || page_data[:redirect_title].length > Conversion::MAX_TITLE_LENGTH
              puts "  -- Page title too long: "
              puts "  -- #{page_data[:title]}"
              puts "  -- #{page_data[:redirect_title]}"
            else
              sql << %[ (#{page_id}, "#{SQL.escape(page_data[:title]).force_encoding('UTF-8')}", "#{SQL.escape(page_data[:redirect_title]).force_encoding('UTF-8')}", #{page_data[:redirect_id]}, #{lang_code}) ].force_encoding('UTF-8')
            end
            sql
          end

          # puts %[ INSERT IGNORE INTO redirections (id, title, redirect_title, redirect_id, language) VALUES #{page_values_sql.join(", ")};]
          dest_db.query(%[ INSERT IGNORE INTO redirections (id, title, redirect_title, redirect_id, language)
            VALUES #{page_values_sql.join(", ")};])
        end
        i += 1
      end until BATCH_SIZE*i >= page_length
    end
  end
end

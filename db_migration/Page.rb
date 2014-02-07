require './helpers.rb'


class Page
  BATCH_SIZE = 100
  MAX_PAGES_PER_QUERY = 50
  WIKI_MEDIA_API = "wikipedia.org/w/api.php?action=query&prop=revisions&rvprop=content&format=json"
  def self.fetch_content(dest_db, force)
    puts "Fetch wiki pages and categories content"

    # drop dest database
    if SQL.check_table_existence(["page_content"], dest_db)
      if !force
        puts "  -- skipped. Dest db already has page_content table"
        return
      else
        SQL.drop_tables(["page_content"], dest_db)
      end
    end

    # page_content: page_id, language(en, zh), content
    dest_db.query("CREATE TABLE page_content(
      `page_id` int(10) unsigned NOT NULL,
      `language` int(3) unsigned NOT NULL DEFAULT '0',
      `content` longtext CHARACTER SET utf8mb4,
      KEY `page_id_language` (`page_id`, `language`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;")

    puts "Fetching page contents"
    page_length = dest_db.query('SELECT COUNT(*) FROM pages').first["COUNT(*)"]
    i = 0
    batch_count = (page_length / BATCH_SIZE) + (page_length % BATCH_SIZE == 0 ? 0 : 1)
    timing = Timing.new(batch_count)

    begin
      puts " -- #{BATCH_SIZE*(i+1)} of #{page_length} pages"
      timing.track do
        page_values = []
        pages = dest_db.query("SELECT id, language FROM pages LIMIT #{BATCH_SIZE*i}, #{BATCH_SIZE}")
        pages.group_by{|p| p["language"]}.each_pair do |language, all_pages|
          language_name = language == 0 ? "en" : "zh"
          all_ids = all_pages.collect{|p| p["id"]}
          existing_ids = dest_db.query("SELECT page_id from page_content
            WHERE language = #{language} AND page_id IN (#{all_ids.join(", ")})").collect{|p| p["page_id"]}
          (all_ids-existing_ids).each_slice(MAX_PAGES_PER_QUERY) do |ids|
            json = JSON.parse open("http://#{language_name}.#{WIKI_MEDIA_API}&pageids=#{ids.join("%7C")}").read
            json["query"]["pages"].each_pair do |page_id, page|
              next unless page["revisions"]
              page_values << %[(#{page_id}, #{language}, "#{SQL.escape(page["revisions"].first["*"])}")]
            end
          end
        end

        if !page_values.empty?
          dest_db.query(%[ INSERT INTO page_content (page_id, language, content) VALUES #{page_values.join(", ")};])
        end

        i += 1
      end
    end until BATCH_SIZE*i >= page_length
  end
end

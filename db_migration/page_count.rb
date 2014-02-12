require './helpers.rb'
require 'uri'

class PageCount
  BATCH_SIZE = 100000
  MINIMUM_PAGE_VIEW = 100 # mean page view: 9.634e+04
  PAGE_COUNT_FOLDER = "data/page_count"

  def self.do(force, verbose)
    puts "Gathering page count information"

    # Create page count column
    dest_db = SQL.connect("dest")
    # dest_db.query("ALTER TABLE pages ADD COLUMN view_count INT unsigned default 0")
    # dest_db.query("ALTER TABLE pages ADD INDEX `page_view_count` (`view_count`)")

    Dir["#{PAGE_COUNT_FOLDER}/en-*", "#{PAGE_COUNT_FOLDER}/zh-*"].each do |file|
      puts "Parsing #{file}"
      File.read(file).lines.each_slice(BATCH_SIZE) do |page_counts|
        lang_code = file.include?("en-") ? 0 : 1
        title_map_view = {}
        page_counts.each do |page_count|
          begin
            language, title, page_view, page_size = page_count.force_encoding('UTF-8').split(" ")
            title = URI.unescape(title).sub(/\:category\:/i, "")
            page_view = page_view.to_i

            next if page_view < MINIMUM_PAGE_VIEW
            title_map_view[title] = page_view
          rescue => e
            if verbose
              puts "-- exception"
              puts page_count
              puts e.message
              puts "-- end"
            end
          end
        end # end of page_counts.each

        next if title_map_view.count == 0
        puts "-- parsed #{title_map_view.count} view records"

        title_map_id = {}
        dest_db.query(%[ SELECT pages.id, pages.title, pages.view_count FROM redirections
          RIGHT JOIN pages ON pages.id = redirections.redirect_id AND pages.language = #{lang_code}
          WHERE redirections.language = #{lang_code}
                AND redirections.title IN ("#{title_map_view.keys.map{|t| SQL.escape(t)}.join("\", \"")}") ]).each do |page|
          title_map_id[page["title"]] = [page["id"], page["view_count"]]
        end

        update_sqls = []
        title_map_id.each_pair do |title, data|
          next unless title_map_view[title]
          page_id, page_view_count = data
          update_sqls << %[ UPDATE pages SET view_count = #{page_view_count+title_map_view[title]} WHERE id = #{page_id} AND language = #{lang_code} ]
        end

        next if update_sqls.count == 0

        puts "-- update #{update_sqls.count} records"
        begin
          dest_db.query("BEGIN")
          update_sqls.each{|sql| dest_db.query(sql)}
          dest_db.query("COMMIT")
        rescue => e
          dest_db.query("ROLLBACK")
          throw e
        end
      end

      puts "-- move #{file} to parsed folder"
      `mkdir -p #{PAGE_COUNT_FOLDER}/parsed && mv #{file} #{PAGE_COUNT_FOLDER}/parsed/`
    end
  end
end

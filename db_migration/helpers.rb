require 'yaml'
require 'mysql2'
require 'open-uri'
require 'json'

class SQL
  def self.escape str
    Mysql2::Client.escape(str)
  end

  def self.configs
    @database_config ||= YAML.load(File.read 'database.yml') rescue nil
    if !@database_config
      puts "Please add database config file 'database.yml'. You can copy one from database.yml.example"
    end
    @database_config
  end

  def self.connect config_name
    Mysql2::Client.new(
      "host" => self.configs["#{config_name}_host"],
      "port" => self.configs["#{config_name}_port"],
      "username" => self.configs["#{config_name}_username"],
      "password" => self.configs["#{config_name}_password"],
      "database" => self.configs["#{config_name}_database"],
      "encoding" => 'utf8mb4'
    )
  end

  def self.check_table_existence(table_names, db)
    (table_names - db.query("show tables;").collect(&:values).flatten).empty?
  end

  def self.drop_tables(table_names, db)
    existing_dest_table_names = table_names & db.query("show tables;").collect(&:values).flatten
    existing_dest_table_names.each do |name|
      puts "-- delete existing table in #{db.query_options[:database]}: #{name}"
      db.query "drop table #{name}"
    end
  end
end

class Wiki
  def self.create_pages(source_db, pages, lang, dest_db, hidden_category_ids)
    puts "  -- inserting pages #{lang}"
    return if pages.size == 0

    page_ids = pages.collect{|p| p["page_id"]}
    id_map_redirected_id = {}
    redirects = source_db.query("SELECT rd_from, page_id FROM redirect
      RIGHT JOIN page ON redirect.rd_title = page.page_title AND redirect.rd_namespace = page.page_namespace
      WHERE rd_from IN (#{page_ids.join(", ")})")
    redirects.each{|r| id_map_redirected_id[r["rd_from"]] = r["page_id"] }

    page_sql_values = pages.reject{|page| hidden_category_ids.include?(page["page_id"]) }.collect do |page|
      title_translation, page_type = yield(page)

      if title_translation.length > Conversion::MAX_TITLE_LENGTH || page["page_title"].length > Conversion::MAX_TITLE_LENGTH
        puts "  -- Page title too long: "
        puts "  -- #{page["page_id"]}"
        puts "  -- #{page["page_title"]}"
        puts "  -- #{title_translation}"
        next
      end

      %[(
        #{page["page_id"]},
        #{page_type},
        #{lang},
        "#{SQL.escape(page["page_title"])}",
        "#{SQL.escape(title_translation)}",
        #{id_map_redirected_id[page["page_id"]] || "NULL"}
      )]
    end.compact.join(",")
    dest_db.query(%[ INSERT IGNORE INTO pages (id, type, language, title, translation, redirect_to_id) VALUES #{page_sql_values};])
  end

  def self.create_page_categories(source_db, page_ids, lang, dest_db, hidden_category_ids)
    return if page_ids.empty?
    puts "  -- inserting page_categories #{lang}"
    page_categories_sql_values = []
    page_categories = source_db.query("SELECT cl.cl_from, p.page_id, p.page_title FROM categorylinks cl
      RIGHT JOIN page p ON p.page_title = cl.cl_to AND page_namespace = 14
      WHERE cl_from in (#{page_ids.join(", ")})")

    page_categories.each do |pc|
      next if hidden_category_ids.include?(pc["page_id"])
      page_categories_sql_values << %[(#{pc["cl_from"]}, #{pc["page_id"]}, #{lang})]
    end
    if not page_categories_sql_values.empty?
      dest_db.query(%[ INSERT IGNORE INTO page_categories (page_id, category_id, language) VALUES #{page_categories_sql_values.join(", ")};])
    end
  end

  def self.create_category_subcategories(source_db, dest_db, category_title_map_ids, lang, hidden_category_ids)
    category_title_map_ids.keys.each_slice(200) do |category_ids|
      subcategories = source_db.query(%[ SELECT cl.cl_from, cl.cl_to, p.page_title FROM categorylinks cl
        RIGHT JOIN page p ON p.page_id = cl.cl_from AND p.page_namespace = 14
        WHERE cl.cl_to IN ("#{category_ids.join("\", \"")}")])
      subcategories_sql_values = []
      subcategories.each do |subcategory|
        next if hidden_category_ids.include?(subcategory["cl_from"])
        subcategories_sql_values << "(#{category_title_map_ids[subcategory["cl_to"]]}, #{subcategory["cl_from"]}, #{lang})"
      end
      if not subcategories_sql_values.empty?
        puts "  -- inserting category_subcategories #{lang}"
        subcategories_sql_values.each_slice(5000) do |sql_values|
          dest_db.query(%[ INSERT IGNORE INTO category_subcategories (category_id, subcategory_id, language) VALUES #{sql_values.join(", ")};])
        end
      end
    end
  end
end

class Timing
  def initialize(total_rounds)
    @past_times = []
    @total_rounds = total_rounds
    @current = 0
  end

  def track
    time = Time.now
    yield
    @current += 1
    secs = Time.now - time
    @past_times << secs
    average_time = @past_times.inject(0){|sum, time| sum += time}/@past_times.size.to_f
    puts " -- took #{secs.round(2)} seconds, time remaining: #{(average_time*(@total_rounds-@current)).round(2)} seconds"
  end
end


class FileHelper
  def self.read_or_exit file_path
    content = File.read(file_path) rescue nil
    return content || begin
      puts "Unable to read file: #{file_path}"
      exit 1
    end
  end
end

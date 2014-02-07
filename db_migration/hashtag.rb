
require './helpers.rb'

class Hashtag

  TWITTER_HASHTAG_FILE = "data/twitter-hashtags.txt"
  WEIBO_HASHTAG_FILE = "data/weibo-hashtags.txt"
  BATCH_SIZE = 300
  def self.do
    # Read Twitter and Weibo hashtag from the text file
    puts "Reading hashtags"
    twitter_hashtags = read_hashtags(TWITTER_HASHTAG_FILE)
    weibo_hashtags = read_hashtags(WEIBO_HASHTAG_FILE)
    puts " -- got #{twitter_hashtags.size + weibo_hashtags.size} hashtags"

    # Find by page title from hashtags in batch
    puts "Finding wiki pages by comparing between page title and hashtag name"
    wiki_title_map_ids = {}
    dest_db = SQL.connect("dest")
    (twitter_hashtags + weibo_hashtags).each_slice(BATCH_SIZE) do |hashtags|
      escaped_hashtags = hashtags.collect{|ht| SQL.escape(ht)}
      hashtags_sql_string = %["#{escaped_hashtags.join("\", \"")}"]
      pages = dest_db.query(%[ SELECT id, title FROM pages WHERE title IN (#{hashtags_sql_string});])
      puts " -- found #{pages.count} pages"
      pages.each do |page|
        wiki_title_map_ids[page["title"].downcase] = page["id"]
      end
    end

    # Create tables
    puts "Inserting records."
    create_tables(dest_db)
    { twitter_hashtags => "twitter", weibo_hashtags => "weibo"}.each_pair do |hashtags, name|
      # Insert hashtags and ht_wikis
      hashtags_sql_values = []
      ht_wikis_sql_values = []
      hashtags.each_with_index do |hashtag, index|
        hashtags_sql_values << "(#{index+1}, \"#{SQL.escape hashtag}\")"
        if wiki_title_map_ids[hashtag.downcase]
          ht_wikis_sql_values << "(#{index+1}, #{wiki_title_map_ids[hashtag]}, \"#{SQL.escape hashtag}\", 1)"
        end
      end
      dest_db.query("INSERT INTO #{name}_hashtags (id, name) VALUES #{hashtags_sql_values.join(", ")};")
      dest_db.query("INSERT INTO #{name}_ht_wikis (hashtag_id, wiki_page_id, wiki_page_title, score) VALUES #{ht_wikis_sql_values.join(", ")};")
    end
  end

  private
  def self.read_hashtags file_path
    return FileHelper.read_or_exit(file_path).split("\n").collect do |line|
      line.split(" ").first
    end.reject do |hashtag|
      hashtag.empty?
    end
  end

  TABLE_NAMES = %w[ twitter_hashtags weibo_hashtags twitter_ht_wikis weibo_ht_wikis ]
  def self.create_tables(db)
    SQL.drop_tables TABLE_NAMES, db

    db.query("CREATE TABLE twitter_hashtags(
      `id` int(10) unsigned NOT NULL,
      `name` varbinary(255) NOT NULL,
      PRIMARY KEY (`id`),
      KEY `name` (`name`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;")
    db.query("CREATE TABLE weibo_hashtags(
      `id` int(10) unsigned NOT NULL,
      `name` varbinary(255) NOT NULL,
      PRIMARY KEY (`id`),
      KEY `name` (`name`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;")
    db.query("CREATE TABLE twitter_ht_wikis(
      `hashtag_id` int(10) unsigned NOT NULL,
      `wiki_page_id` int(10) unsigned NOT NULL,
      `wiki_page_title` varbinary(255) NOT NULL,
      `score` int(1) unsigned NOT NULL,
      KEY `hashtag_id` (`hashtag_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;")
    db.query("CREATE TABLE weibo_ht_wikis(
      `hashtag_id` int(10) unsigned NOT NULL,
      `wiki_page_id` int(10) unsigned NOT NULL,
      `wiki_page_title` varbinary(255) NOT NULL,
      `score` int(1) unsigned NOT NULL,
      KEY `hashtag_id` (`hashtag_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;")
  end
end

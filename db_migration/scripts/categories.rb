require 'mysql2'
require 'yaml'
require 'csv'

BATCH_SIZE = 5000

configs = YAML.load(File.read('database.yml'))
client = Mysql2::Client.new(
  host: configs['dest_host'],
  username: configs['dest_username'],
  password: configs['dest_password'],
  port: configs['dest_port'],
  database: configs['dest_database']
)

index = 0

csv_rows = [%w(
  en_page_id
  en_title
  zh_page_id
  zh_title
  en_category_count
  en_category_differ_count
  zh_category_count
  zh_category_differ_count
  shared_category_coun
)]

total_en_category_differ_count = 0
total_zh_category_differ_count = 0
total_shared_category_count = 0

loop do
  puts "Loading pages (total #{(index + 1) * BATCH_SIZE})"
  pages = client.query("SELECT id, title, translation, translation_id FROM pages2
      WHERE language = 1 AND type = 0 LIMIT #{BATCH_SIZE*index}, #{BATCH_SIZE}")
  index += 1
  break if pages.size == 0

  joined_en_ids = pages.map { |p| p['id'] }.join(', ')
  joined_zh_ids = pages.map { |p| p['translation_id'] }.join(', ')

  puts " -- loading page categories"
  zh_page_id_map_category_ids = {}
  client.query(
      "SELECT page_id, category_translation_id
        FROM page_categories2 WHERE language = 1 AND page_id IN(#{joined_en_ids})"
      ).each do |pc|
        zh_page_id_map_category_ids[pc['page_id']] ||= []
        zh_page_id_map_category_ids[pc['page_id']] << pc['category_translation_id']
      end
  en_page_id_map_category_ids = {}
  client.query(
      "SELECT page_id, category_id FROM page_categories2
        WHERE language = 0 AND page_id IN(#{joined_zh_ids})"
      ).each do |pc|
        en_page_id_map_category_ids[pc['page_id']] ||= []
        en_page_id_map_category_ids[pc['page_id']] << pc['category_id']
      end

  pages.each do |page|
    en_page_id = page['translation_id']
    en_title = page['translation']
    zh_page_id = page['id']
    zh_title = page['title']
    en_category_ids = en_page_id_map_category_ids[en_page_id] || []
    zh_category_ids = zh_page_id_map_category_ids[zh_page_id] || []
    en_category_count = en_category_ids.count
    en_category_differ_count = (en_category_ids - zh_category_ids).count
    zh_category_count = zh_category_ids.count
    zh_category_differ_count = (zh_category_ids - en_category_ids).count
    shared_category_count = (en_category_ids & zh_category_ids).count

    csv_rows << [
      en_page_id,
      en_title,
      zh_page_id,
      zh_title,
      en_category_count,
      en_category_differ_count,
      zh_category_count,
      zh_category_differ_count,
      shared_category_count
    ]

    total_zh_category_differ_count += 1 if zh_category_differ_count != 0
    total_en_category_differ_count += 1 if en_category_differ_count != 0
    total_shared_category_count += 1 if shared_category_count != 0
  end
end

puts "Page count #{csv_rows.count}"
puts "Total zh category differ count #{total_zh_category_differ_count}"
puts "Total en category differ count #{total_en_category_differ_count}"
puts "Total shared category count #{total_shared_category_count}"

puts 'Generating CSV'
CSV.open('tmp/page_categories.csv', 'w') do |csv|
  csv_rows.each { |row| csv << row }
end

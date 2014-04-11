-- Create table pages2 with translation id

CREATE TABLE pages2
SELECT p.*, p2.id AS translation_id
FROM pages AS p
JOIN pages AS p2
  ON p.translation = p2.title AND p.language != p2.language AND p.type = p2.type;

-- Collations
ALTER TABLE pages2 MODIFY `title` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE pages2 MODIFY `translation` varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- Indexes
CREATE INDEX `page_id_language_type` ON pages2 (`id`,`type`,`language`);
CREATE INDEX `page_title` ON pages2 (`title`);
CREATE INDEX `page_translation` ON pages2 (`translation`);
CREATE INDEX `page_id_language` ON pages2 (`id`, `language`);
CREATE INDEX `page_level` ON pages2 (`level`);
CREATE INDEX `page_view_count` ON pages2 (`view_count`);


CREATE TABLE page_categories2
SELECT p.*, p2.translation_id AS category_translation_id
FROM page_categories AS p
JOIN pages2 AS p2
  ON p.category_id = p2.id AND p.language = p2.language
-- LIMIT 100

CREATE INDEX `pc_language_category_id` ON page_categories2 (`language`, `page_id`);

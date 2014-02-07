
# Categories with Chinese translation

SELECT * from langlinks where ll_from IN (4892515,690747,696603,1633936,695027,8017451,771152,694871,3103170,1004110,691928,693800,1013214,956054,2389032,691182,691008,693708,692348,751381,696763,696648,694861,24980271,693555,1784082) and ll_lang = "zh";

Missing: Language = 8017451

# Subcategories of the 26 main categories

SELECT page_title from page where page_id IN (4892515,690747,696603,1633936,695027,8017451,771152,694871,3103170,1004110,691928,693800,1013214,956054,2389032,691182,691008,693708,692348,751381,696763,696648,694861,24980271,693555,1784082);


SELECT * from categorylinks JOIN page ON page.page_id = categorylinks.cl_from WHERE cl_type = 2 AND categorylinks.cl_to IN ("Mathematics","People","Science","Law","Medicine","History","Sports","Geography","Culture","Agriculture","Politics","Nature","Technology","Education","Health","Business","Belief","Humanities","Chronology","Society","Humans","Life","Environment","Arts","Language","Concepts");

= 889

## Subcategories with Chinese Translation

SELECT * from categorylinks JOIN langlinks ON langlinks.ll_from = categorylinks.cl_from AND langlinks.ll_lang = "zh" WHERE cl_type = 2 AND categorylinks.cl_to IN ("Mathematics","People","Science","Law","Medicine","History","Sports","Geography","Culture","Agriculture","Politics","Nature","Technology","Education","Health","Business","Belief","Humanities","Chronology","Society","Humans","Life","Environment","Arts","Language","Concepts");

= 483

## Pages link to subcategories

SELECT * from categorylinks WHERE cl_to IN (SELECT page.page_title from categorylinks LEFT JOIN page ON page.page_id = categorylinks.cl_from WHERE cl_type = 1 AND categorylinks.cl_to IN ("Mathematics","People","Science","Law","Medicine","History","Sports","Geography","Culture","Agriculture","Politics","Nature","Technology","Education","Health","Business","Belief","Humanities","Chronology","Society","Humans","Life","Environment","Arts","Language","Concepts"));

= 9210

## Pages with Chinese translations

SELECT * from categorylinks JOIN langlinks ON langlinks.ll_from = categorylinks.cl_from AND langlinks.ll_lang = "zh"  WHERE cl_to IN (SELECT page.page_title from categorylinks LEFT JOIN page ON page.page_id = categorylinks.cl_from WHERE cl_type = 1 AND categorylinks.cl_to IN ("Mathematics","People","Science","Law","Medicine","History","Sports","Geography","Culture","Agriculture","Politics","Nature","Technology","Education","Health","Business","Belief","Humanities","Chronology","Society","Humans","Life","Environment","Arts","Language","Concepts"));

= 2043

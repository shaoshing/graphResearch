# WikiPedia Project

## Toolkits

### Command: hotpage

This command will expand a existing hotpage txt file with title translation and category information.

#### Input and Output

Input: Txt file contains hot pages provided by Xin.

Output: File with the following fields (separated by |)

* Page id
* English title
* Event times
* Chinese title
* Direct level 1 categories
* Direct level 2 categories
* Indirect level 1 categories
* Indirect level 2 categories
* Redirected Titles
* Redirected Title Translations

#### Direct and Indirect categories

For example, page A has category Technology (level 1) and Eukaryotes (level 3), then its categories will be:

* Direct level 1 categories: Technology
* Direct level 2 categories: none
* Indirect level 1 categories: Life (Organisms belongs to Life)
* Indirect level 2 categories: Organisms (Eukaryotes belongs to Organisms)

Each category has three fields separated by ":", for example, "696648:Technology:技术", and each fields are:

* Category id
* Category English title
* Category Chinese title
* Repeat Count (valid for indirect categories only)

Multiple categories are joined by ",", for example, "696648:Technology:技术, 751381:Health:健康".

#### Example Output

> 84121|Atkins diet|2012-11-11,2012-11-14,Unknown|阿特金斯健康饮食法|||696648:Technology:技术:1, 751381:Health:健康:3, 2389032:Life:生命:1|737037:Behavior:行为:1, 12777176:Technology_and_inventions_by_region:各地区技术与发明:1, 35612550:Determinants_of_health:健康效应因素:3|Atkin's_diet, Atkins_Diet, The_Low_Carb_Revolution, Atkins_Nutritionals_Inc, Atkins_diet_plan, Dr_atkins_diet, Adkins_diet, Aitkens_Diet, Atkin's_Diet, Net_carb, Net_carbs, Atkins_nutritional_approach, Atkinson_diet, Net_carbohydrates, Net_carbohydrate, Atkins_Nutritional_Approach, Atkins_Nutritional_Approach, Atkins_Diet_Revolution, The_atkins_diet, Dr._Atkins'_New_Diet_Revolution, Dr._Atkins'_Diet_Revolution
>
> Page id: 84121
>
> English Title: Atkins diet
>
> Event times: 2012-11-11,2012-11-14,Unknown
>
> Chinese title: 阿特金斯健康饮食法
>
> Direct level 1 categories: none
>
> Direct level 2 categories: none
>
> Indirect level 1 categories: 696648:Technology:技术:1, 751381:Health:健康:3, 2389032:Life:生命:1
>
> Indirect level 2 categories: 737037:Behavior:行为:1, 12777176:Technology_and_inventions_by_region:各地区技术与发明:1, 35612550:Determinants_of_health:健康效应因素:3
>
> Redirected Titles: Atkin's_diet, Atkins_Diet, The_Low_Carb_Revolution, Atkins_Nutritionals_Inc, Atkins_diet_plan, ...
>
> Redirected Title Translations: 食肉减肥法, 阿特金斯减肥法, 低碳减肥法, ...

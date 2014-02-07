
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

Multiple categories are joined by ",", for example, "696648:Technology:技术, 751381:Health:健康".

#### Example Output

> 84121|Atkins diet|2012-11-11,2012-11-14,Unknown|阿特金斯健康饮食法|||696648:Technology:技术, 751381:Health:健康, 2389032:Life:生命|737037:Behavior:行为, 12777176:Technology_and_inventions_by_region:各地区技术与发明, 35612550:Determinants_of_health:健康效应因素
>
> 4467906|Woo Bum-kon|2012-11-08,2012-11-09,2012-11-10|禹範坤|||1013214:Chronology:年代学, 2389032:Life:生命|884430:Years:年, 921954:Death:死亡
>
> 330923|Cachexia|2012-11-02,2012-11-03,2012-11-04|惡病體質||||
>
> 194904|Track and field|Unknown,2012-10-16,2012-10-18|田径|||693708:Sports:体育|23596516:Sports_by_type:各类体育运动
>
> 79658|Lychee|2012-11-07,2012-11-09,2012-11-10|荔枝|||694871:Agriculture:农业, 1633936:Society:社会, 2389032:Life:生命|692675:Biology:生物学, 694870:Crops:農作物, 2933492:Nationality:國籍, 4866835:Agriculture_by_region:各地区农业, 8899078:Organisms:生物

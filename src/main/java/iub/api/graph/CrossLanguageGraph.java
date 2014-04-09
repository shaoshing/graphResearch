package iub.api.graph;

import java.util.Date;

/**
 * Created by shaoshing on 4/8/14.
 */
public class CrossLanguageGraph {

    // return unique id of the graph. The value of the id is the name of the label attached to
    // the created graph (nodes). To show the graph using Cypher, use:
    //  MATCH (x:[value of id]) RETURN x;

    static private final String ENGLISH = "En";
    static private final String CHINESE = "Zh";
    static private final String NEO4J_URL = "http://localhost:7474/db/data/";
    static private final String NODE_KEYWORD = "Keyword";
    static private final String NODE_KEYWORD_NAME_ATTR = "Name";
    static private final String NODE_KEYWORD_LANG_ATTR = "Language";
    static private final String NODE_PAGE = "Page";
    static private final String NODE_PAGE_EN_ID_ATTR = "EnId";
    static private final String NODE_PAGE_ZH_ID_ATTR = "ZhId";
    static private final String RELATION_HAS_KEYWORD = "HasKeyword";


    static public void createGraphByKeywords(String[] enKeywords, String[] zhKeywords){
        createGraphByKeywordsAndLanguage(enKeywords, ENGLISH);
        createGraphByKeywordsAndLanguage(zhKeywords, CHINESE);
    }

    static private void createGraphByKeywordsAndLanguage(String[] keywords, String languageName){
        SearchClient.LANGUAGE searchLanguage = SearchClient.LANGUAGE.ENGLISH;
        if(languageName != ENGLISH){
            searchLanguage = SearchClient.LANGUAGE.CHINESE;
        }
        String nodePageTitleAttr = languageName+"Title"; // = ZhTitle or EnTitle

        for(String keyword: keywords){
            SearchClient.Page[] pages = SearchClient.search(keyword, searchLanguage);
            for(SearchClient.Page page: pages){
                // Create keyword node
                // Cypher: MERGE (n:Keyword {Name: "Academic", Language: "En"}) SET n:212323533
                String createKeywordNodeCypher = String.format(
                        "MERGE (n:%s {%s: \"%s\", %s: \"%s\"})",
                        NODE_KEYWORD, NODE_KEYWORD_NAME_ATTR, keyword, NODE_KEYWORD_LANG_ATTR, languageName);
                neo4jClient().query(createKeywordNodeCypher);

                // Create page node
                // Cypher:
                //      MERGE (:Page {EnId: 2222, ZhId: 3333})
                //      MATCH (n:Page {EnId: 2222}) SET n.EnTitle = "Hello" // or n.ZhTitle = "你好"
                String createPageNodeCypher = String.format( "MERGE (:%s {%s: %s, %s: %s})",
                        NODE_PAGE, NODE_PAGE_EN_ID_ATTR, page.enId, NODE_PAGE_ZH_ID_ATTR, page.zhId);
                neo4jClient().query(createPageNodeCypher);
                String setPageNodeTitleCypher = String.format( "MATCH (n:%s {%s: %s}) SET n.%s = \"%s\"",
                        NODE_PAGE, NODE_PAGE_EN_ID_ATTR, page.enId, nodePageTitleAttr, page.title);
                neo4jClient().query(setPageNodeTitleCypher);

                // Create relation
                // Cypher: MATCH (k:Keyword {Name: "Academic", Language: "En"}), (p:Page {EnId: 2222})
                //         MERGE p-[:HasKeyword]->k
                String createRelationCypher = String.format(
                        "MATCH (k:%s {%s:\"%s\"}), (p:%s {%s:%s}) MERGE p-[:%s]->k",
                        NODE_KEYWORD, NODE_KEYWORD_NAME_ATTR, keyword, NODE_KEYWORD_LANG_ATTR, languageName,
                        NODE_PAGE, NODE_PAGE_EN_ID_ATTR, page.enId,
                        RELATION_HAS_KEYWORD);
                neo4jClient().query(createRelationCypher);
            }
        }
    }


    static private Neo4jClient _neo4jClient;
    static private Neo4jClient neo4jClient(){
        if(_neo4jClient == null){
            _neo4jClient = new Neo4jClient(NEO4J_URL);
        }
        return _neo4jClient;
    }

}

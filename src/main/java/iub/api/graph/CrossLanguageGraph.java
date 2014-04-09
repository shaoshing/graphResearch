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
    static private final String NODE_PAGE_EN_TITLE_ATTR = "EnTitle";
    static private final String NODE_PAGE_ZH_ID_ATTR = "ZhId";
    static private final String NODE_PAGE_ZH_TITLE_ATTR = "ZhTitle";
    static private final String RELATION_HAS_KEYWORD = "HasKeyword";


    static public String createGraphByKeywords(String[] enKeywords, String[] zhKeywords){
        String graphId = Long.toString((new Date()).getTime());

        createGraphByKeywordsAndLanguage(enKeywords, ENGLISH, graphId);
        createGraphByKeywordsAndLanguage(zhKeywords, CHINESE, graphId);

        return graphId;
    }

    static private void createGraphByKeywordsAndLanguage(String[] keywords, String languageName, String graphId){
        SearchClient.LANGUAGE searchLanguage = SearchClient.LANGUAGE.ENGLISH;
        if(languageName != ENGLISH){
            searchLanguage = SearchClient.LANGUAGE.CHINESE;
        }

        for(String keyword: keywords){
            SearchClient.Page[] pages = SearchClient.search(keyword, searchLanguage);
            for(SearchClient.Page page: pages){
                // Create keyword node
                // Cypher: MERGE (n:Keyword {Name: "Academic", Language: "En"}) SET n:212323533
                String createKeywordNodeCypher = String.format(
                        "MERGE (n:%s {%s: \"%s\", %s: \"%s\"}) SET n:%s",
                        NODE_KEYWORD, NODE_KEYWORD_NAME_ATTR, keyword, NODE_KEYWORD_LANG_ATTR, languageName, graphId);
                neo4jClient().query(createKeywordNodeCypher);

                // Create page node
                // Cypher: MERGE (n:Page {EnId: 2222, EnTitle: "Hello", ZhId: 3333, ZhTitle: "你好"}) SET n:212323533
                String createPageNodeCypher = String.format(
                        "MERGE (n:%s {%s: %s, %s: \"%s\", %s: %s, %s: \"%s\"}) SET n:%s", NODE_PAGE,
                        NODE_PAGE_EN_ID_ATTR, page.enId, NODE_PAGE_EN_TITLE_ATTR, page.enTitle,
                        NODE_PAGE_ZH_ID_ATTR, page.zhId, NODE_PAGE_ZH_TITLE_ATTR, page.zhTitle,
                        graphId);
                neo4jClient().query(createPageNodeCypher);

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

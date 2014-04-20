package iub.api.graph;

import java.util.ArrayList;
import java.util.Properties;

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

    static private final String RELATION_EXACT_MATCH_TITLE = "EXACT_MATCH_TITLE";
    static private final String RELATION_PARTIAL_MATCH_TITLE = "PARTIAL_MATCH_TITLE";
    static private final String RELATION_PARTIAL_MATCH_CONTENT = "PARTIAL_MATCH_CONTENT";

    static public final int CREATE_EXACT_MATCH_TITLE_RELATION = 1;
    static public final int CREATE_PARTIAL_MATCH_TITLE_RELATION = 2;
    static public final int CREATE_PARTIAL_MATCH_CONTENT_RELATION= 4;

    private Properties config;

    public CrossLanguageGraph(Properties config){
        this.config = config;
    }

    public void createGraphByKeywords(String[] enKeywords, String[] zhKeywords, int relationOptions){
        String[] enPageIds = createKeywordAndWikiGraph(enKeywords, ENGLISH, relationOptions);
        String[] zhPageIds = createKeywordAndWikiGraph(zhKeywords, CHINESE, relationOptions);
        // createWikiAndCategoryGraph(enPageIds, zhPageIds);
    }


    static private final int[] RELATION_OPTIONS = {CREATE_EXACT_MATCH_TITLE_RELATION,
            CREATE_PARTIAL_MATCH_TITLE_RELATION, CREATE_PARTIAL_MATCH_CONTENT_RELATION};
    // RELATION_NAMES_MAPPING[CREATE_PARTIAL_MATCH_CONTENT_RELATION] returns "PartialMatchContent"
    static private final String[] RELATION_NAMES_MAPPING = { "", RELATION_EXACT_MATCH_TITLE,
            RELATION_PARTIAL_MATCH_TITLE, "", RELATION_PARTIAL_MATCH_CONTENT};

    private String[] createKeywordAndWikiGraph(String[] keywords, String languageName, int relationOptions){
        SearchClient.LANGUAGE searchLanguage = SearchClient.LANGUAGE.ENGLISH;
        if(languageName != ENGLISH){
            searchLanguage = SearchClient.LANGUAGE.CHINESE;
        }
        String nodePageTitleAttr = languageName+"Title"; // = ZhTitle or EnTitle

        ArrayList<String> pageIds = new ArrayList<String>();
        for(int relationOption: RELATION_OPTIONS){
            if((relationOption & relationOptions) == 0){
                continue;
            }

            for(String keyword: keywords){
                SearchClient.Page[] pages = SearchClient.search(keyword, searchLanguage, relationOption);
                for(SearchClient.Page page: pages){
                    createNodesAndRelations(keyword, languageName, page, nodePageTitleAttr, relationOption);
                }

                for(SearchClient.Page page: pages){
                    if(languageName == ENGLISH){
                        pageIds.add(page.enId);
                    }else{
                        pageIds.add(page.zhId);
                    }
                }
            }
        }

        String[] results = new String[pageIds.size()];
        pageIds.toArray(results);
        return results;
    }

//    static private void createWikiAndCategoryGraph(String[] enPageIds, String[] zhPageIds){
//        //
//        String dbClass = "com.mysql.jdbc.Driver";
//        Class.forName(dbClass);
//
//        // setup the connection with the DB.
//        connect = DriverManager
//                .getConnection("jdbc:mysql://localhost/feedback?"
//                        + "user=sqluser&password=sqluserpw");
//    }

    private Neo4jClient _neo4jClient;
    private Neo4jClient neo4jClient(){
        if(_neo4jClient == null){
            _neo4jClient = new Neo4jClient(NEO4J_URL);
        }
        return _neo4jClient;
    }

    static private String escapeString(String str){
        return org.apache.commons.lang3.StringEscapeUtils.escapeJson(str);
    }

    private void createNodesAndRelations(
            String keyword, String languageName, SearchClient.Page page, String nodePageTitleAttr, int relationOption){

        // Create keyword node
        // Cypher: MERGE (n:Keyword {Name: "Academic", Language: "En"}) SET n:212323533
        String createKeywordNodeCypher = String.format(
                "MERGE (n:%s {%s: \"%s\", %s: \"%s\"})",
                NODE_KEYWORD, NODE_KEYWORD_NAME_ATTR, escapeString(keyword),
                NODE_KEYWORD_LANG_ATTR, languageName);
        neo4jClient().query(createKeywordNodeCypher);

        // Create page node
        // Cypher:
        //      MERGE (:Page {EnId: 2222, ZhId: 3333})
        //      MATCH (n:Page {EnId: 2222}) SET n.EnTitle = "Hello" // or n.ZhTitle = "你好"
        String createPageNodeCypher = String.format( "MERGE (:%s {%s: %s, %s: %s})",
                NODE_PAGE, NODE_PAGE_EN_ID_ATTR, page.enId, NODE_PAGE_ZH_ID_ATTR, page.zhId);
        neo4jClient().query(createPageNodeCypher);
        String setPageNodeTitleCypher = String.format( "MATCH (n:%s {%s: %s}) SET n.%s = \"%s\"",
                NODE_PAGE, NODE_PAGE_EN_ID_ATTR, page.enId, nodePageTitleAttr, escapeString(page.title));
        neo4jClient().query(setPageNodeTitleCypher);

        // Create relation
        // Cypher: MATCH (k:Keyword {Name: "Academic", Language: "En"}), (p:Page {EnId: 2222})
        //         MERGE p-[:HasKeyword]->k
        String createRelationCypher = String.format(
                "MATCH (k:%s {%s:\"%s\"}), (p:%s {%s:%s}) MERGE p-[r:%s]->k SET r.Score = %d",
                NODE_KEYWORD, NODE_KEYWORD_NAME_ATTR, keyword, NODE_KEYWORD_LANG_ATTR, languageName,
                NODE_PAGE, NODE_PAGE_EN_ID_ATTR, page.enId,
                RELATION_NAMES_MAPPING[relationOption], page.score);
        neo4jClient().query(createRelationCypher);
    }
}

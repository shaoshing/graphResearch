package iub.api.graph;

import com.google.common.base.Joiner;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by shaoshing on 4/8/14.
 */
public class CrossLanguageGraph {

    // return unique id of the graph. The value of the id is the name of the label attached to
    // the created graph (nodes). To show the graph using Cypher, use:
    //  MATCH (x:[value of id]) RETURN x;

    static private final String CONFIG_NEO4J_URL = "neo4j.url";
    static private final String CONFIG_DB_HOST = "database.host";
    static private final String CONFIG_DB_NAME = "database.name";
    static private final String CONFIG_DB_USER = "database.user";
    static private final String CONFIG_DB_PSWD = "database.password";
    static private final String CONFIG_DB_PORT = "database.port";
    static private final String CONFIG_LUCENE_EN = "lucene.index.en";
    static private final String CONFIG_LUCENE_ZH = "lucene.index.zh";

    static private final String ENGLISH = "En";
    static private final String CHINESE = "Zh";
    static private final String NODE_KEYWORD = "Keyword";
    static private final String NODE_KEYWORD_NAME_ATTR = "Name";
    static private final String NODE_KEYWORD_LANG_ATTR = "Language";
    static private final String NODE_PAGE = "Page";
    static private final String NODE_PAGE_EN_ID_ATTR = "EnId";
    static private final String NODE_PAGE_ZH_ID_ATTR = "ZhId";
    static private final String NODE_CATEGORY = "Category";

    static private final String RELATION_EXACT_MATCH_TITLE = "EXACT_MATCH_TITLE";
    static private final String RELATION_PARTIAL_MATCH_TITLE = "PARTIAL_MATCH_TITLE";
    static private final String RELATION_PARTIAL_MATCH_CONTENT = "PARTIAL_MATCH_CONTENT";
    static private final String RELATION_BELONGS_TO_CATEGORY = "BELONGS_TO_CATEGORY";

    static public final int CREATE_EXACT_MATCH_TITLE_RELATION = 1;
    static public final int CREATE_PARTIAL_MATCH_TITLE_RELATION = 2;
    static public final int CREATE_PARTIAL_MATCH_CONTENT_RELATION= 4;

    private Properties config;

    public CrossLanguageGraph(Properties config){
        this.config = config;
    }

    public void createGraphByKeywords(ArrayList<String> enKeywords, ArrayList<String> zhKeywords, int relationOptions){
        if(!neo4jClient().testConnection()){
            System.out.println("[graph] Unable to connect to neo4j with " + config.getProperty(CONFIG_NEO4J_URL));
            return;
        }

        if(!testDBConnection()){
            return;
        }

        System.out.println("[graph] Creating keywords and wiki graph - EN");
        ArrayList<String> enPageIds = createKeywordAndWikiGraph(enKeywords, ENGLISH, relationOptions);

        System.out.println("[graph] Creating keywords and wiki graph - ZH");
        enPageIds.addAll(createKeywordAndWikiGraph(zhKeywords, CHINESE, relationOptions));

        System.out.println("[graph] Creating wiki and category graph");
        createWikiAndCategoryGraph(enPageIds);
    }


    static private final int[] RELATION_OPTIONS = {CREATE_EXACT_MATCH_TITLE_RELATION,
            CREATE_PARTIAL_MATCH_TITLE_RELATION, CREATE_PARTIAL_MATCH_CONTENT_RELATION};
    // RELATION_NAMES_MAPPING[CREATE_PARTIAL_MATCH_CONTENT_RELATION] returns "PartialMatchContent"
    static private final String[] RELATION_NAMES_MAPPING = { "", RELATION_EXACT_MATCH_TITLE,
            RELATION_PARTIAL_MATCH_TITLE, "", RELATION_PARTIAL_MATCH_CONTENT};

    private ArrayList<String> createKeywordAndWikiGraph(ArrayList<String> keywords, String languageName, int relationOptions){
        SearchClient.LANGUAGE searchLanguage = SearchClient.LANGUAGE.ENGLISH;
        if(languageName != ENGLISH){
            searchLanguage = SearchClient.LANGUAGE.CHINESE;
        }
        String nodePageTitleAttr = languageName+"Title"; // = ZhTitle or EnTitle

        SearchClient searchClient = new SearchClient(
                config.getProperty(CONFIG_LUCENE_EN), config.getProperty(CONFIG_LUCENE_ZH));
        ArrayList<String> pageIds = new ArrayList<String>();
        for(int relationOption: RELATION_OPTIONS){
            if((relationOption & relationOptions) == 0){
                continue;
            }

            for(String keyword: keywords){
                ArrayList<SearchClient.Page> pages = searchClient.search(keyword, searchLanguage, relationOption);
                for(SearchClient.Page page: pages){
                    createNodesAndRelations(keyword, languageName, page, nodePageTitleAttr, relationOption);
                }

                for(SearchClient.Page page: pages){
                    pageIds.add(page.enId);
                }
            }
        }

        return pageIds;
    }



    static final int BATCH_EN_ID_COUNT = 1000;
    private void createWikiAndCategoryGraph(ArrayList<String> enPageIdsArray) {
        // TODO: batch creation
        String [] enPageIds = new String[enPageIdsArray.size()];
        enPageIdsArray.toArray(enPageIds);

        int currentIdIndex = 0;
        while(currentIdIndex+1 < enPageIdsArray.size()){
            int nextIdIndex = currentIdIndex + BATCH_EN_ID_COUNT;
            if(nextIdIndex > enPageIdsArray.size()){
                nextIdIndex = enPageIdsArray.size();
            }
            List<String> ids = enPageIdsArray.subList(currentIdIndex, nextIdIndex);
            currentIdIndex = nextIdIndex;

            try {
                String sql = String.format("SELECT c.id AS category_id, c.name AS category_title, pc.id AS page_id " +
                        "FROM page_categories AS pc " +
                        "JOIN Category AS c ON pc.pages = c.id " +
                        "WHERE pc.id IN (%s);", Joiner.on(", ").join(ids));

                Statement statement = getDB().createStatement();
                ResultSet result = statement.executeQuery(sql);

                while(result.next()){
                    int categoryId = result.getInt("category_id");
                    String categoryTitle = result.getString("category_title");
                    int pageId = result.getInt("page_id");

                    // TODO: add cache
                    // Create category node
                    // Cypher: MERGE (c:Category {Name: "Academic", Language: "En"})
                    String createCategoryNodeCypher = String.format(
                            "MERGE (c:%s {id: %d, title: \"%s\"})",
                            NODE_CATEGORY, categoryId, escapeString(categoryTitle));
                    neo4jClient().query(createCategoryNodeCypher);

                    // Add Relation
                    // Cypher: MATCH (c:Category {id: 1}), (p:Page {id: 2}) MERGE p -[:BELONGS_TO_CATEGORY]-> c
                    String createRelationCypher = String.format(
                            "MATCH (c:%s {id: %d}), (p:%s {%s: %d}) MERGE p -[:%s]-> c",
                            NODE_CATEGORY, categoryId,
                            NODE_PAGE, NODE_PAGE_EN_ID_ATTR, pageId,
                            RELATION_BELONGS_TO_CATEGORY);
                    neo4jClient().query(createRelationCypher);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean testDBConnection(){
        return getDB() != null;
    }

    private Connection _mysqlConnection;
    private Connection getDB() {
        if(this._mysqlConnection != null){
            return this._mysqlConnection;
        }

        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        String connectionStr = String.format("jdbc:mysql://%s:%s/%s?user=%s&password=%s",
                config.getProperty(CONFIG_DB_HOST),
                config.getProperty(CONFIG_DB_PORT),
                config.getProperty(CONFIG_DB_NAME),
                config.getProperty(CONFIG_DB_USER),
                config.getProperty(CONFIG_DB_PSWD));
        try {
            this._mysqlConnection = DriverManager.getConnection(connectionStr);
        } catch (SQLException e) {
            System.out.println("[graph] Unable to connect to MySQL with following link:");
            System.out.println(" -- " + connectionStr + "\n");
            e.printStackTrace();
            return null;
        }

        return this._mysqlConnection;
    }

    private Neo4jClient _neo4jClient;
    private Neo4jClient neo4jClient(){
        if(_neo4jClient == null){
            _neo4jClient = new Neo4jClient(config.getProperty(CONFIG_NEO4J_URL));
        }
        return _neo4jClient;
    }

    static private String escapeString(String str){
        return org.apache.commons.lang3.StringEscapeUtils.escapeJson(str);
    }

    private void createNodesAndRelations(
            String keyword, String languageName, SearchClient.Page page, String nodePageTitleAttr, int relationOption){

        // Create keyword node
        // Cypher: MERGE (n:Keyword {Name: "Academic", Language: "En"})
        String createKeywordNodeCypher = String.format(
                "MERGE (n:%s {%s: \"%s\", %s: \"%s\"})",
                NODE_KEYWORD, NODE_KEYWORD_NAME_ATTR, escapeString(keyword),
                NODE_KEYWORD_LANG_ATTR, languageName);
        neo4jClient().query(createKeywordNodeCypher);

        // Create page node
        // Cypher:
        //      MERGE (:Page {EnId: 2222, ZhId: 3333})
        //      MATCH (n:Page {EnId: 2222}) SET n.EnTitle = "Hello" // or n.ZhTitle = "你好"
        // TODO: add cache
        // TODO: add redirected titles
        String createPageNodeCypher = String.format( "MERGE (:%s {%s: %s, %s: %s})",
                NODE_PAGE, NODE_PAGE_EN_ID_ATTR, page.enId, NODE_PAGE_ZH_ID_ATTR, page.zhId);
        neo4jClient().query(createPageNodeCypher);
        // TODO: add cache
        String setPageNodeTitleCypher = String.format( "MATCH (n:%s {%s: %s}) SET n.%s = \"%s\"",
                NODE_PAGE, NODE_PAGE_EN_ID_ATTR, page.enId, nodePageTitleAttr, escapeString(page.title));
        neo4jClient().query(setPageNodeTitleCypher);

        // Create relation
        // Cypher: MATCH (k:Keyword {Name: "Academic", Language: "En"}), (p:Page {EnId: 2222})
        //         MERGE p-[:HasKeyword]->k
        String createRelationCypher = String.format(
                "MATCH (k:%s {%s:\"%s\", %s: \"%s\"}), (p:%s {%s:%s}) MERGE p-[r:%s]->k SET r.Score = %d",
                NODE_KEYWORD, NODE_KEYWORD_NAME_ATTR, keyword, NODE_KEYWORD_LANG_ATTR, languageName,
                NODE_PAGE, NODE_PAGE_EN_ID_ATTR, page.enId,
                RELATION_NAMES_MAPPING[relationOption], page.score);
        neo4jClient().query(createRelationCypher);
    }
}

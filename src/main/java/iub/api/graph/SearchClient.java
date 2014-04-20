package iub.api.graph;

import java.util.ArrayList;

/**
 * Created by shaoshing on 4/8/14.
 */
public class SearchClient {
    public class Page{
        public String enId;
        public String zhId;
        public String title;
        public int score;

        public Page(String enId, String zhId, String title, int score){
            this.enId = enId;
            this.zhId = zhId;
            this.title = title;
            this.score = score;
        }
    }

    enum LANGUAGE{
        ENGLISH,
        CHINESE
    }

    static public final int EXACT_MATCH_TITLE = 1;
    static public final int PARTIAL_MATCH_TITLE = 2;
    static public final int PARTIAL_MATCH_CONTENT = 4;

    private String zhLucenePath;
    private String enLucenePath;

    public SearchClient(String zhLucenePath, String enLucenePath){
        this.zhLucenePath = zhLucenePath;
        this.enLucenePath = enLucenePath;
    }

    public Page[] search(String keyword, LANGUAGE language, int matchOption){
        // TODO: return empty result until we have the Lucene indexes.
        ArrayList<Page> pages = new ArrayList<Page>();
        return pages.toArray(new Page[pages.size()]);
    };
}

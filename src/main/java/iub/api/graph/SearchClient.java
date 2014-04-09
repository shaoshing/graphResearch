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
    }

    enum LANGUAGE{
        ENGLISH,
        CHINESE
    }

    public static Page[] search(String keyword, LANGUAGE language){
        // TODO: return empty result until we have the Lucene indexes.
        ArrayList<Page> pages = new ArrayList<Page>();
        return pages.toArray(new Page[pages.size()]);
    };
}

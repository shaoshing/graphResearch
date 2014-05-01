package iub.api.graph;

import java.util.ArrayList;
import wikipedia_Chinese_index_search.*;
import wikipedia_English_index_search.*;

/**
 * Created by shaoshing on 4/8/14.
 */
public class SearchClient {
    public class Page{
        public String enId;
        public String zhId;
        public String title;
        public int score;
        public ArrayList<String> redirectedTitles;

        public Page(String enId, String zhId, String title, int score, ArrayList<String> redirectedTitles){
            this.enId = enId;
            this.zhId = zhId;
            this.title = title;
            this.score = score;
            this.redirectedTitles = redirectedTitles;
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
    private SearchEnglishWikiIndex searchEn;
    private SearchChineseWikiIndex searchZh;

    public SearchClient(String zhLucenePath, String enLucenePath){
        this.zhLucenePath = zhLucenePath;
        this.enLucenePath = enLucenePath;
        this.searchEn = new SearchEnglishWikiIndex();
        this.searchZh = new SearchChineseWikiIndex();
    }

    public ArrayList<Page> search(String keyword, LANGUAGE language, int matchOption){
        if(language == LANGUAGE.ENGLISH){
            return searchEn(keyword, matchOption);
        }else{
            return searchZh(keyword, matchOption);
        }
    };

    private ArrayList<Page> searchEn(String keyword, int matchOption){
        ArrayList<wikipedia_English_index_search.Result> results = new ArrayList<wikipedia_English_index_search.Result>();
        try {
            if((matchOption | EXACT_MATCH_TITLE) != 0){
                results = searchEn.getTitle_ExactMatch(keyword, enLucenePath);
            }else if((matchOption | PARTIAL_MATCH_TITLE) != 0){
                results = searchEn.getTitle_PartialMatch(keyword, enLucenePath);
            }else if((matchOption | PARTIAL_MATCH_CONTENT) != 0){
                results = searchEn.getContent(keyword, enLucenePath);
            }
        } catch (Exception e) {
            System.out.println("[lucene] Exception raised. Maybe you have a incorrect EN index path.");
            e.printStackTrace();
        }

        ArrayList<Page> pages = new ArrayList<Page>();
        for(wikipedia_English_index_search.Result result: results){
            Page page = new Page(
                    result.getEnglish_ID(),
                    result.getChinese_ID(),
                    result.getTitle(),
                    1,
                    result.getRedirected_titles());
            pages.add(page);
        }

        return pages;
    }

    private ArrayList<Page> searchZh(String keyword, int matchOption){
        ArrayList<wikipedia_Chinese_index_search.Result> results = new ArrayList<wikipedia_Chinese_index_search.Result>();
        try {
            if((matchOption | EXACT_MATCH_TITLE) != 0){
                results = searchZh.getTitle_ExactMatch(keyword, zhLucenePath);
            }else if((matchOption | PARTIAL_MATCH_TITLE) != 0){
                results = searchZh.getTitle_PartialMatch(keyword, zhLucenePath);
            }else if((matchOption | PARTIAL_MATCH_CONTENT) != 0){
                results = searchZh.getContent(keyword, zhLucenePath);
            }
        } catch (Exception e) {
            System.out.println("[lucene] Exception raised. Maybe you have a incorrect ZH index path.");
            e.printStackTrace();
        }

        ArrayList<Page> pages = new ArrayList<Page>();
        for(wikipedia_Chinese_index_search.Result result: results){
            Page page = new Page(
                    result.getEnglish_ID(),
                    result.getChinese_ID(),
                    result.getTitle(),
                    1,
                    result.getRedirected_titles());
            pages.add(page);
        }

        return pages;
    }
}

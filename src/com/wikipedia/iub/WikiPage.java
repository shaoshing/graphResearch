package com.wikipedia.iub;

import java.util.ArrayList;

/**
 * Created by shaoshing on 2/6/14.
 */
public class WikiPage {
    public int id;
    public int translationId;
    public WikiClient.LANGUAGE language;

    public String title;
    public String titleTranslation;

    public String content;
    public String contentTranslation;

    public ArrayList<WikiCategory> l1_categories;
    public ArrayList<WikiCategory> l2_categories;
    public ArrayList<WikiCategory> l3_categories;
    public ArrayList<WikiCategory> l4_categories;

    public ArrayList<WikiCategory> indirect_l1_categories;
    public ArrayList<WikiCategory> indirect_l2_categories;

    public WikiPage(){
        this.l1_categories = new ArrayList<WikiCategory>();
        this.l2_categories = new ArrayList<WikiCategory>();
        this.l3_categories = new ArrayList<WikiCategory>();
        this.l4_categories = new ArrayList<WikiCategory>();

        this.indirect_l1_categories = new ArrayList<WikiCategory>();
        this.indirect_l2_categories = new ArrayList<WikiCategory>();
    }
}

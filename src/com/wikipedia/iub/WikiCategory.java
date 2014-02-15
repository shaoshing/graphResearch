package com.wikipedia.iub;

/**
 * Created by shaoshing on 2/6/14.
 */
public class WikiCategory {
    public int id;
    public int translationId;
    public String title;
    public String titleTranslation;
    // repeatCount is for indirect categories, which means the count of same indirect category of a wiki page.
    // For example,
    //  given page that has two categories:
    //  AA1 (level 4, its level 1 category is A)
    //  AA2 (level 5, its level 1 category is A)
    //  its indirect category A has repeatCount of value 2.
    public int repeatCount;
}

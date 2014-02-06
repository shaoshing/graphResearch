package com.wikipedia.iub;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by shaoshing on 2/6/14.
 */

public class WikiClient {

    // Todo: database

    private String dbUser;
    private String dbPassword;
    private String dbHost;
    private String dbPort;
    private String dbName;
    private Connection dbConnection;

    public enum LANGUAGE { ENGLISH, CHINESE}

    public WikiClient(String propertyFilePath) throws IOException {
        Properties prop = new Properties();
        prop.load(new FileInputStream(propertyFilePath));

        dbUser = prop.getProperty("database.user");
        dbName= prop.getProperty("database.name");
        dbPassword = prop.getProperty("database.password");
        dbHost = prop.getProperty("database.host");
        dbPort = prop.getProperty("database.port");
    }

    public WikiPage findByPageId(int pageId, LANGUAGE language){
        ArrayList<WikiPage> pages = findByPageIds(new int[]{pageId}, language);
        return pages.size() > 0 ? pages.get(0) : null;
    }

    static final int LANGUAGE_CODE_ENGLISH = 0;
    static final int LANGUAGE_CODE_CHINESE = 1;
    static final int TYPE_CODE_PAGE = 0;
    static final int TYPE_CODE_CATEGORY = 1;
    public ArrayList<WikiPage> findByPageIds(int[] pageIds, LANGUAGE  language){
        int languageCode = language == LANGUAGE.ENGLISH ? LANGUAGE_CODE_ENGLISH : LANGUAGE_CODE_CHINESE;
        int translateLanguageCode = languageCode == LANGUAGE_CODE_ENGLISH ? LANGUAGE_CODE_CHINESE : LANGUAGE_CODE_ENGLISH;

        String pageIdsSql = "";
        for( int idIndex = 0; idIndex < pageIds.length; idIndex++ ){
            pageIdsSql += String.format("%d", pageIds[idIndex]);
            if(idIndex != pageIds.length-1) pageIdsSql += ", ";
        }

        ArrayList<WikiPage> pages = new ArrayList<WikiPage>();
        try {
            // Find pages
            String selectPageSql = String.format(
                "SELECT pages.id, pages.title, translation.title as titleTranslation, translation.id as translationId FROM pages " +
                "RIGHT JOIN pages translation ON translation.title = pages.translation AND translation.language = %d AND translation.type = pages.type " +
                "WHERE pages.language = %d AND pages.id IN (%s)",
                translateLanguageCode, languageCode, pageIdsSql);
            ResultSet pageResult = dbQuery(selectPageSql);
            while(pageResult.next()){
                WikiPage page = new WikiPage();
                page.id = pageResult.getInt("id");
                page.translationId = pageResult.getInt("translationId");
                page.language = language;
                page.title = pageResult.getString("title");
                page.titleTranslation = pageResult.getString("titleTranslation");

                // Find page contents
                String selectContentSql = String.format(
                        "SELECT language, content FROM page_content WHERE (page_id = %d AND language = %d) OR (page_id = %d AND language = %d)",
                        page.id, languageCode, page.translationId, translateLanguageCode);
                ResultSet pageContentResult = dbQuery(selectContentSql);
                while(pageContentResult.next()){
                    String content = pageContentResult.getString("content");
                    if(languageCode == pageContentResult.getInt("language")){
                        page.content = content;
                    }else{
                        page.contentTranslation = content;
                    }
                }

                // Find page l1 and l2 categories
                String selectCategorySql = String.format(
                        "SELECT category.id, category.title, category.translation, category.level from page_categories " +
                        "RIGHT JOIN pages category ON category.id = page_categories.category_id " +
                        "WHERE category.level IN (1, 2, 3, 4) AND page_categories.page_id = %d AND category.language = %d",
                        page.id, languageCode);
                ResultSet pageCategoryResult = dbQuery(selectCategorySql);
                ArrayList<WikiCategory>[] categoryArray = new ArrayList[]{
                        page.l1_categories, page.l2_categories, page.l3_categories, page.l4_categories};
                while(pageCategoryResult.next()){
                    WikiCategory category = new WikiCategory();
                    category.id = pageCategoryResult.getInt("id");
                    category.title = pageCategoryResult.getString("title");
                    category.titleTranslation = pageCategoryResult.getString("translation");
                    categoryArray[pageCategoryResult.getInt("level")-1].add(category);
                }

                pages.add(page);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }


        return pages;
    }


    private ResultSet dbQuery(String sql){
        Statement statement = null;
        ResultSet result;
        try {
            Connection connection = getConnection();
            statement = connection.createStatement();
            result = statement.executeQuery(sql);
            // statement.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return result;
    }

    private Connection getConnection() throws SQLException, ClassNotFoundException {
        if(dbConnection == null){
            Class.forName("com.mysql.jdbc.Driver");
            String connectionString = String.format("jdbc:mysql://%s:%s/%s?user=%s&password=%s&characterEncoding=%s",
                    dbHost, dbPort, dbName, dbUser, dbPassword, "utf8");
            dbConnection = DriverManager.getConnection(connectionString);
        }
        return dbConnection;
    }

}

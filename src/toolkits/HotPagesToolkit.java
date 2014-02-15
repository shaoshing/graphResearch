package toolkits;

import com.wikipedia.iub.WikiCategory;
import com.wikipedia.iub.WikiClient;
import com.wikipedia.iub.WikiPage;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Created by shaoshing on 2/6/14.
 */
public class HotPagesToolkit {
    static final int FIELD_INDEX_ID = 0;
    static final int FIELD_INDEX_TITLE = 1;
    public static void generateHotPagesTxt(String hotPagesFile){

        try {
            WikiClient wikiClient = new WikiClient("configs/wikipedia.property");
            BufferedReader fileReader = new BufferedReader(new FileReader(hotPagesFile));
            String line = null;
            String hotPagesTxt = "";
            PrintWriter writer = new PrintWriter(Main.OUTPUT_FOLDER+"/hot-pages.txt", "UTF-8");
            while((line = fileReader.readLine()) != null){
                String[] fields = line.split("\\|");
                System.out.println(fields[FIELD_INDEX_TITLE]);
                WikiPage page = wikiClient.findByPageId(Integer.parseInt(fields[FIELD_INDEX_ID]), WikiClient.LANGUAGE.ENGLISH);
                if(page == null) continue;

                line += "|" + page.titleTranslation;

                ArrayList<WikiCategory>[] categoriesArray = new ArrayList[]{
                        page.l1_categories, page.l2_categories,
                        page.indirect_l1_categories, page.indirect_l2_categories};
                for(ArrayList<WikiCategory> categories : categoriesArray){
                    String categoryStr = "";
                    for(int i = 0; i < categories.size(); i++){
                        WikiCategory category = categories.get(i);
                        categoryStr += Integer.toString(category.id) + ":" + category.title +
                                ":" + category.titleTranslation + ":" + Integer.toString(category.repeatCount);
                        if(i != categories.size()-1) categoryStr += ", ";
                    }
                    line += "|" + categoryStr;
                }
                writer.println(line);
            };
            writer.println("");
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

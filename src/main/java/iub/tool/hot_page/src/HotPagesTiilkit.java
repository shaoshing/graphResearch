
import iub.api.wikipedia.WikiCategory;
import iub.api.wikipedia.WikiClient;
import iub.api.wikipedia.WikiPage;

import java.io.*;
import java.util.ArrayList;

/**
 * Created by shaoshing on 2/6/14.
 */
class HotPagesToolkit {
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

                line += "|";
                for(int i = 0; i < page.redirectedTitles.size(); i++){
                    line += page.redirectedTitles.get(i);
                    if(i != page.redirectedTitles.size()-1) line += ", ";
                }
                line += "|";
                for(int i = 0; i < page.redirectedTitleTranslations.size(); i++){
                    line += page.redirectedTitleTranslations.get(i);
                    if(i != page.redirectedTitleTranslations.size()-1) line += ", ";
                }

                writer.println(line);
            };
            writer.println("");
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class Main {

        static final String OUTPUT_FOLDER = "output";

        static public void main(String[] args) throws IOException {
            System.out.println("Input command:");
            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

            String command = input.readLine().trim();

            if(command.equals("hotpage")){
                System.out.println("Input file path:");
                String filePath = input.readLine().trim();
                generateHotPagesTxt(filePath);
            }


        }
    }
}

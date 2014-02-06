/**
 * Created by shaoshing on 2/6/14.
 */

import com.wikipedia.iub.*;

import java.io.IOException;

public class Run {
    static public void main(String[] args){
        try {
            WikiClient wikiClient = new WikiClient("configs/wikipedia.property");
            WikiPage page = wikiClient.findByPageId(12, WikiClient.LANGUAGE.ENGLISH);
            if(page == null) return;

            System.out.printf("Id: %d \n", page.id);
            System.out.printf("Title: %s \n", page.title);
            System.out.printf("Title Translation: %s \n", page.titleTranslation);
            if(page.content != null){
                System.out.printf("Content: %s \n", page.content.substring(0, Math.min(page.content.length(), 100)));
            }
            if(page.contentTranslation != null){
                System.out.printf("Content Translation: %s \n", page.contentTranslation.substring(0, Math.min(page.contentTranslation.length(), 100)));
            }
            System.out.printf("L1 categories: %d \n", page.l1_categories.size());
            System.out.printf("L2 categories: %d \n", page.l2_categories.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

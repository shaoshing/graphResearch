/**
 * Created by shaoshing on 2/6/14.
 */

import iub.api.graph.CrossLanguageGraph;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Properties;

public class Main {

    static final String OUTPUT_FOLDER = "output";

    static public void main(String[] args) throws IOException {
        if (args.length < 1){
            System.out.println("Please specify the path of the config file. Example: java -jar cross_graph.jar path/to/the/file.property");
            return;
        }

        String configPath = args[0];
        Boolean rebuild = false;
        if (args.length == 2 && args[1].equals("rebuild")){
            rebuild = true;
        }

        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream(configPath));
        } catch (IOException e) {
            System.out.printf("Unable to locate config file at %s \n", configPath);
            return;
        }

        System.out.println("\nLoading keywords");
        ArrayList<String> enKeywords = loadKeywords(prop.getProperty("graph.en_keywords.path"),
                prop.getProperty("graph.en_keywords.column", "1"));
        ArrayList<String> zhKeywords = loadKeywords(prop.getProperty("graph.zh_keywords.path"),
                prop.getProperty("graph.en_keywords.column", "1"));
        System.out.printf(" -- en keywords %d\n", enKeywords.size());
        System.out.printf(" -- zh keywords %d\n", zhKeywords.size());
        if(enKeywords.size() == 0 || zhKeywords.size() == 0){
            return;
        }

        System.out.println("\nCreating graph");
        CrossLanguageGraph graph = new CrossLanguageGraph(prop);
        if(rebuild){
            System.out.println(" -- truncating Neo4j Database");
            graph.truncateNeo4jDatabase();
        }

        int relationOption = Integer.parseInt(prop.getProperty("graph.match_type", "1"));

        ArrayList<Integer> excludedCategoryIds = loadCategoryIds(prop.getProperty("graph.exclude_categories", ""));

        graph.createGraphByKeywords(enKeywords, zhKeywords, relationOption, excludedCategoryIds);
        System.out.println("\nDone!");
    }

    static private ArrayList<String> loadKeywords(String path, String csvColumn) throws IOException {
        InputStream fis = null;
        try {
            fis = new FileInputStream(path);
        } catch (FileNotFoundException e) {
            System.out.printf(" -- Unable to load keyword from %s\n", path);
            throw e;
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));

        int index = Integer.parseInt(csvColumn)-1;
        String keyword;
        ArrayList<String> enKeywords = new ArrayList<String>();
        while ((keyword = br.readLine()) != null) {
            String[] fields = keyword.split(",");
            if(fields.length < index+1){
                System.out.println(" -- not enough columns: " + keyword);
                continue;
            }
            enKeywords.add(fields[index]);
        }
        br.close();

        return enKeywords;
    }

    static private ArrayList<Integer> loadCategoryIds(String path) throws IOException {
        ArrayList<Integer> ids = new ArrayList<Integer>();
        if(path.equals("")){
            return ids;
        }

        InputStream fis = null;
        try {
            fis = new FileInputStream(path);
        } catch (FileNotFoundException e) {
            System.out.printf(" -- Unable to load category id CSV from %s\n", path);
            throw e;
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));

        String id;
        while ((id = br.readLine()) != null) {
            ids.add(Integer.parseInt(id));
        }
        br.close();
        System.out.printf(" -- %d categories to be excluded.\n", ids.size());

        return ids;
    }
}

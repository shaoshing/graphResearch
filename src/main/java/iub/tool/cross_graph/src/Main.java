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
        if (args.length != 1){
            System.out.println("Please specify the path of the config file. Example: java -jar cross_graph.jar path/to/the/file.property");
            return;
        }

        String configPath = args[0];
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream(configPath));
        } catch (IOException e) {
            System.out.printf("Unable to locate config file at %s \n", configPath);
            return;
        }

        System.out.println("\nLoading keywords");
        ArrayList<String> enKeywords = loadKeywords(prop.getProperty("graph.keywords.en"));
        ArrayList<String> zhKeywords = loadKeywords(prop.getProperty("graph.keywords.zh"));
        System.out.printf(" -- en keywords %d\n", enKeywords.size());
        System.out.printf(" -- zh keywords %d\n", zhKeywords.size());

        System.out.println("\nCreating graph");
        CrossLanguageGraph graph = new CrossLanguageGraph(prop);
        int relationOption = Integer.parseInt(prop.getProperty("graph.match_type", "1"));
        graph.createGraphByKeywords(enKeywords, zhKeywords, relationOption);

        System.out.println("\nDone!");
    }

    static private ArrayList<String> loadKeywords(String path) throws IOException {
        InputStream fis = null;
        try {
            fis = new FileInputStream(path);
        } catch (FileNotFoundException e) {
            System.out.printf(" -- Unable to load keyword from %s\n", path);
            throw e;
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));

        String keyword;
        ArrayList<String> enKeywords = new ArrayList<String>();
        while ((keyword = br.readLine()) != null) {
            enKeywords.add(keyword);
        }
        br.close();

        return enKeywords;
    }
}

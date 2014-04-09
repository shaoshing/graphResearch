package iub.toolkits;

/**
 * Created by shaoshing on 2/6/14.
 */

import iub.api.wikipedia.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {

    static final String OUTPUT_FOLDER = "output";

    static public void main(String[] args) throws IOException {
        System.out.println("Input command:");
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

        String command = input.readLine().trim();

        if(command.equals("hotpage")){
            System.out.println("Input file path:");
            String filePath = input.readLine().trim();
            HotPagesToolkit.generateHotPagesTxt(filePath);
        }


    }
}

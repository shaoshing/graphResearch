package iub.api.graph;

/**
 * Created by shaoshing on 4/8/14.
 */
import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class Neo4jClient {

    private String serverUri;
    static private String CACHE_FILE_PATH = System.getProperty("java.io.tmpdir") + "grpah-neo4j.cache";
    private HashMap<Integer, Boolean> cypherCache;

    public Neo4jClient(String serverUri){
        this.serverUri = serverUri;
        System.out.println(CACHE_FILE_PATH);

        cypherCache = new HashMap<Integer, Boolean>();
        loadCache();
    }

    // NEO4J REST API: http://docs.neo4j.org/chunked/stable/rest-api-cypher.html
    public void execute(String cypherQuery){
        if(cypherCache.get(cypherQuery.hashCode()) != null){
            return;
        }

        WebResource resource = Client.create().resource( this.serverUri+"cypher" );
        String query = JSONObject.escape(cypherQuery);
        ClientResponse neo4jResponse = resource.accept( "application/json" ).type( "application/json" )
                .entity( "{\"query\" : \""+query+"\", \"params\" : {}}" )
                .post( ClientResponse.class );

        String cypherResult = neo4jResponse.getEntity( String.class );
        neo4jResponse.close();

        if(neo4jResponse.getStatus() != 200){
            System.out.println("[neo4j] InvalidSyntax");
            System.out.println(cypherQuery);
        }

        cypherCache.put(cypherQuery.hashCode(), true);
        saveCache();
    }

    public boolean testConnection(){
        try{
            WebResource resource = Client.create().resource( this.serverUri );
            ClientResponse neo4jResponse = resource.post( ClientResponse.class );
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public void loadCache(){
        File cacheFile = new File(CACHE_FILE_PATH);

        if(!cacheFile.exists()){
            return;
        }

        try{
            FileInputStream fileIn = new FileInputStream(CACHE_FILE_PATH);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            cypherCache = (HashMap<Integer, Boolean>) in.readObject();
            in.close();
            fileIn.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void resetCache(){
        cypherCache = new HashMap<Integer, Boolean>();
        saveCache();
    }

    private void saveCache(){
        try{
            FileOutputStream fileOut = new FileOutputStream(CACHE_FILE_PATH);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(cypherCache);
            out.close();
            fileOut.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
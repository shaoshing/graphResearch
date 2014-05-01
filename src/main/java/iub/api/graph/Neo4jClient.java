package iub.api.graph;

/**
 * Created by shaoshing on 4/8/14.
 */
import java.util.ArrayList;
import java.util.HashMap;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class Neo4jClient {

    private String serverUri;

    public Neo4jClient(String serverUri){
        this.serverUri = serverUri;
    }

    private HashMap<Integer, Boolean> cypherCache = new HashMap<Integer, Boolean>();

    // NEO4J REST API: http://docs.neo4j.org/chunked/stable/rest-api-cypher.html
    public void query(String cypherQuery){
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
}
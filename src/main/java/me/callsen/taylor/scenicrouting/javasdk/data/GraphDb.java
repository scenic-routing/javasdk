package me.callsen.taylor.scenicrouting.javasdk.data;

import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import me.callsen.taylor.scenicrouting.javasdk.RoutingConstants;

public class GraphDb {
  
  private GraphDatabaseService db;
  private DatabaseManagementService managementService;

  public GraphDb(String graphDbPath) {
    // initialize graph db connection
    managementService = new DatabaseManagementServiceBuilder( Paths.get( graphDbPath ) ).build();
    db = managementService.database( DEFAULT_DATABASE_NAME );
    // db = new GraphDatabaseFactory().newEmbeddedDatabase( new File( graphDbPath ) );
    System.out.println("Graph DB @ " + graphDbPath + " initialized");    
  }

  public Transaction getTransaction() {
    return db.beginTx();
  }

  public void shutdown(){
    this.managementService.shutdown();
    System.out.println("Graph DB shutdown");
  }

  public long getRelationshipCount() {

    long count = 0;

    Transaction tx = this.db.beginTx();
    try ( Result result = tx.execute( "MATCH ()-[r]-() RETURN COUNT(DISTINCT(r)) AS total" ) ) {
      while ( result.hasNext() ) {
        Map<String, Object> row = result.next();
        count = (Long) row.get("total");
      }
    } finally {
      tx.close();  
    }

    return count;
  }

  public Result getRelationshipPage(Transaction tx, int pageNumber) {
    long startIndex = pageNumber * RoutingConstants.GRAPH_RELATIONSHIP_PAGINATION_AMOUNT;
    Result result = tx.execute( String.format("MATCH ()-[r]-() RETURN DISTINCT(r) as way ORDER BY r.osm_id DESC SKIP %s LIMIT %s", startIndex, RoutingConstants.GRAPH_RELATIONSHIP_PAGINATION_AMOUNT ) );
    return result;
  }

  public void setAssociatedData(Relationship relationship, String propertyName, JSONObject associatedData) {
    // add property to associatedData list
    addAssociatedDataProperty(relationship, propertyName);

    JSONArray associatedDataArray = new JSONArray();
    associatedDataArray.put(associatedData);

    // add JSON data to associated data property 
    relationship.setProperty(propertyName, associatedDataArray.toString());
  }

  public void setAssociatedData(Relationship relationship, String propertyName, JSONArray associatedData) {
    // add property to associatedData list
    addAssociatedDataProperty(relationship, propertyName);

    // add JSON data to associated data property 
    relationship.setProperty(propertyName, associatedData.toString());
  }

  private void addAssociatedDataProperty(Relationship relationship, String propertyName) {
    // add referenced associated data property to associatedData array (or create one of doesn't exist yet)
    String[] associatedDataArray;
    if (relationship.hasProperty(RoutingConstants.GRAPH_ASSOCIATED_DATA_PROPERTY)) {
      ArrayList<String> existingAssociatedDataList = new ArrayList<String>(Arrays.asList((String[]) relationship.getProperty(RoutingConstants.GRAPH_ASSOCIATED_DATA_PROPERTY)));
      // prevent duplicates - don't add property if already exists in the list
      if (!existingAssociatedDataList.contains(propertyName)) {
        existingAssociatedDataList.add(propertyName);
      }
      associatedDataArray = existingAssociatedDataList.stream().toArray(String[]::new);
    } else {
      associatedDataArray = new String[1];
      associatedDataArray[0] = propertyName ;
    }
    relationship.setProperty(RoutingConstants.GRAPH_ASSOCIATED_DATA_PROPERTY, associatedDataArray);
  }

}
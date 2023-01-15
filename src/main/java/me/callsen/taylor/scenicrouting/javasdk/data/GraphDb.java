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
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.values.storable.CoordinateReferenceSystem;
import org.neo4j.values.storable.PointValue;
import org.neo4j.values.storable.Values;

import me.callsen.taylor.scenicrouting.javasdk.RoutingConstants;
import me.callsen.taylor.scenicrouting.javasdk.RoutingConstants.NodeLabels;
import me.callsen.taylor.scenicrouting.javasdk.RoutingConstants.RelationshipTypes;

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
    if (relationship.hasProperty(RoutingConstants.GRAPH_PROPERTY_NAME_ASSOCIATED_DATA)) {
      ArrayList<String> existingAssociatedDataList = new ArrayList<String>(Arrays.asList((String[]) relationship.getProperty(RoutingConstants.GRAPH_PROPERTY_NAME_ASSOCIATED_DATA)));
      // prevent duplicates - don't add property if already exists in the list
      if (!existingAssociatedDataList.contains(propertyName)) {
        existingAssociatedDataList.add(propertyName);
      }
      associatedDataArray = existingAssociatedDataList.stream().toArray(String[]::new);
    } else {
      associatedDataArray = new String[1];
      associatedDataArray[0] = propertyName ;
    }
    relationship.setProperty(RoutingConstants.GRAPH_PROPERTY_NAME_ASSOCIATED_DATA, associatedDataArray);
  }

  public void truncateGraphNodes() {
    
    System.out.println("truncating graph nodes..");
    
    try ( Transaction tx = this.getTransaction() ) {
      tx.execute( "MATCH (n) DETACH DELETE n" );
      tx.commit();
    } catch (Exception e) {
      System.out.println("failed to truncateGraph nodes"); 
      e.printStackTrace();
    }
    
  }
  
  public void truncateGraphRelationships() {
    
    System.out.println("truncating graph relationships..");
    
    try ( Transaction tx = this.getTransaction() ) {
      tx.execute( "MATCH (a)-[r]-(b) DETACH DELETE r" );
      tx.commit();
    } catch (Exception e) {
      System.out.println("failed to truncateGraph relationships"); 
      e.printStackTrace();
    }

  }
  
  public void createNodeIndexes() {

    System.out.println("creating node index for quick retrieval with osm_id and geom");

    //create node to create index off of
    try ( Transaction tx = this.getTransaction() ) {
      Node indexNode = tx.createNode(NodeLabels.INTERSECTION);
      indexNode.setProperty(RoutingConstants.GRAPH_PROPERTY_NAME_OSM_ID, "indexNode");
      indexNode.setProperty(RoutingConstants.GRAPH_PROPERTY_NAME_GEOM, Values.pointValue(CoordinateReferenceSystem.get(4326), 50d, 50d));
      tx.commit();
    } catch (Exception e) {
      System.out.println("failed to create index node!"); 
      e.printStackTrace();
    }

    //create osm_id index
    try ( Transaction tx = this.getTransaction() ) {
      String cypherString = String.format("CREATE INDEX ON :%s", RoutingConstants.GRAPH_INDEX_NAME_INTERSECTION_OSM_ID);
      tx.execute(cypherString);
      tx.commit();
    } catch (Exception e) {
      System.out.println("failed to create index!"); 
      e.printStackTrace();
    }

    //create point index - https://neo4j.com/docs/cypher-manual/current/syntax/spatial/#spatial-values-point-index
    try ( Transaction tx = this.getTransaction() ) {
      String cypherString = String.format("CREATE POINT INDEX %s FOR (n:%s) ON (n.%s)", 
          RoutingConstants.GRAPH_INDEX_NAME_INTERSECTION_GEOM_POINT,
          RoutingConstants.NodeLabels.INTERSECTION,
          RoutingConstants.GRAPH_PROPERTY_NAME_GEOM);
      tx.execute(cypherString);
      tx.commit();
    } catch (Exception e) {
      System.out.println("failed to create index!");
      e.printStackTrace();
    }

    //delete node that index was created with
    try ( Transaction tx = this.getTransaction() ) {
      Node indexNode = tx.findNode( NodeLabels.INTERSECTION , RoutingConstants.GRAPH_PROPERTY_NAME_OSM_ID, "indexNode" );
      indexNode.delete();
      tx.commit();
    } catch (Exception e) {
      System.out.println("failed to delete index node!"); 
      e.printStackTrace();
    }

  }

  public void createRelationshipIndexes() {

    System.out.println("creating relationship index for quick retrieval with geom");

    // create relationship to create index off of
    try ( Transaction tx = this.getTransaction() ) {
      Node startNode = tx.createNode(NodeLabels.INTERSECTION);
      Node endNode = tx.createNode(NodeLabels.INTERSECTION);
      Relationship rel = startNode.createRelationshipTo(endNode, RelationshipTypes.CONNECTS);

      // used for lookup during delection
      startNode.setProperty(RoutingConstants.GRAPH_PROPERTY_NAME_OSM_ID, "start");
      endNode.setProperty(RoutingConstants.GRAPH_PROPERTY_NAME_OSM_ID, "end");
      rel.setProperty(RoutingConstants.GRAPH_PROPERTY_NAME_OSM_ID, "rel");

      // set mock point array as geom
      PointValue[] points = new PointValue[2];
      points[0] = Values.pointValue(CoordinateReferenceSystem.get(4326), 50d, 50d);
      points[1] = Values.pointValue(CoordinateReferenceSystem.get(4326), 51d, 51d);
      rel.setProperty(RoutingConstants.GRAPH_PROPERTY_NAME_GEOM, points);

      tx.commit();
    } catch (Exception e) {
      System.out.println("failed to create index relationship!");
      e.printStackTrace();
    }

    // create point index on geom - https://neo4j.com/docs/cypher-manual/current/syntax/spatial/#spatial-values-point-index
    try ( Transaction tx = this.getTransaction() ) {
      String cypherString = String.format("CREATE POINT INDEX %s FOR ()-[r:%s]-() ON (r.%s)" , 
          RoutingConstants.GRAPH_INDEX_NAME_WAY_GEOM_POINT,
          RoutingConstants.RelationshipTypes.CONNECTS,
          RoutingConstants.GRAPH_PROPERTY_NAME_GEOM);
      tx.execute(cypherString);
      tx.commit();
    } catch (Exception e) {
      System.out.println("failed to create relationship index!");
      e.printStackTrace();
    }

    // delete relationship that index was created with
    try ( Transaction tx = this.getTransaction() ) {
      Relationship rel = tx.findRelationship( RelationshipTypes.CONNECTS , RoutingConstants.GRAPH_PROPERTY_NAME_OSM_ID, "rel" );
      rel.delete();
      Node startNode = tx.findNode( NodeLabels.INTERSECTION , RoutingConstants.GRAPH_PROPERTY_NAME_OSM_ID, "start" );
      startNode.delete();
      Node endNode = tx.findNode( NodeLabels.INTERSECTION , RoutingConstants.GRAPH_PROPERTY_NAME_OSM_ID, "end" );
      endNode.delete();
      tx.commit();
    } catch (Exception e) {
      System.out.println("failed to delete relationship index nodes/rel!"); 
      e.printStackTrace();
    }

  }
  
  public void dropNodeIndexes() {
    
    System.out.println("dropping node index for quick retrieval with osm_Id and geom");
    
    //drop index if exists
    try ( Transaction tx = this.getTransaction() ) {
      tx.execute( String.format("DROP INDEX ON :%s", RoutingConstants.GRAPH_INDEX_NAME_INTERSECTION_OSM_ID) );
      tx.execute( String.format("DROP INDEX %s", RoutingConstants.GRAPH_INDEX_NAME_INTERSECTION_GEOM_POINT) );
      tx.commit();
    } catch (Exception e) {
      System.out.println("warning - failed to drop osm_Id index; index may not exist (not necessarily an issue)");
    }

  }

  public void dropRelationshipIndexes() {

    System.out.println("dropping relationship index for quick retrieval with geom");

    //drop index if exists
    try ( Transaction tx = this.getTransaction() ) {
      tx.execute( String.format("DROP INDEX %s", RoutingConstants.GRAPH_INDEX_NAME_WAY_GEOM_POINT) );
      tx.commit();
    } catch (Exception e) {
      System.out.println("warning - failed to drop relationship geom index; index may not exist (not necessarily an issue)");
    }

  }

}
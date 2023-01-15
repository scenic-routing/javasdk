package me.callsen.taylor.scenicrouting.javasdk.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import me.callsen.taylor.scenicrouting.javasdk.RoutingConstants;
import me.callsen.taylor.scenicrouting.javasdk.TestUtils;

@TestInstance(Lifecycle.PER_CLASS)
public class GraphDbIndexTest {

  private GraphDb db;

  @BeforeAll
  public void initResources() throws Exception {
    db = TestUtils.getEmptyGraphDb();
  }

  @AfterAll
  public void shutdownResources() {
    db.shutdown();
  }

  @Test
  public void testNodeIndexes() throws Exception {
    
    // confirm indexes created
    db.createNodeIndexes();

    try ( Transaction tx = db.getTransaction() ) {
      boolean foundNodeIntersectionOsmId = false;
      boolean foundNodeIntersectionGeomPoint = false;

      Result result = tx.execute("CALL db.indexes();");
      while ( result.hasNext() ) {
        Map<String, Object> row = result.next();
        ArrayList<String> labelsOrTypes = (ArrayList<String>) row.get("labelsOrTypes");
        ArrayList<String> properties = (ArrayList<String>) row.get("properties");

        // node intersection osm_id
        if (checkRowForNodeIntersectionOsmId(row, labelsOrTypes, properties)) {
          foundNodeIntersectionOsmId = true;
        }

        // node intersection geom point
        if (checkRowForNodeIntersectionGeomPoint(row, labelsOrTypes, properties)) {
          foundNodeIntersectionGeomPoint = true;
        }
      }

      assertTrue(foundNodeIntersectionOsmId);
      assertTrue(foundNodeIntersectionGeomPoint);
      tx.close();
    }

    // confirm index dropped
    db.dropNodeIndexes();

    try ( Transaction tx = db.getTransaction() ) {
      
      boolean foundNodeIntersectionOsmId = false;
      boolean foundNodeIntersectionGeomPoint = false;
      Result result = tx.execute("CALL db.indexes();");
      while ( result.hasNext() ) {
        Map<String, Object> row = result.next();
        ArrayList<String> labelsOrTypes = (ArrayList<String>) row.get("labelsOrTypes");
        ArrayList<String> properties = (ArrayList<String>) row.get("properties");

        // node intersection osm_id
        if (checkRowForNodeIntersectionOsmId(row, labelsOrTypes, properties)) {
          foundNodeIntersectionOsmId = true;
        }

        // node intersection geom point
        if (checkRowForNodeIntersectionGeomPoint(row, labelsOrTypes, properties)) {
          foundNodeIntersectionGeomPoint = true;
        }
      }

      assertFalse(foundNodeIntersectionOsmId);
      assertFalse(foundNodeIntersectionGeomPoint);
      tx.close();
    }
  }

  @Test
  public void testRelationshipIndexes() throws Exception {
    
    // confirm indexes created
    db.createRelationshipIndexes();

    try ( Transaction tx = db.getTransaction() ) {
      boolean foundRelationShipWayGeomPoint = false;

      Result result = tx.execute("CALL db.indexes();");
      while ( result.hasNext() ) {
        Map<String, Object> row = result.next();
        ArrayList<String> labelsOrTypes = (ArrayList<String>) row.get("labelsOrTypes");
        ArrayList<String> properties = (ArrayList<String>) row.get("properties");

        // relationship connects geom point
        if (checkRowForRelationShipWayGeomPoint(row, labelsOrTypes, properties)) {
          foundRelationShipWayGeomPoint = true;
        }
      }

      assertTrue(foundRelationShipWayGeomPoint);
      tx.close();
    }

    // confirm indexes dropped
    db.dropRelationshipIndexes();

    try ( Transaction tx = db.getTransaction() ) {
      boolean foundRelationShipWayGeomPoint = false;

      Result result = tx.execute("CALL db.indexes();");
      while ( result.hasNext() ) {
        Map<String, Object> row = result.next();
        ArrayList<String> labelsOrTypes = (ArrayList<String>) row.get("labelsOrTypes");
        ArrayList<String> properties = (ArrayList<String>) row.get("properties");

        // relationship connects geom point
        if (checkRowForRelationShipWayGeomPoint(row, labelsOrTypes, properties)) {
          foundRelationShipWayGeomPoint = true;
        }
      }

      assertFalse(foundRelationShipWayGeomPoint);
      tx.close();
    }

  }

  private static boolean checkRowForNodeIntersectionOsmId(Map<String, Object> row, ArrayList<String> labelsOrTypes, ArrayList<String> properties) {
    return row.get("entityType").equals("NODE") && 
        labelsOrTypes.contains(RoutingConstants.NodeLabels.INTERSECTION.toString()) &&
        properties.contains(RoutingConstants.GRAPH_PROPERTY_NAME_OSM_ID);
  }

  private static boolean checkRowForNodeIntersectionGeomPoint(Map<String, Object> row, ArrayList<String> labelsOrTypes, ArrayList<String> properties) {
    return row.get("name").equals(RoutingConstants.GRAPH_INDEX_NAME_INTERSECTION_GEOM_POINT) &&
        row.get("type").equals("POINT") && 
        row.get("entityType").equals("NODE") && 
        labelsOrTypes.contains(RoutingConstants.NodeLabels.INTERSECTION.toString()) &&
        properties.contains(RoutingConstants.GRAPH_PROPERTY_NAME_GEOM);
  }

  private static boolean checkRowForRelationShipWayGeomPoint(Map<String, Object> row, ArrayList<String> labelsOrTypes, ArrayList<String> properties) {
    return row.get("name").equals(RoutingConstants.GRAPH_INDEX_NAME_WAY_GEOM_POINT) &&
        row.get("type").equals("POINT") && 
        row.get("entityType").equals("RELATIONSHIP") && 
        labelsOrTypes.contains(RoutingConstants.RelationshipTypes.CONNECTS.toString()) &&
        properties.contains(RoutingConstants.GRAPH_PROPERTY_NAME_GEOM);
  }

}

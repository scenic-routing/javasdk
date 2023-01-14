package me.callsen.taylor.scenicrouting.javasdk.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import me.callsen.taylor.scenicrouting.javasdk.RoutingConstants;
import me.callsen.taylor.scenicrouting.javasdk.TestUtils;;

@TestInstance(Lifecycle.PER_CLASS)
public class GraphDbTest {

  private GraphDb db;

  @BeforeAll
  public void initResources() throws Exception {
    db = TestUtils.getLoadedGraphDb();
  }

  @AfterAll
  public void shutdownResources() {
    db.shutdown();
  }

  @Test
  public void testDbRelCount() throws Exception {
    assertEquals(676, db.getRelationshipCount());
  }

  @Test
  public void testSetAssociatedDataSingleProp() throws Exception {
    Transaction tx = db.getTransaction();
    Result result = tx
        .execute("MATCH ()-[r]-() WHERE r.start_osm_id=65312481 AND r.end_osm_id=65312480 return DISTINCT(r)");
    while (result.hasNext()) {
      Map<String, Object> row = result.next();
      Relationship rel = (Relationship) row.get("r");

      // set single property
      db.setAssociatedData(rel, "myProp", new JSONArray("[{\"my\":\"data\"}]"));

      String[] associatedDataProps = (String[]) rel.getProperty(RoutingConstants.GRAPH_ASSOCIATED_DATA_PROPERTY);
      assertEquals(1, associatedDataProps.length);
      assertEquals("myProp", associatedDataProps[0]);

      JSONArray associatedDataArray = new JSONArray((String) rel.getProperty("myProp"));
      JSONObject associatedData = associatedDataArray.getJSONObject(0);
      assertEquals("data", associatedData.getString("my"));
    }
    tx.rollback();
    tx.close();
  }

  @Test
  public void testSetAssociatedDataMultipleProp() throws Exception {
    Transaction tx = db.getTransaction();
    Result result = tx
        .execute("MATCH ()-[r]-() WHERE r.start_osm_id=65312481 AND r.end_osm_id=65312480 return DISTINCT(r)");
    while (result.hasNext()) {
      Map<String, Object> row = result.next();
      Relationship rel = (Relationship) row.get("r");

      // set single property
      db.setAssociatedData(rel, "myProp", new JSONArray("[{\"my\":\"data\"}]"));
      db.setAssociatedData(rel, "myProp2", new JSONArray("[{\"my2\":\"data2\"}]"));

      String[] associatedDataProps = (String[]) rel.getProperty(RoutingConstants.GRAPH_ASSOCIATED_DATA_PROPERTY);
      assertEquals(2, associatedDataProps.length);
      assertEquals("myProp", associatedDataProps[0]);
      assertEquals("myProp2", associatedDataProps[1]);

      // confirm myProp and myProp2 properties both set
      JSONArray associatedDataArray = new JSONArray((String) rel.getProperty("myProp"));
      JSONObject associatedData = associatedDataArray.getJSONObject(0);
      assertEquals("data", associatedData.getString("my"));
      associatedDataArray = new JSONArray((String) rel.getProperty("myProp2"));
      associatedData = associatedDataArray.getJSONObject(0);
      assertEquals("data2", associatedData.getString("my2"));
    }
    tx.rollback();
    tx.close();
  }

  @Test
  public void testSetAssociatedDataMultipleSameProp() throws Exception {
    Transaction tx = db.getTransaction();
    Result result = tx
        .execute("MATCH ()-[r]-() WHERE r.start_osm_id=65312481 AND r.end_osm_id=65312480 return DISTINCT(r)");
    while (result.hasNext()) {
      Map<String, Object> row = result.next();
      Relationship rel = (Relationship) row.get("r");

      // set single property twice
      db.setAssociatedData(rel, "myProp", new JSONArray("[{\"my\":\"data\"}]"));
      db.setAssociatedData(rel, "myProp", new JSONArray("[{\"my2\":\"data2\"}]"));

      // confirm property is only added to associatedData once
      String[] associatedDataProps = (String[]) rel.getProperty(RoutingConstants.GRAPH_ASSOCIATED_DATA_PROPERTY);
      assertEquals(1, associatedDataProps.length);
      assertEquals("myProp", associatedDataProps[0]);

      // should still overwrite myProp
      JSONArray associatedDataArray = new JSONArray((String) rel.getProperty("myProp"));
      JSONObject associatedData = associatedDataArray.getJSONObject(0);
      assertEquals("data2", associatedData.getString("my2"));
    }
    tx.rollback();
    tx.close();
  }

  @Test
  public void testSetAssociatedDataJSONObject() throws Exception {
    Transaction tx = db.getTransaction();
    Result result = tx
        .execute("MATCH ()-[r]-() WHERE r.start_osm_id=65312481 AND r.end_osm_id=65312480 return DISTINCT(r)");
    while (result.hasNext()) {
      Map<String, Object> row = result.next();
      Relationship rel = (Relationship) row.get("r");

      // set single property
      db.setAssociatedData(rel, "myProp", new JSONObject("{\"my\":\"data\"}"));
      db.setAssociatedData(rel, "myProp2", new JSONObject("{\"my2\":\"data2\"}"));

      String[] associatedDataProps = (String[]) rel.getProperty(RoutingConstants.GRAPH_ASSOCIATED_DATA_PROPERTY);
      assertEquals(2, associatedDataProps.length);
      assertEquals("myProp", associatedDataProps[0]);
      assertEquals("myProp2", associatedDataProps[1]);

      // JSONObjects can be added separately and will be combined
      JSONArray associatedDataArray = new JSONArray((String) rel.getProperty("myProp"));
      JSONObject associatedData = associatedDataArray.getJSONObject(0);
      assertEquals("data", associatedData.getString("my"));
      associatedDataArray = new JSONArray((String) rel.getProperty("myProp2"));
      associatedData = associatedDataArray.getJSONObject(0);
      assertEquals("data2", associatedData.getString("my2"));
    }
    tx.rollback();
    tx.close();
  }

  @Test
  public void testSetAssociatedDataOverwrite() throws Exception {
    Transaction tx = db.getTransaction();
    Result result = tx
        .execute("MATCH ()-[r]-() WHERE r.start_osm_id=65312481 AND r.end_osm_id=65312480 return DISTINCT(r)");
    while (result.hasNext()) {
      Map<String, Object> row = result.next();
      Relationship rel = (Relationship) row.get("r");

      // set single property
      db.setAssociatedData(rel, "myProp", new JSONObject("{\"my\":\"data\"}"));
      db.setAssociatedData(rel, "myProp", new JSONObject("{\"my2\":\"data2\"}"));

      String[] associatedDataProps = (String[]) rel.getProperty(RoutingConstants.GRAPH_ASSOCIATED_DATA_PROPERTY);
      assertEquals(1, associatedDataProps.length);
      assertEquals("myProp", associatedDataProps[0]); // will already have ad_elevation added & commited from first test

      // confirm myProp will be overwritten
      JSONArray associatedDataArray = new JSONArray((String) rel.getProperty("myProp"));
      JSONObject associatedData = associatedDataArray.getJSONObject(0);
      assertEquals("data2", associatedData.getString("my2"));
    }
    tx.rollback();
    tx.close();
  }

}

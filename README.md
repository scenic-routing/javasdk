# Scenic Routing Java SDK

Tools to help with the development of Scenic Routing applications using Java. Currently includes: 

- Wrapper class (`GraphDb`) to help with the initialization of embedded Neo4j databases, and the read/write of `assocaitedData` (used by routing scrorers). 
- Constants and testing utilities to help with the creation of unit tests (e.g. `TestUtils.getLoadedGraph()` returns a `GraphDb` already with nodes and relationships).

## Include

To include this module as a Maven dependency, first add the [JitPack](https://jitpack.io/) repository to your `pom.xml` file:

```
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>
```

Next, add the dependency entry for this module:

```
<dependency>
  <groupId>com.github.scenic-routing</groupId>
  <artifactId>javasdk</artifactId>
  <version>1.0.0</version>
</dependency>
```

Optionally, add a dependency entry for the `test-jar` if you plan to use the testing utils:

```
<dependency>
  <groupId>com.github.scenic-routing</groupId>
  <artifactId>javasdk</artifactId>
  <version>1.0.0</version>
  <type>test-jar</type>
  <scope>test</scope>
</dependency>
```

## Use

To interact with the Neo4j graph DB using the `GraphDB` wrapper, and retrieve associated data:

```
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import me.callsen.taylor.scenicrouting.javasdk.data.GraphDb;
import me.callsen.taylor.scenicrouting.javasdk.RoutingConstants;

// create the embedded Neo4j db
GraphDb graphDb = new GraphDb(graphDbFilePath);

// execute a transaction
Transaction tx = graphDb.getTransaction();
Result result = tx.execute("MATCH ()-[r]-() WHERE r.start_osm_id=65312481 AND r.end_osm_id=65320204 return DISTINCT(r)");
while ( result.hasNext() ) {
  Map<String, Object> row = result.next();
  Relationship rel = (Relationship)row.get("r");
  
  // associatedData (pointer array containing names of properties where associated data is actually stored)
  String[] associatedData = (String[]) rel.getProperty(RoutingConstants.GRAPH_ASSOCIATED_DATA_PROPERTY);
  // example value: ["ad_elevation"]

  // associated data values - always read as a org.json.JSONArray
  JSONArray elevationDataArray = new JSONArray( (String) rel.getProperty("ad_elevation") );

  // read single associated data object
  JSONObject elevationData = elevationDataArray.getJSONObject(0);
  double elevationStart = elevationData.getDouble("start");
  // example value: 49.2573d;
}
tx.close();

// close the graph db
graphDb.shutdown();
```

## Build

Checkout the repo locally and execute the following command to build the `jar` file:

```
mvn clean install
```

## Test

This module's tests can be executed with:

```
mvn test
```
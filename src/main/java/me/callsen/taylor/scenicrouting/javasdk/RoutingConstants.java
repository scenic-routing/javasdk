package me.callsen.taylor.scenicrouting.javasdk;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;

public class RoutingConstants {
  
  // graph constants
  public enum NodeLabels implements Label { INTERSECTION; } 
  public enum RelationshipTypes implements RelationshipType { CONNECTS; }
  public static final String GRAPH_ASSOCIATED_DATA_PROPERTY = "associatedData";
  public static final String GRAPH_GEOM_PROPERTY = "geom";
  public static final int GRAPH_RELATIONSHIP_PAGINATION_AMOUNT = 5000;

}

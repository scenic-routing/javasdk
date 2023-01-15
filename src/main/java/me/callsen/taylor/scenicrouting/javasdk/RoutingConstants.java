package me.callsen.taylor.scenicrouting.javasdk;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;

public class RoutingConstants {
  
  // graph constants
  public enum NodeLabels implements Label { INTERSECTION; } 
  public enum RelationshipTypes implements RelationshipType { CONNECTS; }

  public static final String GRAPH_INDEX_NAME_WAY_GEOM_POINT = "way_geom_point_idx";
  public static final String GRAPH_INDEX_NAME_INTERSECTION_GEOM_POINT = "intersection_geom_point_idx";
  public static final String GRAPH_INDEX_NAME_INTERSECTION_OSM_ID = "INTERSECTION(osm_id)";

  public static final int GRAPH_RELATIONSHIP_PAGINATION_AMOUNT = 5000;

  public static final String GRAPH_PROPERTY_NAME_ASSOCIATED_DATA = "associatedData";
  public static final String GRAPH_PROPERTY_NAME_GEOM = "geom";
  public static final String GRAPH_PROPERTY_NAME_OSM_ID = "osm_id";

}

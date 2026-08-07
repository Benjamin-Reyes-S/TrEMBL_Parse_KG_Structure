package org.example.biodwh2starter.integration;
import java.util.*;
import org.neo4j.driver.Driver;import org.neo4j.driver.Record;import org.neo4j.driver.Session;
/** Retrieves identifier-to-Neo4j-id maps for existing concept nodes. */
public final class ConceptNodeIndex {
 private static final String QUERY="MATCH (c:`%s`) UNWIND coalesce(c.ids,[]) AS identifier RETURN toString(identifier) AS identifier,id(c) AS neo4jId ORDER BY neo4jId,identifier";
 private final Driver driver; public ConceptNodeIndex(Driver d){driver=d;}
 public LinkedHashMap<String,Long> load(EntityType type){LinkedHashMap<String,Long> map=new LinkedHashMap<>();try(Session s=driver.session()){for(Record r:s.run(String.format(QUERY,type.conceptLabel())).list())map.putIfAbsent(r.get("identifier").asString(),r.get("neo4jId").asLong());}return map;}
 public Map<EntityType,LinkedHashMap<String,Long>> loadAll(){Map<EntityType,LinkedHashMap<String,Long>> maps=new LinkedHashMap<>();for(EntityType t:EntityType.values())maps.put(t,load(t));return maps;}
}

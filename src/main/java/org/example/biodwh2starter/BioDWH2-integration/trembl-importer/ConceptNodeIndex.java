package org.example.biodwh2starter.integration;
import java.util.*;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
/** Retrieves identifier-to-Neo4j-id maps for existing concept nodes. */
public final class ConceptNodeIndex {
    private static final String QUERY="MATCH (c:`%s`) UNWIND coalesce(c.ids,[]) AS identifier RETURN toString(identifier) AS identifier, id(c) AS neo4jId ORDER BY neo4jId,identifier";
    private final Driver driver; 
    public ConceptNodeIndex(Driver d){driver=d;}
    //method to generate a hashmap<identifier, neo4j id> for indexing each concept node (PROTEIN , TAXON) from neo4j
    public LinkedHashMap<String,Long> load(EntityType type){LinkedHashMap<String,Long> map=new LinkedHashMap<>();
        try(Session s=driver.session()){
            // #Implemented for 1 run{concept-index-streaming}
            Result result=s.run(String.format(QUERY,type.conceptLabel()));
            while(result.hasNext()){Record r=result.next();
                map.putIfAbsent(r.get("identifier").asString(),r.get("neo4jId").asLong());}}
            return map;}
            
    // iterative method to run load() over different EntityType (PROTEIN, TAXON) and return a map of maps
    public Map<EntityType,LinkedHashMap<String,Long>> loadAll(){Map<EntityType,LinkedHashMap<String,Long>> maps=new LinkedHashMap<>();
        for(EntityType t:EntityType.values())maps.put(t,load(t));
        return maps;}
}
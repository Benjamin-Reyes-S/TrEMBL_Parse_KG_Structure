package org.example.biodwh2starter.integration;
import static org.neo4j.driver.Values.parameters;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.apache.commons.csv.*;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
/** Streams entity rows and writes bounded transactional batches. */
public final class CsvEntityProcessor {

 private final Driver driver;
 private final int batchSize;
 public CsvEntityProcessor(Driver d,int size){if(size<1)throw new IllegalArgumentException("batch size must be positive");
    driver=d;
    batchSize=size;}

 // process(csv file path, EntityType object, Map<EntityType Object, LinkedHashMap<accession, neo4jid>, prefix for conceptIdentifier)
 public long process(Path file,EntityType type,LinkedHashMap<String,Long> concepts,String prefix)throws IOException{
  if(!Files.isRegularFile(file))throw new IOException("Missing CSV: "+file);
  long count=0;
  
  List<Map<String,Object>> batch=new ArrayList<>(batchSize);
  try(Reader in=Files.newBufferedReader(file,StandardCharsets.UTF_8);
    CSVParser csv=CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(in)){
        //adapt the 
        for(String column:type.propertyColumns())
            if(!csv.getHeaderMap().containsKey(column))throw new IOException("Missing column "+column+" in "+file);
        for(CSVRecord record:csv){String id=record.get(type.identifierColumn()).trim();
            if(id.isEmpty())continue;
            Map<String,Object> props=new HashMap<>();
                for(String column:type.propertyColumns()){String v=record.get(column);
                    if(!v.isEmpty())props.put(column,v);}Map<String,Object> row=new HashMap<>();
                        row.put("identifier",id);row.put("properties",props);
                        String name=record.get(type.nameColumn());
                        row.put("name",name.isEmpty()?null:name);
                        String conceptIdentifier=prefix+id;
                        row.put("conceptIdentifier",conceptIdentifier);
                        if(concepts.containsKey(conceptIdentifier))row.put("conceptId",concepts.get(conceptIdentifier));
                            batch.add(row);
                            if(batch.size()==batchSize){write(type,batch,concepts);
                                count+=batch.size();
                                batch.clear();
                            if(count%100000==0)System.out.printf("Processed %,d %s rows%n",count,type);}}
            if(!batch.isEmpty()){write(type,batch,concepts);
                count+=batch.size();}
  }System.out.printf("Imported %,d %s rows%n",count,type);
  return count;
 }
 private void write(EntityType type,List<Map<String,Object>> rows,LinkedHashMap<String,Long> concepts){
    List<Map<String,Object>> oldRows=new ArrayList<>();
    Map<String,Map<String,Object>> uniqueNewRows=new LinkedHashMap<>();
    for(Map<String,Object> row:rows){   
        if(row.containsKey("conceptId"))oldRows.add(row);
        else uniqueNewRows.putIfAbsent((String)row.get("conceptIdentifier"),row);}
        List<Map<String,Object>> newRows=new ArrayList<>(uniqueNewRows.values());
        try(Session s=driver.session()){
            s.writeTransaction(tx->{
                if(!oldRows.isEmpty())tx.run(existing(type),parameters("rows",oldRows)).consume();
                if(!newRows.isEmpty())for(Record r:tx.run(missing(type),parameters("rows",newRows)).list())concepts.put(r.get("identifier").asString(),r.get("conceptId").asLong());
                    return null;});
            }}
 private static String existing(EntityType t){return "UNWIND $rows AS row MERGE (s:`"+t.sourceLabel()+"` {`"+t.identifierColumn()+"`:row.identifier}) SET s += row.properties WITH row, s MATCH (c:`"+t.conceptLabel()+"`) WHERE id(c)=row.conceptId MERGE (s)-[:MAPPED_TO]->(c)";}
 private static String missing(EntityType t){return "UNWIND $rows AS row MERGE (s:`"+t.sourceLabel()+"` {`"+t.identifierColumn()+"`:row.identifier}) SET s += row.properties WITH row, s CREATE (c:`"+t.conceptLabel()+"` {ids:[row.conceptIdentifier],names:CASE WHEN row.name IS NULL THEN [] ELSE [row.name] END,__mapped:true}) MERGE (s)-[:MAPPED_TO]->(c) RETURN row.conceptIdentifier AS identifier,id(c) AS conceptId";}
}

package org.example.biodwh2starter.integration;
import static org.neo4j.driver.Values.parameters;
import java.io.*;import java.nio.charset.StandardCharsets;import java.nio.file.*;import java.util.*;import org.apache.commons.csv.*;import org.neo4j.driver.*;
/** Streams protein-to-organism edges without retaining the file in memory. */
public final class CsvRelationshipProcessor {
 private static final String QUERY="UNWIND $rows AS row MATCH (p:TrEMBL_Protein {accession:row.protein}) MATCH (o:TrEMBL_Organism {taxonomy_id:row.organism}) MERGE (p)-[:BELONGS_TO]->(o)";private final Driver driver;private final int size;
 public CsvRelationshipProcessor(Driver d,int s){driver=d;size=s;}
 public long processProteinOrganisms(Path file)throws IOException{long count=0;List<Map<String,Object>> batch=new ArrayList<>(size);try(Reader in=Files.newBufferedReader(file,StandardCharsets.UTF_8);CSVParser csv=CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(in)){for(CSVRecord r:csv){Map<String,Object> row=new HashMap<>();row.put("protein",r.get("protein_accession"));row.put("organism",r.get("organism_taxonomy_id"));batch.add(row);if(batch.size()==size){write(batch);count+=batch.size();batch.clear();}}if(!batch.isEmpty()){write(batch);count+=batch.size();}}System.out.printf("Imported %,d protein-organism edges%n",count);return count;}
 private void write(List<Map<String,Object>> rows){try(Session s=driver.session()){s.writeTransaction(tx->{tx.run(QUERY,parameters("rows",rows)).consume();return null;});}}
}

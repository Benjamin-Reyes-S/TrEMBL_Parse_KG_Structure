package org.example.biodwh2starter.integration;
import java.nio.file.*;
import java.util.*;
import org.neo4j.driver.*;
/** Opens the BioDWH2 Bolt endpoint and imports TrEMBL nodes and edges. */
public final class TremblImporter {
 private TremblImporter(){}

 public static void main(String[] args)throws Exception{String uri=env("NEO4J_URI","bolt://localhost:7689"),user=env("NEO4J_USER",""),password=env("NEO4J_PASSWORD","");

    Path input=Paths.get(env("CSV_INPUT_DIRECTORY",args.length==0?"output-csv":args[0])); 
    int size=Integer.parseInt(env("IMPORT_BATCH_SIZE","1000"));
    if(!Files.isDirectory(input))throw new IllegalArgumentException("Missing CSV directory: "+input);
        try(Driver driver=user.isEmpty()?GraphDatabase.driver(uri,AuthTokens.none()):GraphDatabase.driver(uri,AuthTokens.basic(user,password))){driver.verifyConnectivity();
            run(driver,input,size);}}


 static void run(Driver driver,Path input,int size)throws Exception{Map<EntityType,LinkedHashMap<String,String>> maps=new ConceptNodeIndex(driver).loadAll();
    // the concept node indexing should be made in the list of the BioDWH2 nodes, not from csv
    for(EntityType type:EntityType.values())System.out.printf("Found %,d existing %s concept identifiers%n",maps.get(type).size(),type);
        // process entries (nodes)
        CsvEntityProcessor processor=new CsvEntityProcessor(driver,size);
        processor.process(EntityType.ORGANISM.fileIn(input),EntityType.ORGANISM,maps.get(EntityType.ORGANISM));
        processor.process(EntityType.PROTEIN.fileIn(input),EntityType.PROTEIN,maps.get(EntityType.PROTEIN));
        //process relationships (edges)
        new CsvRelationshipProcessor(driver,size).processProteinOrganisms(input.resolve("protein_organism_mapping.csv"));}

 private static String env(String key,String fallback){String value=System.getenv(key);return value==null||value.trim().isEmpty()?fallback:value.trim();}
}

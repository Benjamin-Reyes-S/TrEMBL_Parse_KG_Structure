package org.example.biodwh2starter.integration;
import java.nio.file.Path;
import java.util.*;
public enum EntityType {
 PROTEIN("proteins.csv","TrEMBL_Protein","PROTEIN","accession","name",Arrays.asList("accession","name","sequence")),
 ORGANISM("organisms.csv","TrEMBL_Organism","ORGANISM","taxonomy_id","scientific_name",Arrays.asList("taxonomy_id","scientific_name","common_name"));
 private final String file,source,concept,id,name; private final List<String> properties;
 EntityType(String f,String s,String c,String i,String n,List<String> p){file=f;source=s;concept=c;id=i;name=n;properties=p;}
 public Path fileIn(Path p){return p.resolve(file);} public String sourceLabel(){return source;}
 public String conceptLabel(){return concept;} public String identifierColumn(){return id;}
 public String nameColumn(){return name;} public List<String> propertyColumns(){return properties;}
}

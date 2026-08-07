FROM maven:3.9.9-eclipse-temurin-11 AS build
WORKDIR /build
COPY pom.xml ./
COPY src ./src
RUN mvn -q -DskipTests package
RUN mvn -q dependency:copy-dependencies -DoutputDirectory=target/lib
FROM eclipse-temurin:11-jre-jammy
WORKDIR /app
COPY --from=build /build/target/*-SNAPSHOT.jar /app/trembl-importer.jar
COPY --from=build /build/target/lib /app/lib
ENTRYPOINT ["java","-cp","/app/trembl-importer.jar:/app/lib/*","org.example.biodwh2starter.integration.TremblImporter"]

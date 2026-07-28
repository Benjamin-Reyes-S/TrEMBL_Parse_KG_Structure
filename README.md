# BioDWH2-inspired Maven Starter

A small, standalone Java project that follows the main development conventions visible in BioDWH2:

- Maven project structure
- Java 11 compilation target
- UTF-8 source encoding
- JUnit Jupiter 5.10.1
- conventional `src/main/java` and `src/test/java` directories
- package-oriented Java classes
- GitHub Actions Maven build

This starter is intentionally single-module. BioDWH2 itself is a large multi-module Maven reactor, but a single module is easier for beginning a new application.

## Requirements

- JDK 11 or newer
- Internet access on the first Maven-wrapper run, so Maven and dependencies can be downloaded

You do not need to install Maven globally. The included wrapper scripts use an installed `mvn` when available and otherwise download Maven 3.9.9.

## Build and test

Linux/macOS:

```bash
./mvnw clean verify
```

Windows:

```bat
mvnw.cmd clean verify
```

The compiled classes are written to `target/classes`, tests to `target/test-classes`, and the executable JAR to:

```text
target/biodwh2-maven-starter-0.1.0-SNAPSHOT.jar
```

## Run

Run through Maven:

```bash
./mvnw exec:java
```

Or build and run the JAR:

```bash
./mvnw clean package
java -jar target/biodwh2-maven-starter-0.1.0-SNAPSHOT.jar
```

Expected output begins with:

```text
BioDWH2-inspired starter is running.
```

## Project layout

```text
biodwh2-maven-starter/
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .mvn/wrapper/maven-wrapper.properties
├── .github/workflows/maven.yml
├── src/main/java/org/example/biodwh2starter/
│   ├── Application.java
│   ├── model/BioEntity.java
│   └── service/BioEntityService.java
└── src/test/java/org/example/biodwh2starter/service/
    └── BioEntityServiceTest.java
```

## Start writing your own classes

Add a class under the matching package directory. For example:

```text
src/main/java/org/example/biodwh2starter/parser/MyParser.java
```

Start the file with:

```java
package org.example.biodwh2starter.parser;

public final class MyParser {
    public void parse() {
        // Add your implementation here.
    }
}
```

Run `./mvnw test` after adding or changing classes.

## Rename the project

Change these values in `pom.xml`:

- `groupId`: your organization or reversed domain, such as `com.mycompany`
- `artifactId`: your project name
- `version`: your project version

Then move the Java files into a directory matching your package and update each `package` declaration and the JAR plugin's `mainClass`.

## Optional: convert to a BioDWH2-style multi-module build

When the application grows, change the root project packaging to `pom`, add a `<modules>` section, and give each child module its own `pom.xml`. BioDWH2 uses this pattern for its core, main application, storage, and data-source modules.

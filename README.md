# Java service utilities
This library provides some utilities for [Java Service Providers](https://www.baeldung.com/java-spi).

## @ServiceProvider
The `@ServiceProvider` annotation deals with the tedious work of registring service providers.

Current features:
- generates classpath files in `META-INF` folder
- supports multiple registration of one class
- plays nice with [Java 9 modularity](https://www.baeldung.com/java-9-modularity)
- checks coherence between classpath and modulepath if `module-info.java` is available

Current limitations:
- detects modulepath static method `provider` but doesn't generate a workaround for classpath

## How to use

Java example:
```java
public interface HelloService {}

public interface SomeService {}

@ServiceProvider(HelloService.class)
public class SimpleProvider implements HelloService {}

@ServiceProvider(HelloService.class)
@ServiceProvider(SomeService.class)
public class MultiProvider implements HelloService, SomeService {}
```

Maven pom example:
```xml
<dependencies>
  <dependency>
    <groupId>be.nbb.rd</groupId>
    <artifactId>java-service-annotation</artifactId>
    <version>LATEST_VERSION</version>
    <scope>provided</scope>
  </dependency>
</dependencies>

<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
      <configuration>
        <annotationProcessorPaths>
          <path>
            <groupId>be.nbb.rd</groupId>
            <artifactId>java-service-processor</artifactId>
            <version>LATEST_VERSION</version>
          </path>
        </annotationProcessorPaths>
      </configuration>
    </plugin>
  </plugins>
</build>

<repositories>
  <repository>
    <id>oss-jfrog-artifactory-releases</id>
    <url>https://oss.jfrog.org/artifactory/oss-release-local</url>
    <snapshots>
      <enabled>false</enabled>
    </snapshots>
  </repository>
</repositories>
```

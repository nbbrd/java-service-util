# Java service utilities

[![Download](https://img.shields.io/github/release/nbbrd/java-service-util.svg)](https://github.com/nbbrd/java-service-util/releases/latest)

This library provides some utilities for [Java Service Providers](https://www.baeldung.com/java-spi).

Key points:
- ligthweight library with no dependency
- Java 8 minimum requirement
- all the work is done at compile time
- has an automatic module name that makes it compatible with [JPMS](https://www.baeldung.com/java-9-modularity) 

## @ServiceProvider
The `@ServiceProvider` annotation deals with the tedious work of registring service providers.

Current features:
- generates classpath files in `META-INF/services` folder
- supports multiple registration of one class
- can infer the service if the provider implements/extends exactly one interface/class
- checks coherence between classpath and modulepath if `module-info.java` is available

Current limitations:
- detects modulepath `public static provider()` method but doesn't generate a workaround for classpath

Example:
```java
public interface HelloService {}

public interface SomeService {}

@ServiceProvider
public class SimpleProvider implements HelloService {}

@ServiceProvider(HelloService.class)
@ServiceProvider(SomeService.class)
public class MultiProvider implements HelloService, SomeService {}
```

## @ServiceDefinition
The `@ServiceDefinition` annotation generates a specialized service loader that takes care of the loading and enforces a specific usage.

Current features:
- generates a specialized service loader
- checks coherence of service use in modules if `module-info.java` is available

Example:
```java
public interface Logger {
    void info(String message);
}

public final class LoggerFactory {
    private LoggerFactory() {}
  
    public static Logger getLogger(Class<?> type) {
      return LoggerSpiLoader.get().makeNewLoggerInstance(type.getName());
    }
}

@ServiceDefinition(
    singleton = true,
    quantifier = Quantifier.SINGLE,
    fallback = NoOpLoggerSpi.class
)
public interface LoggerSpi {
    Logger makeNewLoggerInstance(String name);
}
```

## Setup

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
Alternate setup if the IDE doesn't detect the processor:
```xml
<dependencies>
  <dependency>
    <groupId>be.nbb.rd</groupId>
    <artifactId>java-service-processor</artifactId>
    <version>LATEST_VERSION</version>
    <scope>provided</scope>
  </dependency>
</dependencies>
```

## Related work

- [NetBeans lookup](https://search.maven.org/search?q=g:org.netbeans.api%20AND%20a:org-openide-util-lookup&core=gav)
- [Google AutoServive](https://www.baeldung.com/google-autoservice)

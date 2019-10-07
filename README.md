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
public class InferredProvider implements HelloService {}

@ServiceProvider(HelloService.class)
@ServiceProvider(SomeService.class)
public class MultiProvider implements HelloService, SomeService {}
```

## @ServiceDefinition
The `@ServiceDefinition` annotation generates a specialized service loader that takes care of the loading and enforces a specific usage.

Current features:
- generates a **specialized service loader** with the following parameters:
  - `quantifier`: optional, single or multiple service instances
  - `preprocessor`: filter/map/sort operations 
  - `mutability`: none, basic or concurrent access
  - `singleton`: global or local scope
- **checks coherence** of service use **in modules** if `module-info.java` is available

Examples can be found in the [examples project](https://github.com/nbbrd/java-service-util/tree/develop/java-service-examples/src/main/java/nbbrd/service/examples).

### Quantifier

OPTIONAL example:
```java
@ServiceDefinition(quantifier = Quantifier.OPTIONAL)
public interface WinRegistry { 
  String readString(int hkey, String key, String valueName);
  static int HKEY_LOCAL_MACHINE = 0;
}

Optional<WinRegistry> optional = new WinRegistryLoader().get();
optional.ifPresent(reg -> System.out.println(reg.readString(HKEY_LOCAL_MACHINE, "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion", "ProductName")));
```

SINGLE example:
```java
@ServiceDefinition(quantifier = Quantifier.SINGLE, fallback = FallbackLogger.class)
public interface LoggerFinder {
  Consumer<String> getLogger(String name);
}

public class FallbackLogger implements LoggerFinder {
  @Override
  public Consumer<String> getLogger(String name) {
    return message -> System.out.println(String.format("[%s] %s", name, message));
  }
}

LoggerFinder single = new LoggerFinderLoader().get();
single.getLogger("MyClass").accept("some message");
```

MULTIPLE example:
```java
@ServiceDefinition(quantifier = Quantifier.MULTIPLE)
public interface Translator {
  String translate(String text);
}

List<Translator> multiple = new TranslatorLoader().get();
multiple.forEach(translator -> System.out.println(translator.translate("hello")));
```

### Preprocessor

Filter/sort example:
```java
@ServiceDefinition(preprocessor = ByAvailabilityAndCost.class)
public interface FileSearch {
  List<File> searchByName(String name);
  boolean isAvailable();
  int getCost();
}

public class ByAvailabilityAndCost implements UnaryOperator<Stream<FileSearch>> {
  @Override
  public Stream<FileSearch> apply(Stream<FileSearch> stream) {
    return stream
            .filter(FileSearch::isAvailable)
            .sorted(Comparator.comparingInt(FileSearch::getCost));
  }
}

new FileSearchLoader().get().ifPresent(search -> search.searchByName(".xlsx").forEach(System.out::println));
```

### Mutability

BASIC example:
```java
@ServiceDefinition(mutability = Mutability.BASIC)
public interface Messenger {
  void send(String message);
}

MessengerLoader loader = new MessengerLoader();
loader.get().ifPresent(o -> o.send("First"));

loader.set(Optional.of(msg -> System.out.println(msg)));
loader.get().ifPresent(o -> o.send("Second"));

loader.set(Optional.of(msg -> JOptionPane.showMessageDialog(null, msg)));
loader.get().ifPresent(o -> o.send("Third"));

loader.reload();
loader.get().ifPresent(o -> o.send("Fourth"));
```

### Singleton

Local example:
```java
@ServiceDefinition(singleton = false)
public interface StatefulAlgorithm {
  double compute(double... values);
}

StatefulAlgorithm algo1 = new StatefulAlgorithmLoader().get().orElseThrow(RuntimeException::new);
StatefulAlgorithm algo2 = new StatefulAlgorithmLoader().get().orElseThrow(RuntimeException::new);

Stream.of(algo1, algo2)
      .parallel()
      .forEach(algo -> System.out.println(algo.compute(1, 2, 3)));
```

Global example:
```java
@ServiceDefinition(singleton = true)
public interface SystemSettings {
  String getDeviceName();
}

SystemSettingsLoader.get().ifPresent(sys -> System.out.println(sys.getDeviceName()));
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

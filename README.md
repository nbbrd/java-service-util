# Java service utilities

[![Download](https://img.shields.io/github/release/nbbrd/java-service-util.svg)](https://github.com/nbbrd/java-service-util/releases/latest)

This library provides some utilities for [Java Service Providers](https://www.baeldung.com/java-spi).

Key points:
- ligthweight library with no dependency
- Java 8 minimum requirement
- all the work is done at compile time
- has an automatic module name that makes it compatible with [JPMS](https://www.baeldung.com/java-9-modularity) 

## @ServiceProvider
The `@ServiceProvider` annotation **registers service providers** on classpath and modulepath.

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
The `@ServiceDefinition` annotation **generates a specialized service loader** that enforces a specific usage.  
It generates boilerplate code, thus reducing bugs and improving code coherence.

Current features:
- generates a specialized service loader with the following properties:
  - `quantifier`: optional, single or multiple service instances
  - `preprocessor`: filter/map/sort operations 
  - `mutability`: none, basic or concurrent access
  - `singleton`: global or local scope
- checks coherence of service use in modules if `module-info.java` is available

Current limitations:
- does not support service type inspection before instantiation
- does not support lazy instantiation

Examples can be found in the [examples project](https://github.com/nbbrd/java-service-util/tree/develop/java-service-examples/src/main/java/nbbrd/service/examples).

### Quantifier property

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

### Preprocessor property

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

### Mutability property

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

### Singleton property

Local example:
```java
@ServiceDefinition(singleton = false)
public interface StatefulAlgorithm {
  void init(SecureRandom random);
  double compute(double... values);
}

StatefulAlgorithm algo1 = new StatefulAlgorithmLoader().get().orElseThrow(RuntimeException::new);
algo1.init(SecureRandom.getInstance("NativePRNG"));

StatefulAlgorithm algo2 = new StatefulAlgorithmLoader().get().orElseThrow(RuntimeException::new);
algo2.init(SecureRandom.getInstance("PKCS11"));

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

### SPI pattern

In some cases, it is better to clearly separate API from SPI. Here is an example on how to do it:

```java
public final class FileType {

  private FileType() {
  }

  private static final List<FileTypeSpi> PROBES = new internal.FileTypeSpiLoader().get();

  public static Optional<String> probeContentType(Path file) throws IOException {
    for (FileTypeSpi probe : PROBES) {
      String result;
      if ((result = probe.getContentTypeOrNull(file)) != null) {
        return Optional.of(result);
      }
    }
    return Optional.empty();
  }
}

@ServiceDefinition(
  quantifier = Quantifier.MULTIPLE,
  preprocessor = FileTypeSpi.ProbePreprocessor.class,
  loaderName = "internal.FileTypeSpiLoader")
public interface FileTypeSpi {

  enum Accuracy { HIGH, LOW }

  String getContentTypeOrNull(Path file) throws IOException;

  Accuracy getAccuracy();

  static final class ProbePreprocessor implements UnaryOperator<Stream<FileTypeSpi>> {
    @Override
    public Stream<FileTypeSpi> apply(Stream<FileTypeSpi> probes) {
        return probes.sorted(Comparator.comparing(FileTypeSpi::getAccuracy));
    }
  }
}

String[] files = {"hello.csv", "stuff.txt"};
for (String file : files) {
  System.out.println(file + ": " + FileType.probeContentType(Paths.get(file)).orElse("?"));
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

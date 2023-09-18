# Java service utilities

[![Download](https://img.shields.io/github/release/nbbrd/java-service-util.svg)](https://github.com/nbbrd/java-service-util/releases/latest)

This library provides some **utilities for Java SPI** ([Service Provider Interface](https://www.baeldung.com/java-spi)).

The Java SPI is a mechanism that decouples a service from its implementation(s).
It allows the creation of extensible or replaceable modules/plugins.
It is composed of four main components: service, service provider interface, service provider and service loader.
If the service is a single interface then it is the same as a service provider interface.

**Key points:**
- lightweight library with no dependency
- no dependency at runtime, all the work is done at compile-time
- Java 8 minimum requirement
- has an automatic module name that makes it compatible with [JPMS](https://www.baeldung.com/java-9-modularity) 

## @ServiceProvider
The `@ServiceProvider` annotation **registers service providers** on classpath and modulepath.

**Features:**
- generates classpath files in `META-INF/services` folder
- supports multiple registration of one class
- can infer the service if the provider implements/extends exactly one interface/class
- checks coherence between classpath and modulepath if `module-info.java` is available

**Limitations:**
- detects modulepath `public static provider()` method but doesn't generate a [workaround for classpath](https://github.com/nbbrd/java-service-util/issues/12)

Example:
```java
public interface FooSPI {}

public interface BarSPI {}

@ServiceProvider
public class FooProvider implements FooSPI {}

@ServiceProvider ( FooSPI.class )
@ServiceProvider ( BarSPI.class )
public class FooBarProvider implements FooSPI, BarSPI {}
```

## @ServiceDefinition
The `@ServiceDefinition` annotation **defines a service usage and generates a specialized service loader** that enforces that specific usage.  

**Features:**
- generates boilerplate code, thus reducing bugs and improving code coherence
- improves documentation by declaring services explicitly and generating javadoc
- checks coherence of service use in modules if `module-info.java` is available
- allows use of [custom service loader](#custom-service-loader)
- allows [batch loading](#batch-loading) of providers

**Limitations:**
- does not support [type inspection before instantiation](https://github.com/nbbrd/java-service-util/issues/13)
- does not support [lazy instantiation](https://github.com/nbbrd/java-service-util/issues/6)

The loading behavior is defined by the following properties:
- [`quantifier`](#quantifier): optional, single or multiple service instances
- [`preprocessing`](#preprocessing): filter/map/sort operations
- [`mutability`](#mutability): none, basic or concurrent access
- [`singleton`](#singleton): global or local scope

Examples can be found in the [examples project](https://github.com/nbbrd/java-service-util/tree/develop/java-service-examples/src/main/java/nbbrd/service/examples).

### Quantifier

OPTIONAL: when a service is not guaranteed to be available such as OS-specific API
```java
@ServiceDefinition ( quantifier = Quantifier.OPTIONAL )
public interface WinRegistry { 
    String readString(int hkey, String key, String valueName);
    int HKEY_LOCAL_MACHINE = 0;
}

Optional<WinRegistry> optional = WinRegistryLoader.load();
optional.ifPresent(reg -> System.out.println(reg.readString(HKEY_LOCAL_MACHINE, "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion", "ProductName")));
```

SINGLE: when exactly one service is guaranteed to be available
```java
@ServiceDefinition ( quantifier = Quantifier.SINGLE, fallback = FallbackLogger.class )
public interface LoggerFinder {
    Consumer<String> getLogger(String name);
}

public class FallbackLogger implements LoggerFinder {
    @Override
    public Consumer<String> getLogger(String name) {
        return message -> System.out.println(String.format("[%s] %s", name, message));
    }
}

LoggerFinder single = LoggerFinderLoader.load();
single.getLogger("MyClass").accept("some message");
```

MULTIPLE: when several instances of a service could be used at the same time
```java
@ServiceDefinition ( quantifier = Quantifier.MULTIPLE )
public interface Translator {
    String translate(String text);
}

List<Translator> multiple = TranslatorLoader.load();
multiple.forEach(translator -> System.out.println(translator.translate("hello")));
```

### Preprocessing

Preprocessing applies map/filter/sort to services instances during the loading.  
It can be specified by using one of these two solutions:
- Basic solution: 
  - Map: `@ServiceDefinition#wrapper` property
  - Filter: `@ServiceFilter` annotation
  - Sort: `@ServiceSorter` annotation 
- Advanced solution: 
  - Preprocessor: `@ServiceDefinition#preprocessor` property

Map/Filter/sort example:
```java
@ServiceDefinition ( wrapper = FailSafeSearch.class )
public interface FileSearch {

    List<File> searchByName(String name);

    @ServiceFilter
    boolean isAvailable();

    @ServiceSorter
    int getCost();
}

class FailSafeSearch implements FileSearch {
  
    public FailSafeSearch(FileSearch delegate) { ... }

    @Override
    public List<File> searchByName(String name) {
        try {
            return delegate.searchByName(name);
        } catch (RuntimeException unexpected) {
            // log unexpected ...
            return Collections.emptyList();
        }
    }
}

FileSearchLoader.load().ifPresent(search -> search.searchByName(".xlsx").forEach(System.out::println));
```

### Mutability

BASIC example:
```java
@ServiceDefinition ( mutability = Mutability.BASIC )
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
@ServiceDefinition ( singleton = false )
public interface StatefulAlgorithm {
    void init(SecureRandom random);
    double compute(double... values);
}

StatefulAlgorithm algo1 = StatefulAlgorithmLoader.load().orElseThrow(RuntimeException::new);
algo1.init(SecureRandom.getInstance("NativePRNG"));

StatefulAlgorithm algo2 = StatefulAlgorithmLoader.load().orElseThrow(RuntimeException::new);
algo2.init(SecureRandom.getInstance("PKCS11"));

Stream.of(algo1, algo2)
      .parallel()
      .forEach(algo -> System.out.println(algo.compute(1, 2, 3)));
```

Global example:
```java
@ServiceDefinition ( singleton = true )
public interface SystemSettings {
    String getDeviceName();
}

SystemSettingsLoader.get().ifPresent(sys -> System.out.println(sys.getDeviceName()));
```

### Custom service loader

It is possible to use a custom service loader such as [NetBeans Lookup](https://search.maven.org/search?q=g:org.netbeans.api%20AND%20a:org-openide-util-lookup&core=gav) instead of JDK `ServiceLoader`.

Example:
```java
@ServiceDefinition ( backend = NetBeansLookup.class, cleaner = NetBeansLookup.class )
public interface IconProvider {
    Icon getIconOrNull(CommonIcons icon);
}

public enum NetBeansLookup implements Function<Class, Iterable>, Consumer<Iterable> {

    INSTANCE;

    @Override
    public Iterable apply(Class type) { return new NetBeansLookupResult(type); }

    @Override
    public void accept(Iterable iterable) { ((NetBeansLookupResult) iterable).reload(); }

    private static final class NetBeansLookupResult implements Iterable {

        private final Lookup.Result result;
        private Collection instances;

        private NetBeansLookupResult(Class type) {
            this.result = Lookup.getDefault().lookupResult(type);
            this.instances = result.allInstances();
        }

        @Override
        public Iterator iterator() { return instances.iterator(); }

        public void reload() { this.instances = result.allInstances(); }
    }
}
```

### Batch loading

Batch loading loads several providers at once. It can be used to **create a bridge between two services** or to **generate providers on the fly**.
It is enabled by the `batch` property. 

Example:
```java
@ServiceDefinition ( batch = true, quantifier = Quantifier.MULTIPLE )
public interface SwingColorScheme {
    List<Color> getColors();
}

@ServiceDefinition ( quantifier = Quantifier.MULTIPLE )
public interface RgbColorScheme {
    List<Integer> getColors();
}

@ServiceProvider
public class RgbToSwingProvider implements SwingColorSchemeBatch {

    @Override
    public Stream<SwingColorScheme> getProviders() {
        return RgbColorSchemeLoader.load().stream().map(this::convert);
    }

    private SwingColorScheme convert(RgbColorScheme colorScheme) {
        return () -> colorScheme.getColors().stream().map(Color::new).collect(Collectors.toList());
    }
}

@ServiceProvider
public class SwingProvider implements SwingColorScheme {
    @Override
    public List<Color> getColors() { return Collections.singletonList(Color.BLACK); }
}

@ServiceProvider
public class RgbProvider implements RgbColorScheme {
    @Override
    public List<Integer> getColors() { return Arrays.asList(-65536, -16711936, -16776961); }
}

SwingColorSchemeLoader.load().forEach(colorScheme -> System.out.println(colorScheme.getColors()));
```

### SPI pattern

In some cases, it is better to clearly separate API from SPI. Here is an example on how to do it:

```java
public final class FileType {

    private FileType() {}

    public static Optional<String> probeContentType(Path file) throws IOException {
        for (FileTypeSpi probe : internal.FileTypeSpiLoader.get()) {
            String result = probe.getContentTypeOrNull(file);
            if (result != null) {
                return Optional.of(result);
            }
        }
        return Optional.empty();
    }
}

@ServiceDefinition(
    quantifier = Quantifier.MULTIPLE,
    loaderName = "internal.FileTypeSpiLoader",
    singleton = true
)
public interface FileTypeSpi {

    enum Accuracy { HIGH, LOW }

    String getContentTypeOrNull(Path file) throws IOException;

    @ServiceSorter
    Accuracy getAccuracy();
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
    <groupId>com.github.nbbrd.java-service-util</groupId>
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
            <groupId>com.github.nbbrd.java-service-util</groupId>
            <artifactId>java-service-processor</artifactId>
            <version>LATEST_VERSION</version>
          </path>
        </annotationProcessorPaths>
      </configuration>
    </plugin>
  </plugins>
</build>

```
Alternate setup if the IDE doesn't detect the processor:
```xml
<dependencies>
  <dependency>
    <groupId>com.github.nbbrd.java-service-util</groupId>
    <artifactId>java-service-processor</artifactId>
    <version>LATEST_VERSION</version>
    <scope>provided</scope>
  </dependency>
</dependencies>
```

## Related work

- [NetBeans Lookup](https://search.maven.org/search?q=g:org.netbeans.api%20AND%20a:org-openide-util-lookup&core=gav)
- [Google AutoServive](https://www.baeldung.com/google-autoservice)
- [purejin - PURE Java INjection](http://jbee.github.io/purejin/)

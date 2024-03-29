# Java service utilities

[![Download](https://img.shields.io/github/release/nbbrd/java-service-util.svg)](https://github.com/nbbrd/java-service-util/releases/latest)

This library provides some **utilities for Java SPI** ([Service Provider Interface](https://www.baeldung.com/java-spi)).

The Java SPI is a mechanism that decouples a service from its implementation(s).
It allows the creation of extensible or replaceable modules/plugins.
It consists of four main components: a service, a service provider interface, some service providers and a service loader.
If the service is a single interface then it is the same as a service provider interface.

**Key points:**
- lightweight library with no dependency
- no dependency at runtime, all the work is done at compile-time
- Java 8 minimum requirement
- has an automatic module name that makes it compatible with [JPMS](https://www.baeldung.com/java-9-modularity) 

[ [Components](#components) | [Design](#design) | [Setup](#setup) | [Developing](#developing) | [Contributing](#contributing)  | [Licensing](#licensing) | [Related work](#related-work) | [Alternatives](#alternatives) ]

## Components

| Annotation                               | Purpose                                                    |
|------------------------------------------|------------------------------------------------------------|
| [@ServiceProvider](#serviceprovider)     | registers a service provider                               |
| [@ServiceDefinition](#servicedefinition) | defines a service usage and generates a specialized loader |
| [@ServiceId](#serviceid)                 | specifies the method used to identify a service provider   |
| [@ServiceFilter](#servicefilter)         | specifies the method used to filter a service provider     |
| [@ServiceSorter](#servicesorter)         | specifies the method used to sort a service provider       |

### @ServiceProvider

The `@ServiceProvider` annotation **registers a service provider** on classpath and modulepath.

Features:
- generates classpath files in `META-INF/services` folder
- supports multiple registration of one class
- can infer the service if the provider implements/extends exactly one interface/class
- checks coherence between classpath and modulepath if `module-info.java` is available

Limitations:
- detects modulepath `public static provider()` method but doesn't generate a [workaround for classpath](https://github.com/nbbrd/java-service-util/issues/12)

```java
public interface FooSPI {}

public interface BarSPI {}

// 💡 One provider, one service
@ServiceProvider
public class FooProvider implements FooSPI {}

// 💡 One provider, multiple services
@ServiceProvider ( FooSPI.class )
@ServiceProvider ( BarSPI.class )
public class FooBarProvider implements FooSPI, BarSPI {}
```

### @ServiceDefinition
The `@ServiceDefinition` annotation **defines a service usage and generates a specialized loader** that enforces that specific usage.  

Features:
- generates boilerplate code, thus reducing bugs and improving code coherence
- improves documentation by declaring services explicitly and generating javadoc
- checks coherence of service use in modules if `module-info.java` is available
- allows [identification](#serviceid)
- allows [filtering](#servicefilter) and [sorting](#servicesorter)
- allows [batch loading](#batch-type-property) 
- allows [custom backend](#backend-and-cleaner-properties)

Limitations:
- does not support [type inspection before instantiation](https://github.com/nbbrd/java-service-util/issues/13)

Main properties:
- [`#quantifier`](#quantifier-property): number of services expected at runtime
- [`#loaderName`](#loader-name-property): custom qualified name of the loader
- [`#fallback`](#fallback-property): fallback type for `SINGLE` quantifier
- [`#batchType`](#batch-type-property): bridge different services and generate providers on the fly

Advanced properties:
- [`#mutability`](#mutability-property): on-demand set and reload
- [`#singleton`](#singleton-property): loader scope
- [`#wrapper`](#wrapper-property): wrapper type on backend
- [`#preprocessing`](#preprocessing-property): custom operations on backend
- [`#backend` `#cleaner`](#backend-and-cleaner-properties): custom service loader

#### Quantifier property

The `#quantifier` property specifies the **number of services expected at runtime**.

Values:

- `OPTIONAL`: when a service is not guaranteed to be available such as OS-specific API
  ```java
  @ServiceDefinition(quantifier = Quantifier.OPTIONAL)
  public interface WinRegistry {
  
    String readString(int hkey, String key, String valueName);
  
    int HKEY_LOCAL_MACHINE = 0;
  
    static void main(String[] args) {
      // 💡 Service availability not guaranteed
      Optional<WinRegistry> optional = WinRegistryLoader.load();
      optional.map(reg -> reg.readString(
              HKEY_LOCAL_MACHINE,
              "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion",
              "ProductName"))
          .ifPresent(System.out::println);
    }
  }
  ```
  _Source: [nbbrd/service/examples/WinRegistry.java](java-service-examples/src/main/java/nbbrd/service/examples/WinRegistry.java)_


- `SINGLE`: when exactly one service is guaranteed to be available
  ```java
  @ServiceDefinition(quantifier = Quantifier.SINGLE, fallback = LoggerFinder.FallbackLogger.class)
  public interface LoggerFinder {
  
    Consumer<String> getLogger(String name);
  
    class FallbackLogger implements LoggerFinder {
  
      @Override
      public Consumer<String> getLogger(String name) {
        return msg -> System.out.printf(Locale.ROOT, "[%s] %s%n", name, msg);
      }
    }
  
    static void main(String[] args) {
      // 💡 Service availability guaranteed
      LoggerFinder single = LoggerFinderLoader.load();
      single.getLogger("MyClass").accept("some message");
    }
  }
  ```
  _Source: [nbbrd/service/examples/LoggerFinder.java](java-service-examples/src/main/java/nbbrd/service/examples/LoggerFinder.java)_


- `MULTIPLE`: when several instances of a service could be used at the same time
  ```java
  @ServiceDefinition(quantifier = Quantifier.MULTIPLE)
  public interface Translator {
  
    String translate(String text);
  
    static void main(String[] args) {
        // 💡 Multiple services expected
      List<Translator> multiple = TranslatorLoader.load();
      multiple.stream()
          .map(translator -> translator.translate("hello"))
          .forEach(System.out::println);
    }
  }
  ```
  _Source: [nbbrd/service/examples/Translator.java](java-service-examples/src/main/java/nbbrd/service/examples/Translator.java)_


#### Loader name property

The `#loaderName` property specifies the **custom qualified name of the loader**.

```java
// 💡 Name without interpretation
@ServiceDefinition(loaderName = "internal.FooSPILoader")
public interface FooSPI { }
```

An empty value generates an automatic name.  
A non-empty value is interpreted as a [Mustache template](https://mustache.github.io/) with the following tags:

| Tag             | Description                           |
|-----------------|---------------------------------------|
| `packageName`   | The package name of the service class |
| `simpleName`    | The service class name                |
| `canonicalName` | The full service class name           |

```java
// 💡 Name with interpretation
@ServiceDefinition(loaderName = "internal.{{simpleName}}Loader")
public interface FooSPI { }
```

#### Fallback property

The `#fallback` property specifies the **fallback class to use if no service is available**.  
This property is only used in conjunction with `Quantifier#SINGLE`.

```java
@ServiceDefinition(quantifier = Quantifier.SINGLE, fallback = NoOpFooProvider.class)
public interface FooSPI { }

// 💡 Provider that does nothing except preventing NPE
public class NoOpFooProvider implements FooSPI { }
```

_Note that a warning is raised at compile time if the fallback is missing 
but this warning can be disabled with the `@SupressWarning("SingleFallbackNotExpected")` annotation._

#### Batch type property

The `#batchType` property allows to **bridge different services** and to **generate providers on the fly**.  
Batch providers are used alongside regular providers.

```java
@ServiceDefinition(quantifier = Quantifier.MULTIPLE, batchType = SwingColorScheme.Batch.class)
public interface SwingColorScheme {

  List<Color> getColors();

  static void main(String[] args) {
    // 💡 Invisible use of RgbColorScheme
    SwingColorSchemeLoader.load()
        .stream()
        .map(SwingColorScheme::getColors)
        .forEach(System.out::println);
  }

  interface Batch {
    Stream<SwingColorScheme> getProviders();
  }
  
  // 💡 Bridge between SwingColorScheme and RgbColorScheme
  @ServiceProvider(Batch.class)
  final class RgbBridge implements Batch {

    @Override
    public Stream<SwingColorScheme> getProviders() {
      return RgbColorSchemeLoader.load()
          .stream()
          .map(RgbAdapter::new);
    }
  }

  // 💡 Regular provider
  @ServiceProvider(SwingColorScheme.class)
  final class Cyan implements SwingColorScheme {

    @Override
    public List<Color> getColors() {
      return Collections.singletonList(Color.CYAN);
    }
  }
}
```
_Source: [nbbrd/service/examples/SwingColorScheme.java](java-service-examples/src/main/java/nbbrd/service/examples/SwingColorScheme.java)_

Constraints:
1. Batch type must be an interface or an abstract class.
2. Batch method must be unique.

#### Mutability property

The `#mutability` property allows **on-demand set and reload** of a loader.

_Example: [nbbrd/service/examples/Messenger.java](java-service-examples/src/main/java/nbbrd/service/examples/Messenger.java)_

⚠️ _This is a complex mechanism that targets specific usages. It will be removed and/or simplified in a future release._

#### Singleton property

The `#singleton` property specifies the **loader scope**.

_Example: [nbbrd/service/examples/StatefulAlgorithm.java](java-service-examples/src/main/java/nbbrd/service/examples/StatefulAlgorithm.java)
and [nbbrd/service/examples/SystemSettings.java](java-service-examples/src/main/java/nbbrd/service/examples/SystemSettings.java)_

⚠️ _This is a complex mechanism that targets specific usages. It will be removed and/or simplified in a future release._

#### Wrapper property

The `#wrapper` property allows **service decoration** before any map/filter/sort operation.

_Example: `TODO`_

⚠️ _This is a complex mechanism that targets specific usages. It will be removed and/or simplified in a future release._

#### Preprocessing property

The `#preprocessor` property allows **custom operations on backend** before any map/filter/sort operation.

_Example: `TODO`_

⚠️ _This is a complex mechanism that targets specific usages. It will be removed and/or simplified in a future release._

#### Backend and cleaner properties

The `#backend` and `#cleaner` properties allow to use a **custom service loader** such as [NetBeans Lookup](https://search.maven.org/search?q=g:org.netbeans.api%20AND%20a:org-openide-util-lookup&core=gav) instead of JDK `ServiceLoader`.

_Example: [nbbrd/service/examples/IconProvider.java](java-service-examples/src/main/java/nbbrd/service/examples/IconProvider.java)_

⚠️ _This is a complex mechanism that targets specific usages. It will be removed and/or simplified in a future release._

### @ServiceId

The `@ServiceId` annotation **specifies the method used to identify a service provider**.

Properties:
- `#pattern`: specifies the regex pattern that the ID is expected to match

```java
@ServiceDefinition(quantifier = Quantifier.MULTIPLE)
public interface HashAlgorithm {

  // 💡 Enforce service naming
  @ServiceId(pattern = ServiceId.SCREAMING_KEBAB_CASE)
  String getName();

  String hashToHex(byte[] input);

  static void main(String[] args) {
    // 💡 Retrieve service by name
    HashAlgorithmLoader.load()
      .stream()
      .filter(algo -> algo.getName().equals("SHA-256"))
      .findFirst()
      .map(algo -> algo.hashToHex("hello".getBytes(UTF_8)))
      .ifPresent(System.out::println);
  }
}
```
_Source: [nbbrd/service/examples/HashAlgorithm.java](java-service-examples/src/main/java/nbbrd/service/examples/HashAlgorithm.java)_

Characteristics:
- The `#pattern` property is used as a filter.
- The `#pattern` property is available as a static field in the loader.

Constraints:
1. It only applies to methods of a service.
2. It does not apply to static methods.
3. The annotated method must have no-args.
4. The annotated method must return String.
5. The annotated method must be unique.
6. The annotated method must not throw checked exceptions.
7. Its pattern must be valid.

### @ServiceFilter

The `@ServiceFilter` annotation **specifies the method used to filter a service provider**.

Properties:
- `#position`: sets the filter ordering in case of multiple filters
- `#negate`: applies a logical negation

```java
@ServiceDefinition
public interface FileSearch {

  List<File> searchByName(String name);

  // 💡 General filter
  @ServiceFilter(position = 1)
  boolean isAvailableOnCurrentOS();

  // 💡 Specific filter
  @ServiceFilter(position = 2, negate = true)
  boolean isDisabledBySystemProperty();

  static void main(String[] args) {
    FileSearchLoader.load()
      .map(search -> search.searchByName(".xlsx"))
      .orElseGet(Collections::emptyList)
      .forEach(System.out::println);
  }
}
```
_Source: [nbbrd/service/examples/FileSearch.java](java-service-examples/src/main/java/nbbrd/service/examples/FileSearch.java)_

Characteristics:
- There is no limit to the number of annotations per service.
- Filtering is done before sorting.

Constraints:
1. It only applies to methods of a service.
2. It does not apply to static methods.
3. The annotated method must have no-args.
4. The annotated method must return boolean.
5. The annotated method must not throw checked exceptions.

### @ServiceSorter

The `@ServiceSorter` annotation **specifies the method used to sort a service provider**.

Properties:
- `#position`: sets the sorter ordering in case of multiple sorters
- `#reverse`: applies a reverse sorting

```java
@ServiceDefinition
public interface LargeLanguageModel {

  String summarize(String text);

  // 💡 Maximize quality
  @ServiceSorter(position = 1, reverse = true)
  int getQuality();

  // 💡 Minimize cost
  @ServiceSorter(position = 2)
  int getCost();

  static void main(String[] args) {
    LargeLanguageModelLoader.load()
      .map(search -> search.summarize("bla bla bla"))
      .ifPresent(System.out::println);
  }
}
```
_Source: [nbbrd/service/examples/LargeLanguageModel.java](java-service-examples/src/main/java/nbbrd/service/examples/LargeLanguageModel.java)_

Characteristics:
- There is no limit to the number of annotations per service.
- Sorting is done after filtering.

Constraints:
1. It only applies to methods of a service.
2. It does not apply to static methods.
3. The annotated method must have no-args.
4. The annotated method must return double, int, long or comparable.
5. The annotated method must not throw checked exceptions.

## Design

### API vs SPI

In some cases it is better to have a clear separation between API and SPI.

An API is designed to be called and used. It should be simple and foolproof.  
An SPI is designed to be extended and implemented. It can be complex but should be performant.

Here is an example on how to do it:

```java
public final class FileType {

  // 💡 API: designed to be called and used
  public static Optional<String> probeContentType(Path file) throws IOException {
    for (FileTypeSpi probe : FileTypeSpiLoader.get()) {
      String result = probe.getContentTypeOrNull(file);
      if (result != null) return Optional.of(result);
    }
    return Optional.empty();
  }

  public static void main(String[] args) throws IOException {
    for (String file : Arrays.asList("hello.csv", "stuff.txt")) {
      System.out.println(file + ": " + FileType.probeContentType(Paths.get(file)).orElse("?"));
    }
  }

  // 💡 SPI: designed to be extended and implemented
  @ServiceDefinition(
      quantifier = Quantifier.MULTIPLE,
      loaderName = "internal.{{canonicalName}}Loader",
      singleton = true
  )
  public interface FileTypeSpi {

    enum Accuracy {HIGH, LOW}

    String getContentTypeOrNull(Path file) throws IOException;

    @ServiceSorter
    Accuracy getAccuracy();
  }
}
```
_Source: [nbbrd/service/examples/FileType.java](java-service-examples/src/main/java/nbbrd/service/examples/FileType.java)_

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

## Developing

This project is written in Java and uses [Apache Maven](https://maven.apache.org/) as a build tool.  
It requires [Java 8 as minimum version](https://whichjdk.com/) and all its dependencies are hosted on [Maven Central](https://search.maven.org/).

The code can be build using any IDE or by just type-in the following commands in a terminal:

```shell
git clone https://github.com/nbbrd/java-service-util.git
cd java-service-util
mvn clean install
```

## Contributing

Any contribution is welcome and should be done through pull requests and/or issues.

## Licensing

The code of this project is licensed under the [European Union Public Licence (EUPL)](https://joinup.ec.europa.eu/page/eupl-text-11-12).

## Related work

This project is not the only one using with the SPI mechanism.  
Here is a non-exhaustive list of related work:
- [NetBeans Lookup](https://search.maven.org/search?q=g:org.netbeans.api%20AND%20a:org-openide-util-lookup&core=gav)
- [Google AutoService](https://www.baeldung.com/google-autoservice)
- [TOOListicon SPI-Annotation-Processor](https://github.com/toolisticon/SPI-Annotation-Processor)
- [Kordamp Jipsy](https://github.com/kordamp/jipsy)

## Alternatives

The SPI mechanism is not suitable for all use cases. Here are some alternatives:
- [Dependency injection](https://github.com/akullpp/awesome-java?tab=readme-ov-file#dependency-injection)

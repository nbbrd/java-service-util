package definition;

import java.lang.Class;
import java.lang.Iterable;
import java.lang.Object;
import java.lang.Runnable;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Custom service loader for {@link definition.TestNonNestedDef}.
 * <p>Properties:
 * <ul>
 * <li>Quantifier: OPTIONAL</li>
 * <li>Fallback: null</li>
 * <li>Preprocessing: null</li>
 * <li>Name: null</li>
 * <li>Batch type: null</li>
 * </ul>
 */
public final class TestNonNestedDefLoader {
  private final Iterable<?> providerSource;

  private final Runnable providerReloader;

  private TestNonNestedDefLoader(Iterable<?> providerSource, Runnable providerReloader) {
    this.providerSource = providerSource;
    this.providerReloader = providerReloader;
  }

  /**
   * Reloads the content by clearing the cache and fetching available providers.
   * <p>It should be called when the set of available providers may have changed.
   */
  public void reload() {
    providerReloader.run();
  }

  private Stream<TestNonNestedDef> stream() {
    return StreamSupport.stream(providerSource.spliterator(), false).filter(TestNonNestedDef.class::isInstance).map(TestNonNestedDef.class::cast);
  }

  /**
   * Gets an optional {@link definition.TestNonNestedDef} instance.
   * <p>Returns the first available provider after applying filters and sorters, or empty if none is found.
   * @return a non-null optional {@link definition.TestNonNestedDef} instance
   */
  public Optional<TestNonNestedDef> get() {
    return stream()
        .findFirst();
  }

  /**
   * Gets an optional {@link definition.TestNonNestedDef} instance.
   * <p>Returns the first available provider after applying filters and sorters, or empty if none is found.
   * @return a non-null optional {@link definition.TestNonNestedDef} instance
   * <br>This is equivalent to the following code: <code>builder().build().get()</code>
   * <br>Therefore, the returned value might be different at each call.
   */
  public static Optional<TestNonNestedDef> load() {
    return builder().build().get();
  }

  /**
   * Creates a new builder to configure and construct a loader instance.
   * <p>Use this method to customize the backend (e.g. NetBeans Lookup) instead of the default ServiceLoader.
   * @return a non-null new Builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private Function<Class<?>, Object> factory = ServiceLoader::load;

    private Function<Object, Iterable<?>> streamer = backend -> ((ServiceLoader) backend);

    private Consumer<Object> reloader = backend -> ((ServiceLoader) backend).reload();

    /**
     * Configures a custom backend for loading and reloading providers.
     * @param factory a function that creates a backend instance from a service class, not null
     * @param streamer a function that streams providers from the backend, not null
     * @param reloader a consumer that triggers a reload on the backend, not null
     * @return this builder instance
     */
    public <BACKEND> Builder backend(Function<Class<?>, BACKEND> factory,
        Function<BACKEND, Iterable<?>> streamer, Consumer<BACKEND> reloader) {
      this.factory = (Function<Class<?>, Object>) factory;
      this.streamer = (Function<Object, Iterable<?>>) streamer;
      this.reloader = (Consumer<Object>) reloader;
      return this;
    }

    /**
     * Configures a custom backend for loading providers (without reload support).
     * @param factory a function that creates a backend instance from a service class, not null
     * @param streamer a function that streams providers from the backend, not null
     * @return this builder instance
     */
    public <BACKEND> Builder backend(Function<Class<?>, BACKEND> factory,
        Function<BACKEND, Iterable<?>> streamer) {
      this.factory = (Function<Class<?>, Object>) factory;
      this.streamer = (Function<Object, Iterable<?>>) streamer;
      this.reloader = ignore -> {};
      return this;
    }

    /**
     * Builds a new loader instance using the configured backend.
     * @return a non-null loader instance
     */
    public TestNonNestedDefLoader build() {
      Object providerBackend = factory.apply(TestNonNestedDef.class);
      return new TestNonNestedDefLoader(
          streamer.apply(providerBackend), () -> reloader.accept(providerBackend)
          );
    }
  }
}

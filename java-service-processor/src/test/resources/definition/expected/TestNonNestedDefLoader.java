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
   */
  public void reload() {
    providerReloader.run();
  }

  private Stream<TestNonNestedDef> stream() {
    return StreamSupport.stream(providerSource.spliterator(), false).filter(TestNonNestedDef.class::isInstance).map(TestNonNestedDef.class::cast);
  }

  /**
   * Gets an optional {@link definition.TestNonNestedDef} instance.
   */
  public Optional<TestNonNestedDef> get() {
    return stream()
        .findFirst();
  }

  /**
   * Gets an optional {@link definition.TestNonNestedDef} instance.
   * <br>This is equivalent to the following code: <code>builder().build().get()</code>
   * <br>Therefore, the returned value might be different at each call.
   * @return a non-null value
   */
  public static Optional<TestNonNestedDef> load() {
    return builder().build().get();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private Function<Class<?>, Object> factory = ServiceLoader::load;

    private Function<Object, Iterable<?>> streamer = backend -> ((ServiceLoader) backend);

    private Consumer<Object> reloader = backend -> ((ServiceLoader) backend).reload();

    public <BACKEND> Builder backend(Function<Class<?>, BACKEND> factory,
        Function<BACKEND, Iterable<?>> streamer, Consumer<BACKEND> reloader) {
      this.factory = (Function<Class<?>, Object>) factory;
      this.streamer = (Function<Object, Iterable<?>>) streamer;
      this.reloader = (Consumer<Object>) reloader;
      return this;
    }

    public TestNonNestedDefLoader build() {
      Object providerBackend = factory.apply(TestNonNestedDef.class);
      return new TestNonNestedDefLoader(
          streamer.apply(providerBackend), () -> reloader.accept(providerBackend)
          );
    }
  }
}

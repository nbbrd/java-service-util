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

public final class TestBatchReloadingLoader {
  /**
   * Custom service loader for {@link definition.TestBatchReloading.Mutable}.
   * <p>Properties:
   * <ul>
   * <li>Quantifier: OPTIONAL</li>
   * <li>Fallback: null</li>
   * <li>Preprocessing: null</li>
   * <li>Name: null</li>
   * <li>Batch type: definition.TestBatchReloading.Batch</li>
   * </ul>
   */
  public static final class Mutable {
    private final Iterable<?> providerSource;

    private final Runnable providerReloader;

    private final Iterable<?> batchSource;

    private final Runnable batchReloader;

    private Mutable(Iterable<?> providerSource, Runnable providerReloader, Iterable<?> batchSource,
        Runnable batchReloader) {
      this.providerSource = providerSource;
      this.providerReloader = providerReloader;
      this.batchSource = batchSource;
      this.batchReloader = batchReloader;
    }

    /**
     * Reloads the content by clearing the cache and fetching available providers.
     */
    public void reload() {
      providerReloader.run();
      batchReloader.run();
    }

    private Stream<TestBatchReloading.Mutable> stream() {
      return Stream.concat(StreamSupport.stream(providerSource.spliterator(), false).filter(TestBatchReloading.Mutable.class::isInstance).map(TestBatchReloading.Mutable.class::cast), StreamSupport.stream(batchSource.spliterator(), false).filter(TestBatchReloading.Batch.class::isInstance).map(TestBatchReloading.Batch.class::cast).flatMap(o -> o.getProviders()));
    }

    /**
     * Gets an optional {@link definition.TestBatchReloading.Mutable} instance.
     */
    public Optional<TestBatchReloading.Mutable> get() {
      return stream()
          .findFirst();
    }

    /**
     * Gets an optional {@link definition.TestBatchReloading.Mutable} instance.
     * <br>This is equivalent to the following code: <code>builder().build().get()</code>
     * <br>Therefore, the returned value might be different at each call.
     * @return a non-null value
     */
    public static Optional<TestBatchReloading.Mutable> load() {
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

      public <BACKEND> Builder backend(Function<Class<?>, BACKEND> factory,
          Function<BACKEND, Iterable<?>> streamer) {
        this.factory = (Function<Class<?>, Object>) factory;
        this.streamer = (Function<Object, Iterable<?>>) streamer;
        this.reloader = ignore -> {};
        return this;
      }

      public Mutable build() {
        Object providerBackend = factory.apply(TestBatchReloading.Mutable.class);
        Object batchBackend = factory.apply(TestBatchReloading.Batch.class);
        return new Mutable(
            streamer.apply(providerBackend), () -> reloader.accept(providerBackend),
            streamer.apply(batchBackend), () -> reloader.accept(batchBackend)
            );
      }
    }
  }
}

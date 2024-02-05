package definition;

import java.lang.Iterable;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class TestBatchReloadingLoader {
  /**
   * Custom service loader for {@link definition.TestBatchReloading.Mutable}.
   * <br>This class is thread-safe.
   * <p>Properties:
   * <ul>
   * <li>Quantifier: OPTIONAL</li>
   * <li>Fallback: null</li>
   * <li>Preprocessing: null</li>
   * <li>Mutability: CONCURRENT</li>
   * <li>Singleton: false</li>
   * <li>Name: null</li>
   * <li>Backend: null</li>
   * <li>Cleaner: null</li>
   * <li>Batch: true</li>
   * <li>Batch name: null</li>
   * </ul>
   */
  public static final class Mutable {
    private final Iterable<TestBatchReloading.Mutable> source = ServiceLoader.load(TestBatchReloading.Mutable.class);

    private final Iterable<TestBatchReloadingBatch.Mutable> batch = ServiceLoader.load(TestBatchReloadingBatch.Mutable.class);

    private final AtomicReference<Optional<TestBatchReloading.Mutable>> resource = new AtomicReference<>(doLoad());

    private final Consumer<Iterable> cleaner = loader -> ((ServiceLoader)loader).reload();

    private Optional<TestBatchReloading.Mutable> doLoad() {
      return Stream.concat(StreamSupport.stream(source.spliterator(), false), StreamSupport.stream(batch.spliterator(), false).flatMap(o -> o.getProviders()))
          .findFirst();
    }

    /**
     * Gets an optional {@link definition.TestBatchReloading.Mutable} instance.
     * <br>This method is thread-safe.
     * @return the current non-null value
     */
    public Optional<TestBatchReloading.Mutable> get() {
      return resource.get();
    }

    /**
     * Sets an optional {@link definition.TestBatchReloading.Mutable} instance.
     * <br>This method is thread-safe.
     * @param newValue new non-null value
     */
    public void set(Optional<TestBatchReloading.Mutable> newValue) {
      resource.set(Objects.requireNonNull(newValue));
    }

    /**
     * Reloads the content by clearing the cache and fetching available providers.
     * <br>This method is thread-safe.
     */
    public void reload() {
      synchronized(source) {
        cleaner.accept(source);
        cleaner.accept(batch);
        set(doLoad());
      }
    }

    /**
     * Resets the content without clearing the cache.
     * <br>This method is thread-safe.
     */
    public void reset() {
      synchronized(source) {
        set(doLoad());
      }
    }
  }

  /**
   * Custom service loader for {@link definition.TestBatchReloading.MutableSingleton}.
   * <br>This class is thread-safe.
   * <p>Properties:
   * <ul>
   * <li>Quantifier: OPTIONAL</li>
   * <li>Fallback: null</li>
   * <li>Preprocessing: null</li>
   * <li>Mutability: CONCURRENT</li>
   * <li>Singleton: true</li>
   * <li>Name: null</li>
   * <li>Backend: null</li>
   * <li>Cleaner: null</li>
   * <li>Batch: true</li>
   * <li>Batch name: null</li>
   * </ul>
   */
  public static final class MutableSingleton {
    private static final Iterable<TestBatchReloading.MutableSingleton> SOURCE = ServiceLoader.load(TestBatchReloading.MutableSingleton.class);

    private static final Iterable<TestBatchReloadingBatch.MutableSingleton> BATCH = ServiceLoader.load(TestBatchReloadingBatch.MutableSingleton.class);

    private static final AtomicReference<Optional<TestBatchReloading.MutableSingleton>> RESOURCE = new AtomicReference<>(doLoad());

    private static final Consumer<Iterable> CLEANER = loader -> ((ServiceLoader)loader).reload();

    private MutableSingleton() {
    }

    private static Optional<TestBatchReloading.MutableSingleton> doLoad() {
      return Stream.concat(StreamSupport.stream(SOURCE.spliterator(), false), StreamSupport.stream(BATCH.spliterator(), false).flatMap(o -> o.getProviders()))
          .findFirst();
    }

    /**
     * Gets an optional {@link definition.TestBatchReloading.MutableSingleton} instance.
     * <br>This method is thread-safe.
     * @return the current non-null value
     */
    public static Optional<TestBatchReloading.MutableSingleton> get() {
      return RESOURCE.get();
    }

    /**
     * Sets an optional {@link definition.TestBatchReloading.MutableSingleton} instance.
     * <br>This method is thread-safe.
     * @param newValue new non-null value
     */
    public static void set(Optional<TestBatchReloading.MutableSingleton> newValue) {
      RESOURCE.set(Objects.requireNonNull(newValue));
    }

    /**
     * Reloads the content by clearing the cache and fetching available providers.
     * <br>This method is thread-safe.
     */
    public static void reload() {
      synchronized(SOURCE) {
        CLEANER.accept(SOURCE);
        CLEANER.accept(BATCH);
        set(doLoad());
      }
    }

    /**
     * Resets the content without clearing the cache.
     * <br>This method is thread-safe.
     */
    public static void reset() {
      synchronized(SOURCE) {
        set(doLoad());
      }
    }
  }
}
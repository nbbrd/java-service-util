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
   * <li>Name: null</li>
   * <li>Backend: null</li>
   * <li>Cleaner: null</li>
   * <li>Batch type: definition.TestBatchReloading.Batch</li>
   * </ul>
   */
  public static final class Mutable {
    private final Iterable<TestBatchReloading.Mutable> source = ServiceLoader.load(TestBatchReloading.Mutable.class);

    private final Iterable<TestBatchReloading.Batch> batch = ServiceLoader.load(TestBatchReloading.Batch.class);

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
}

package definition;

import java.lang.Iterable;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Consumer;
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
    private final Iterable<TestBatchReloading.Mutable> source = ServiceLoader.load(TestBatchReloading.Mutable.class);

    private final Iterable<TestBatchReloading.Batch> batch = ServiceLoader.load(TestBatchReloading.Batch.class);

    private final Consumer<Iterable> cleaner = loader -> ((ServiceLoader)loader).reload();

    public Optional<TestBatchReloading.Mutable> get() {
      return Stream.concat(StreamSupport.stream(source.spliterator(), false), StreamSupport.stream(batch.spliterator(), false).flatMap(o -> o.getProviders()))
          .findFirst();
    }

    /**
     * Reloads the content by clearing the cache and fetching available providers.
     */
    public void reload() {
      cleaner.accept(source);
      cleaner.accept(batch);
    }

    /**
     * Gets an optional {@link definition.TestBatchReloading.Mutable} instance.
     * <br>This is equivalent to the following code: <code>new Mutable().get()</code>
     * <br>Therefore, the returned value might be different at each call.
     * @return a non-null value
     */
    public static Optional<TestBatchReloading.Mutable> load() {
      return new Mutable().get();
    }
  }
}

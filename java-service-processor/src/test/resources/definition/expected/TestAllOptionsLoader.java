package definition;

import java.lang.Iterable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Custom service loader for {@link definition.TestAllOptions}.
 * <br>This class is thread-safe.
 * <p>Properties:
 * <ul>
 * <li>Quantifier: MULTIPLE</li>
 * <li>Fallback: null</li>
 * <li>Preprocessing: wrapper: none filters:[isAvailable+isDisabled] sorters:[getCost1+getCost2]</li>
 * <li>Mutability: CONCURRENT</li>
 * <li>Singleton: true</li>
 * <li>Name: null</li>
 * <li>Backend: null</li>
 * <li>Cleaner: null</li>
 * <li>Batch: true</li>
 * <li>Batch name: null</li>
 * </ul>
 */
public final class TestAllOptionsLoader {
  private static final Iterable<TestAllOptions> SOURCE = ServiceLoader.load(TestAllOptions.class);

  public static final Pattern ID_PATTERN = Pattern.compile("^[A-Z0-9]+(?:_[A-Z0-9]+)*$");

  private static final Iterable<TestAllOptionsBatch> BATCH = ServiceLoader.load(TestAllOptionsBatch.class);

  private static final Predicate<TestAllOptions> FILTER = ((Predicate<TestAllOptions>)o -> ID_PATTERN.matcher(o.getName()).matches()).and(TestAllOptions::isAvailable).and(((Predicate<TestAllOptions>)TestAllOptions::isDisabled).negate());

  private static final Comparator<TestAllOptions> SORTER = ((Comparator<TestAllOptions>)Comparator.comparingInt(TestAllOptions::getCost1)).thenComparing(Collections.reverseOrder(Comparator.comparingInt(TestAllOptions::getCost2)));

  private static final AtomicReference<List<TestAllOptions>> RESOURCE = new AtomicReference<>(doLoad());

  private static final Consumer<Iterable> CLEANER = loader -> ((ServiceLoader)loader).reload();

  private TestAllOptionsLoader() {
  }

  private static List<TestAllOptions> doLoad() {
    return Stream.concat(StreamSupport.stream(SOURCE.spliterator(), false), StreamSupport.stream(BATCH.spliterator(), false).flatMap(o -> o.getProviders()))
        .filter(FILTER)
        .sorted(SORTER)
        .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
  }

  /**
   * Gets a list of {@link definition.TestAllOptions} instances.
   * <br>This method is thread-safe.
   * @return the current non-null value
   */
  public static List<TestAllOptions> get() {
    return RESOURCE.get();
  }

  /**
   * Sets a list of {@link definition.TestAllOptions} instances.
   * <br>This method is thread-safe.
   * @param newValue new non-null value
   */
  public static void set(List<TestAllOptions> newValue) {
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

package definition;

import java.lang.Iterable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Custom service loader for {@link definition.TestAllOptions}.
 * <p>Properties:
 * <ul>
 * <li>Quantifier: MULTIPLE</li>
 * <li>Fallback: null</li>
 * <li>Preprocessing: filters:[isAvailable+isDisabled] sorters:[getCost1+getCost2]</li>
 * <li>Name: null</li>
 * <li>Batch type: null</li>
 * </ul>
 */
public final class TestAllOptionsLoader {
  public static final Pattern ID_PATTERN = Pattern.compile("^[A-Z0-9]+(?:_[A-Z0-9]+)*$");

  private final Iterable<TestAllOptions> source = ServiceLoader.load(TestAllOptions.class);

  private final Predicate<TestAllOptions> filter = ((Predicate<TestAllOptions>)o -> ID_PATTERN.matcher(o.getName()).matches()).and(TestAllOptions::isAvailable).and(((Predicate<TestAllOptions>)TestAllOptions::isDisabled).negate());

  private final Comparator<TestAllOptions> sorter = ((Comparator<TestAllOptions>)Comparator.comparingInt(TestAllOptions::getCost1)).thenComparing(Collections.reverseOrder(Comparator.comparingInt(TestAllOptions::getCost2)));

  private List<TestAllOptions> resource = doLoad();

  private final Consumer<Iterable> cleaner = loader -> ((ServiceLoader)loader).reload();

  private List<TestAllOptions> doLoad() {
    return StreamSupport.stream(source.spliterator(), false)
        .filter(filter)
        .sorted(sorter)
        .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
  }

  /**
   * Gets a list of {@link definition.TestAllOptions} instances.
   * @return the current non-null value
   */
  public List<TestAllOptions> get() {
    return resource;
  }

  /**
   * Sets a list of {@link definition.TestAllOptions} instances.
   * @param newValue new non-null value
   */
  private void set(List<TestAllOptions> newValue) {
    resource = Objects.requireNonNull(newValue);
  }

  /**
   * Reloads the content by clearing the cache and fetching available providers.
   */
  public void reload() {
    cleaner.accept(source);
    set(doLoad());
  }

  /**
   * Gets a list of {@link definition.TestAllOptions} instances.
   * <br>This is equivalent to the following code: <code>new TestAllOptionsLoader().get()</code>
   * <br>Therefore, the returned value might be different at each call.
   * @return a non-null value
   */
  public static List<TestAllOptions> load() {
    return new TestAllOptionsLoader().get();
  }
}

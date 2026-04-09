package definition;

import java.lang.Class;
import java.lang.Iterable;
import java.lang.Object;
import java.lang.Runnable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

  private final Iterable<?> providerSource;

  private final Runnable providerReloader;

  private final Predicate<TestAllOptions> filter = ((Predicate<TestAllOptions>)o -> ID_PATTERN.matcher(o.getName()).matches()).and(TestAllOptions::isAvailable).and(((Predicate<TestAllOptions>)TestAllOptions::isDisabled).negate());

  private final Comparator<TestAllOptions> sorter = ((Comparator<TestAllOptions>)Comparator.comparingInt(TestAllOptions::getCost1)).thenComparing(Collections.reverseOrder(Comparator.comparingInt(TestAllOptions::getCost2)));

  private TestAllOptionsLoader(Iterable<?> providerSource, Runnable providerReloader) {
    this.providerSource = providerSource;
    this.providerReloader = providerReloader;
  }

  /**
   * Reloads the content by clearing the cache and fetching available providers.
   */
  public void reload() {
    providerReloader.run();
  }

  private Stream<TestAllOptions> stream() {
    return StreamSupport.stream(providerSource.spliterator(), false).filter(TestAllOptions.class::isInstance).map(TestAllOptions.class::cast);
  }

  /**
   * Gets a list of {@link definition.TestAllOptions} instances.
   */
  public List<TestAllOptions> get() {
    return stream()
        .filter(filter)
        .sorted(sorter)
        .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
  }

  /**
   * Gets a list of {@link definition.TestAllOptions} instances.
   * <br>This is equivalent to the following code: <code>builder().build().get()</code>
   * <br>Therefore, the returned value might be different at each call.
   * @return a non-null value
   */
  public static List<TestAllOptions> load() {
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

    public TestAllOptionsLoader build() {
      Object providerBackend = factory.apply(TestAllOptions.class);
      return new TestAllOptionsLoader(
          streamer.apply(providerBackend), () -> reloader.accept(providerBackend)
          );
    }
  }
}

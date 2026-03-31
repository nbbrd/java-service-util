package definition;

import java.lang.Iterable;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Consumer;
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
  private final Iterable<TestNonNestedDef> source = ServiceLoader.load(TestNonNestedDef.class);

  private final Consumer<Iterable> cleaner = loader -> ((ServiceLoader)loader).reload();

  public Optional<TestNonNestedDef> get() {
    return StreamSupport.stream(source.spliterator(), false)
        .findFirst();
  }

  /**
   * Reloads the content by clearing the cache and fetching available providers.
   */
  public void reload() {
    cleaner.accept(source);
  }

  /**
   * Gets an optional {@link definition.TestNonNestedDef} instance.
   * <br>This is equivalent to the following code: <code>new TestNonNestedDefLoader().get()</code>
   * <br>Therefore, the returned value might be different at each call.
   * @return a non-null value
   */
  public static Optional<TestNonNestedDef> load() {
    return new TestNonNestedDefLoader().get();
  }
}

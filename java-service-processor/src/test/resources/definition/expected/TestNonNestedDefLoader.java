package definition;

import java.lang.Iterable;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

/**
 * Custom service loader for {@link definition.TestNonNestedDef}.
 * <br>This class is thread-safe.
 * <p>Properties:
 * <ul>
 * <li>Quantifier: OPTIONAL</li>
 * <li>Fallback: null</li>
 * <li>Preprocessing: null</li>
 * <li>Mutability: NONE</li>
 * <li>Singleton: false</li>
 * <li>Name: null</li>
 * <li>Backend: null</li>
 * <li>Cleaner: null</li>
 * <li>Batch: false</li>
 * <li>Batch name: null</li>
 * </ul>
 */
public final class TestNonNestedDefLoader {
  private final Iterable<TestNonNestedDef> source = ServiceLoader.load(TestNonNestedDef.class);

  private final Optional<TestNonNestedDef> resource = doLoad();

  private Optional<TestNonNestedDef> doLoad() {
    return StreamSupport.stream(source.spliterator(), false)
        .findFirst();
  }

  /**
   * Gets an optional {@link definition.TestNonNestedDef} instance.
   * <br>This method is thread-safe.
   * @return the current non-null value
   */
  public Optional<TestNonNestedDef> get() {
    return resource;
  }

  /**
   * Gets an optional {@link definition.TestNonNestedDef} instance.
   * <br>This is equivalent to the following code: <code>new TestNonNestedDefLoader().get()</code>
   * <br>Therefore, the returned value might be different at each call.
   * <br>This method is thread-safe.
   * @return a non-null value
   */
  public static Optional<TestNonNestedDef> load() {
    return new TestNonNestedDefLoader().get();
  }
}
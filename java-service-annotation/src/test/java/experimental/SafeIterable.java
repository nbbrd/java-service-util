package experimental;

import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

final class SafeIterable<T> implements Iterable<T> {

    private final Iterable<T> unsafeIterable;

    private final Consumer<Throwable> onUnexpectedError;

    public SafeIterable(Iterable<T> unsafeIterable, Consumer<Throwable> onUnexpectedError) {
        this.unsafeIterable = unsafeIterable;
        this.onUnexpectedError = onUnexpectedError;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Iterator<T> iterator() {
        try {
            return new SafeIterator<>(unsafeIterable.iterator(), onUnexpectedError);
        } catch (Throwable ex) {
            onUnexpectedError.accept(ex);
            return Collections.emptyIterator();
        }
    }

    public Stream<T> asStream() {
        return StreamSupport.stream(spliterator(), false).filter(Objects::nonNull);
    }

    private static final class SafeIterator<T> implements Iterator<T> {

        private final Iterator<T> unsafeIterator;
        private final Consumer<Throwable> onUnexpectedError;

        public SafeIterator(Iterator<T> unsafeIterator, Consumer<Throwable> onUnexpectedError) {
            this.unsafeIterator = unsafeIterator;
            this.onUnexpectedError = onUnexpectedError;
        }

        @Override
        public boolean hasNext() {
            try {
                return unsafeIterator.hasNext();
            } catch (Throwable ex) {
                onUnexpectedError.accept(ex);
                return false;
            }
        }

        @Override
        public T next() {
            try {
                return unsafeIterator.next();
            } catch (Throwable ex) {
                onUnexpectedError.accept(ex);
                return null;
            }
        }
    }
}

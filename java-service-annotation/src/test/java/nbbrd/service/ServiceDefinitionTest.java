package nbbrd.service;

import org.junit.jupiter.api.Test;
import org.openide.util.Lookup;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class ServiceDefinitionTest {

    public interface MySPI {
    }

    @SuppressWarnings("unused")
    @org.openide.util.lookup.ServiceProvider(service = MySPI.class)
    public static final class A implements MySPI {

        public A() {
            INSTANTIATION_COUNT.incrementAndGet();
        }
    }

    @SuppressWarnings("unused")
    @org.openide.util.lookup.ServiceProvider(service = MySPI.class)
    public static final class B implements MySPI {

        public B() {
            INSTANTIATION_COUNT.incrementAndGet();
        }
    }

    private static final AtomicInteger INSTANTIATION_COUNT = new AtomicInteger();

    @Test
    public void testServiceLoaderLaziness() {
        ServiceLoader<MySPI> main = ServiceLoader.load(MySPI.class);
        ServiceLoader<MySPI> other = ServiceLoader.load(MySPI.class);
        Lookup.Result<MySPI> lookup = Lookup.getDefault().lookupResult(MySPI.class);

        {
            Iterator<MySPI> firstIteratorOnMain = main.iterator();
            assertThat(INSTANTIATION_COUNT).hasValue(0);
            firstIteratorOnMain.next();
            assertThat(INSTANTIATION_COUNT).hasValue(1);
            firstIteratorOnMain.next();
            assertThat(INSTANTIATION_COUNT).hasValue(2);
            assertThat(firstIteratorOnMain.hasNext()).isFalse();
        }

        {
            Iterator<MySPI> secondIteratorOnMain = main.iterator();
            assertThat(INSTANTIATION_COUNT).hasValue(2);
            secondIteratorOnMain.next();
            assertThat(INSTANTIATION_COUNT).hasValue(2);
            secondIteratorOnMain.next();
            assertThat(INSTANTIATION_COUNT).hasValue(2);
            assertThat(secondIteratorOnMain.hasNext()).isFalse();
        }

        {
            Iterator<MySPI> firstIteratorOnOther = other.iterator();
            assertThat(INSTANTIATION_COUNT).hasValue(2);
            firstIteratorOnOther.next();
            assertThat(INSTANTIATION_COUNT).hasValue(3);
            firstIteratorOnOther.next();
            assertThat(INSTANTIATION_COUNT).hasValue(4);
            assertThat(firstIteratorOnOther.hasNext()).isFalse();
        }

        {
            Iterator<MySPI> secondIteratorOnOther = other.iterator();
            assertThat(INSTANTIATION_COUNT).hasValue(4);
            secondIteratorOnOther.next();
            assertThat(INSTANTIATION_COUNT).hasValue(4);
            secondIteratorOnOther.next();
            assertThat(INSTANTIATION_COUNT).hasValue(4);
            assertThat(secondIteratorOnOther.hasNext()).isFalse();
        }

        {
            Iterator<? extends MySPI> firstIteratorOnLookup = lookup.allInstances().iterator();
            assertThat(INSTANTIATION_COUNT).hasValue(4);
            firstIteratorOnLookup.next();
            assertThat(INSTANTIATION_COUNT).hasValue(6);
            firstIteratorOnLookup.next();
            assertThat(INSTANTIATION_COUNT).hasValue(6);
            assertThat(firstIteratorOnLookup.hasNext()).isFalse();
        }

        {
            Iterator<? extends MySPI> secondIteratorOnLookup = lookup.allInstances().iterator();
            assertThat(INSTANTIATION_COUNT).hasValue(6);
            secondIteratorOnLookup.next();
            assertThat(INSTANTIATION_COUNT).hasValue(6);
            secondIteratorOnLookup.next();
            assertThat(INSTANTIATION_COUNT).hasValue(6);
            assertThat(secondIteratorOnLookup.hasNext()).isFalse();
        }
    }
}

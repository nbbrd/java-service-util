package nbbrd.service.examples;

import org.openide.util.Lookup;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;

public enum NetBeansLookup implements Function<Class, Iterable>, Consumer<Iterable> {

    INSTANCE;

    @Override
    public Iterable apply(Class type) {
        return new NetBeansLookupResult(type);
    }

    @Override
    public void accept(Iterable iterable) {
        ((NetBeansLookupResult) iterable).reload();
    }

    private static final class NetBeansLookupResult implements Iterable {

        private final Lookup.Result result;
        private Collection instances;

        private NetBeansLookupResult(Class type) {
            this.result = Lookup.getDefault().lookupResult(type);
            this.instances = result.allInstances();
        }

        @Override
        public Iterator iterator() {
            return instances.iterator();
        }

        public void reload() {
            this.instances = result.allInstances();
        }
    }
}

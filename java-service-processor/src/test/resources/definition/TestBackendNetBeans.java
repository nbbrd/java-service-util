package definition;

import nbbrd.service.ServiceDefinition;
import org.openide.util.Lookup;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;

@ServiceDefinition(backend = TestBackendNetBeans.NetBeansLookup.class)
interface TestBackendNetBeans {

    enum NetBeansLookup implements Function<Class, Iterable>, Consumer<Iterable> {

        INSTANCE;

        @Override
        public Iterable apply(Class type) {
            return new Stuff(type);
        }

        @Override
        public void accept(Iterable iterable) {
            ((Stuff) iterable).reload();
        }

        private static final class Stuff implements Iterable {

            private final Lookup.Result result;
            private Collection instances;

            private Stuff(Class type) {
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
}

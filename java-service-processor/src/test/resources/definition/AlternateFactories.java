package definition;

import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import nbbrd.service.Quantifier;
import nbbrd.service.ServiceDefinition;

class AlternateFactories {

    @ServiceDefinition(quantifier = Quantifier.SINGLE, fallback = ConstructorFallback.class, lookup = ConstructorLookup.class)
    interface ConstructorService {
    }

    static class ConstructorFallback implements ConstructorService {

        public ConstructorFallback() {
        }
    }

    static class ConstructorLookup implements UnaryOperator<Stream<ConstructorService>> {

        public ConstructorLookup() {
        }

        @Override
        public Stream<ConstructorService> apply(Stream<ConstructorService> t) {
            return t;
        }
    }

    @ServiceDefinition(quantifier = Quantifier.SINGLE, fallback = StaticMethodFallback.class, lookup = StaticMethodLookup.class)
    interface StaticMethodService {
    }

    static class StaticMethodFallback implements StaticMethodService {

        public static StaticMethodFallback getInstance() {
            return new StaticMethodFallback();
        }

        private StaticMethodFallback() {
        }
    }

    static class StaticMethodLookup implements UnaryOperator<Stream<StaticMethodService>> {

        public static StaticMethodLookup getInstance() {
            return new StaticMethodLookup();
        }

        private StaticMethodLookup() {
        }

        @Override
        public Stream<StaticMethodService> apply(Stream<StaticMethodService> t) {
            return t;
        }
    }

    @ServiceDefinition(quantifier = Quantifier.SINGLE, fallback = EnumFieldFallback.class, lookup = EnumFieldLookup.class)
    interface EnumFieldService {
    }

    static enum EnumFieldFallback implements EnumFieldService {
        INSTANCE;
    }

    static enum EnumFieldLookup implements UnaryOperator<Stream<EnumFieldService>> {
        INSTANCE;

        @Override
        public Stream<EnumFieldService> apply(Stream<EnumFieldService> t) {
            return t;
        }
    }

    @ServiceDefinition(quantifier = Quantifier.SINGLE, fallback = StaticFieldFallback.class, lookup = StaticFieldLookup.class)
    interface StaticFieldService {
    }

    static class StaticFieldFallback implements StaticFieldService {

        public static final StaticFieldFallback STUFF = new StaticFieldFallback();

        private StaticFieldFallback() {
        }
    }

    static class StaticFieldLookup implements UnaryOperator<Stream<StaticFieldService>> {

        public static final StaticFieldLookup STUFF = new StaticFieldLookup();

        private StaticFieldLookup() {
        }

        @Override
        public Stream<StaticFieldService> apply(Stream<StaticFieldService> t) {
            return t;
        }
    }
}

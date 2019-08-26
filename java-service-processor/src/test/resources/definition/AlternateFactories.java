package definition;

import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import nbbrd.service.Quantifier;
import nbbrd.service.ServiceDefinition;

class AlternateFactories {

    @ServiceDefinition(quantifier = Quantifier.SINGLE, fallback = ConstructorFallback.class, preprocessor = ConstructorProc.class)
    interface ConstructorService {
    }

    static class ConstructorFallback implements ConstructorService {

        public ConstructorFallback() {
        }
    }

    static class ConstructorProc implements UnaryOperator<Stream<ConstructorService>> {

        public ConstructorProc() {
        }

        @Override
        public Stream<ConstructorService> apply(Stream<ConstructorService> t) {
            return t;
        }
    }

    @ServiceDefinition(quantifier = Quantifier.SINGLE, fallback = StaticMethodFallback.class, preprocessor = StaticMethodProc.class)
    interface StaticMethodService {
    }

    static class StaticMethodFallback implements StaticMethodService {

        public static StaticMethodFallback getInstance() {
            return new StaticMethodFallback();
        }

        private StaticMethodFallback() {
        }
    }

    static class StaticMethodProc implements UnaryOperator<Stream<StaticMethodService>> {

        public static StaticMethodProc getInstance() {
            return new StaticMethodProc();
        }

        private StaticMethodProc() {
        }

        @Override
        public Stream<StaticMethodService> apply(Stream<StaticMethodService> t) {
            return t;
        }
    }

    @ServiceDefinition(quantifier = Quantifier.SINGLE, fallback = EnumFieldFallback.class, preprocessor = EnumFieldProc.class)
    interface EnumFieldService {
    }

    static enum EnumFieldFallback implements EnumFieldService {
        INSTANCE;
    }

    static enum EnumFieldProc implements UnaryOperator<Stream<EnumFieldService>> {
        INSTANCE;

        @Override
        public Stream<EnumFieldService> apply(Stream<EnumFieldService> t) {
            return t;
        }
    }

    @ServiceDefinition(quantifier = Quantifier.SINGLE, fallback = StaticFieldFallback.class, preprocessor = StaticFieldProc.class)
    interface StaticFieldService {
    }

    static class StaticFieldFallback implements StaticFieldService {

        public static final StaticFieldFallback STUFF = new StaticFieldFallback();

        private StaticFieldFallback() {
        }
    }

    static class StaticFieldProc implements UnaryOperator<Stream<StaticFieldService>> {

        public static final StaticFieldProc STUFF = new StaticFieldProc();

        private StaticFieldProc() {
        }

        @Override
        public Stream<StaticFieldService> apply(Stream<StaticFieldService> t) {
            return t;
        }
    }
}

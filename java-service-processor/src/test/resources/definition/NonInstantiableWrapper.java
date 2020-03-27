package definition;

import nbbrd.service.ServiceDefinition;

class NonInstantiableWrapper {

    @ServiceDefinition(wrapper = WrapperByPrivateConstructor.class)
    interface ByPrivateConstructor {
    }

    static class WrapperByPrivateConstructor implements ByPrivateConstructor {

        private WrapperByPrivateConstructor() {
        }
    }

    @ServiceDefinition(wrapper = WrapperByNoArgConstructor.class)
    interface ByNoArgConstructor {
    }

    static class WrapperByNoArgConstructor implements ByNoArgConstructor {

        public WrapperByNoArgConstructor() {
        }
    }

    @ServiceDefinition(wrapper = WrapperByInvalidArgConstructor.class)
    interface ByInvalidArgConstructor {
    }

    static class WrapperByInvalidArgConstructor implements ByInvalidArgConstructor {

        public WrapperByInvalidArgConstructor(String delegate) {
        }
    }

    @ServiceDefinition(wrapper = WrapperByToManyStaticMethods.class)
    interface ByToManyStaticMethods {
    }

    static class WrapperByToManyStaticMethods implements ByToManyStaticMethods {

        public static ByToManyStaticMethods wrap1(ByToManyStaticMethods delegate) {
            return new WrapperByToManyStaticMethods();
        }

        public static ByToManyStaticMethods wrap2(ByToManyStaticMethods delegate) {
            return new WrapperByToManyStaticMethods();
        }
    }
}

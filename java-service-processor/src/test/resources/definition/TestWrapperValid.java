package definition;

import nbbrd.service.ServiceDefinition;

class TestWrapperValid {

    @ServiceDefinition(wrapper = WrapperByConstructor.class)
    interface ByConstructor {
    }

    static class WrapperByConstructor implements ByConstructor {

        public WrapperByConstructor(ByConstructor delegate) {
        }
    }

    @ServiceDefinition(wrapper = WrapperByStaticMethod.class)
    interface ByStaticMethod {
    }

    static class WrapperByStaticMethod implements ByStaticMethod {

        public static ByStaticMethod wrap(ByStaticMethod delegate) {
            return new WrapperByStaticMethod();
        }
    }

    @ServiceDefinition(wrapper = WrapperByStaticMethodX.class)
    interface ByStaticMethodX {
    }

    static class WrapperByStaticMethodX implements ByStaticMethodX {

        public static WrapperByStaticMethodX wrap(ByStaticMethodX delegate) {
            return new WrapperByStaticMethodX();
        }
    }
}

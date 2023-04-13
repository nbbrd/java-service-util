package nbbrd.service.examples;

public interface MultiAnnotationProcessors {

    @nbbrd.service.ServiceProvider
    class Implementation1 implements MultiAnnotationProcessors {
    }

    // FIXME: should play nice together instead we have
    //  "javax.annotation.processing.FilerException: Attempt to reopen a file for path"
//    @org.openide.util.lookup.ServiceProvider(service = MultiAnnotationProcessors.class)
    class Implementation2 implements MultiAnnotationProcessors {
    }
}

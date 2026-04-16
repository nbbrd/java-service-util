package internal.nbbrd.service.provider;

import internal.nbbrd.service.ProcessorTool;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

final class ServiceProviderCollector extends ProcessorTool {

    public ServiceProviderCollector(Supplier<ProcessingEnvironment> envSupplier) {
        super(envSupplier);
    }

    private final List<ProviderRef> pendingRefs = new ArrayList<>();

    public void collect(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        pendingRefs.addAll(new AnnotationRegistry(annotations, roundEnv).readAll());
    }

    public void clear() {
        pendingRefs.clear();
    }

    public List<ProviderRef> build() {
        return new ArrayList<>(pendingRefs);
    }
}

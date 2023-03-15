package internal.nbbrd.service.provider;

import internal.nbbrd.service.ProcessorTool;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

final class ServiceProviderCollector extends ProcessorTool {

    public ServiceProviderCollector(Supplier<ProcessingEnvironment> envSupplier) {
        super(envSupplier);
    }

    // we store Name instead of TypeElement due to a bug(?) in JDK8
    // that creates not-equivalent elements between rounds
    private final Set<Name> pendingRefs = new HashSet<>();

    public void collect(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        new AnnotationRegistry(annotations, roundEnv)
                .readAll()
                .forEach(ref -> pendingRefs.add(ref.getProvider().getQualifiedName()));
    }

    public void clear() {
        pendingRefs.clear();
    }

    public List<ProviderRef> build() {
        Elements elements = getEnv().getElementUtils();
        return pendingRefs
                .stream()
                .map(x -> elements.getTypeElement(x.toString()))
                .flatMap(AnnotationRegistry::newRefs)
                .collect(Collectors.toList());
    }
}

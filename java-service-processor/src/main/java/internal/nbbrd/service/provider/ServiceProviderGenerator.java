package internal.nbbrd.service.provider;

import internal.nbbrd.service.Instantiator;
import internal.nbbrd.service.ProcessorTool;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;


final class ServiceProviderGenerator extends ProcessorTool {

    public ServiceProviderGenerator(Supplier<ProcessingEnvironment> envSupplier) {
        super(envSupplier);
    }

    public void generate(List<ProviderRef> annotationRefs) throws IOException {
        registerClassPath(annotationRefs, new ClassPathRegistry(getEnv()));
    }

    private void registerClassPath(List<ProviderRef> annotationRefs, ClassPathRegistry classPath) throws IOException {
        for (Map.Entry<TypeElement, List<ProviderRef>> x : getRefByService(annotationRefs).entrySet()) {
            registerClassPath(x.getKey(), x.getValue(), classPath);
        }
    }

    private void registerClassPath(TypeElement service, List<ProviderRef> refs, ClassPathRegistry classPath) throws IOException {
        List<String> oldLines = classPath.readLinesByService(service);
        List<String> newLines = classPath.formatAll(service, generateDelegates(refs));
        classPath.writeLinesByService(merge(oldLines, newLines), service);
    }

    private List<ProviderRef> generateDelegates(List<ProviderRef> refs) {
        Types types = getEnv().getTypeUtils();
        for (ProviderRef ref : refs) {
            if (Instantiator.allOf(types, ref.getService(), ref.getProvider()).stream().anyMatch(ServiceProviderGenerator::isStaticMethod)) {
                getEnv().error(ref, "Static method support not implemented yet");
            }
        }
        return refs;
    }

    private static boolean isStaticMethod(Instantiator o) {
        return o.getKind() == Instantiator.Kind.STATIC_METHOD;
    }

    private static Map<TypeElement, List<ProviderRef>> getRefByService(List<ProviderRef> annotationRefs) {
        return annotationRefs.stream().collect(groupingBy(ProviderRef::getService));
    }

    static List<String> merge(List<String> first, List<String> second) {
        return Stream.concat(first.stream(), second.stream())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}

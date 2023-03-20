package internal.nbbrd.service.provider;

import internal.nbbrd.service.Instantiator;
import internal.nbbrd.service.ProcessorTool;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;


final class ServiceProviderChecker extends ProcessorTool {

    public ServiceProviderChecker(Supplier<ProcessingEnvironment> envSupplier) {
        super(envSupplier);
    }

    public boolean check(List<ProviderRef> refs) throws IOException {
        return checkDuplicatedRefs(refs)
                && checkRefRules(refs)
                && checkModulePath(refs);
    }

    private boolean checkDuplicatedRefs(List<ProviderRef> refs) {
        Set<ProviderRef> duplicates = ProviderRef.getDuplicates(refs);
        for (ProviderRef ref : duplicates) {
            getEnv().error(ref, String.format("Duplicated provider: '%1$s'", ref.getProvider()));
        }
        return duplicates.isEmpty();
    }

    private boolean checkRefRules(List<ProviderRef> refs) {
        return refs.stream().allMatch(this::checkRefRules);
    }

    private boolean checkRefRules(ProviderRef ref) {
        Elements elements = getEnv().getElementUtils();
        Types types = getEnv().getTypeUtils();

        if (types.isSameType(ref.getService().asType(), elements.getTypeElement(Void.class.getName()).asType())) {
            getEnv().error(ref, "Cannot infer service from provider ");
            return false;
        }

        if (!types.isAssignable(ref.getProvider().asType(), types.erasure(ref.getService().asType()))) {
            getEnv().error(ref, String.format("Provider '%1$s' doesn't extend nor implement service '%2$s'", ref.getProvider(), ref.getService()));
            return false;
        }

        if (ref.getProvider().getEnclosingElement().getKind() == ElementKind.CLASS && !ref.getProvider().getModifiers().contains(Modifier.STATIC)) {
            getEnv().error(ref, String.format("Provider '%1$s' must be static inner class", ref.getProvider()));
            return false;
        }

        if (ref.getProvider().getModifiers().contains(Modifier.ABSTRACT)) {
            getEnv().error(ref, String.format("Provider '%1$s' must not be abstract", ref.getProvider()));
            return false;
        }

        if (Instantiator.allOf(types, ref.getService(), ref.getProvider()).stream().noneMatch(this::isValidInstantiator)) {
            getEnv().error(ref, String.format("Provider '%1$s' must have a public no-argument constructor", ref.getProvider()));
            return false;
        }

        return true;
    }

    private boolean isValidInstantiator(Instantiator instantiator) {
        switch (instantiator.getKind()) {
            case CONSTRUCTOR:
                return true;
            case STATIC_METHOD:
                return instantiator.getElement().getSimpleName().contentEquals("provider");
            default:
                return false;
        }
    }

    private boolean checkModulePath(List<ProviderRef> annotationRefs) throws IOException {
        return new ModulePathRegistry(getEnv())
                .readAll()
                .map(providerEntries -> checkModulePath(annotationRefs, providerEntries))
                .orElse(true);
    }

    private boolean checkModulePath(List<ProviderRef> annotationRefs, List<ProviderEntry> modulePathEntries) {
        return checkMissingRefs(annotationRefs, modulePathEntries)
                && checkMissingEntries(annotationRefs, modulePathEntries);
    }

    private boolean checkMissingRefs(List<ProviderRef> annotationRefs, List<ProviderEntry> modulePathEntries) {
        List<ProviderRef> missingRefs = getMissingRefs(annotationRefs, modulePathEntries).collect(Collectors.toList());
        for (ProviderRef ref : missingRefs) {
            getEnv().error(ref, "Missing module-info directive 'provides " + ref.getService() + " with " + ref.getProvider() + "'");
        }
        return missingRefs.isEmpty();
    }

    private boolean checkMissingEntries(List<ProviderRef> annotationRefs, List<ProviderEntry> modulePathEntries) {
        List<ProviderEntry> missingEntries = getMissingEntries(annotationRefs, modulePathEntries).collect(Collectors.toList());
        for (ProviderEntry entry : missingEntries) {
            errorEntry(entry, "Missing annotation for '" + entry + "'");
        }
        return missingEntries.isEmpty();
    }

    private void errorEntry(ProviderEntry entry, String message) {
        TypeElement optionalType = getEnv().getElementUtils().getTypeElement(entry.getProvider());
        if (optionalType != null) {
            getEnv().getMessager().printMessage(Diagnostic.Kind.ERROR, message, optionalType);
        } else {
            getEnv().getMessager().printMessage(Diagnostic.Kind.ERROR, message);
        }
    }

    static Stream<ProviderRef> getMissingRefs(List<ProviderRef> annotationRefs, List<ProviderEntry> modulePathEntries) {
        return annotationRefs
                .stream()
                .filter(ref -> !modulePathEntries.contains(ref.toEntry()));
    }

    static Stream<ProviderEntry> getMissingEntries(List<ProviderRef> annotationRefs, List<ProviderEntry> modulePathEntries) {
        Set<ProviderEntry> annotationEntries = annotationRefs.stream().map(ProviderRef::toEntry).collect(Collectors.toSet());

        return modulePathEntries
                .stream()
                .filter(entry -> (!annotationEntries.contains(entry)));
    }
}

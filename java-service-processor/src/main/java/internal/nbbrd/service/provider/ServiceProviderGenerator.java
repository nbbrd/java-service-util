package internal.nbbrd.service.provider;

import com.squareup.javapoet.*;
import internal.nbbrd.service.Instantiator;
import internal.nbbrd.service.ProcessorTool;
import internal.nbbrd.service.ProcessorUtil;
import nbbrd.service.ServiceDefinition;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;


final class ServiceProviderGenerator extends ProcessorTool {

    public ServiceProviderGenerator(Supplier<ProcessingEnvironment> envSupplier) {
        super(envSupplier);
    }

    public void generate(List<ProviderRef> annotationRefs) throws IOException {
        // Generate batch providers for enums and collect their batch service registrations
        List<BatchProviderRef> batchRefs = collectBatchProviderRefs(annotationRefs);
        List<BatchProviderRegistration> batchRegistrations = generateBatchProviders(batchRefs);

        // Filter out enum providers that have batch providers generated
        List<ProviderRef> refsToRegister = filterOutEnumsWithBatchProviders(annotationRefs, batchRefs);

        // Register in SPI files
        ClassPathRegistry classPath = new ClassPathRegistry(getEnv());
        registerClassPath(refsToRegister, classPath);
        registerBatchProviders(batchRegistrations, classPath);
    }

    private void registerClassPath(List<ProviderRef> annotationRefs, ClassPathRegistry classPath) throws IOException {
        for (Map.Entry<TypeElement, List<ProviderRef>> x : getRefByService(annotationRefs).entrySet()) {
            registerClassPath(x.getKey(), x.getValue(), classPath);
        }
    }

    private void registerClassPath(TypeElement service, List<ProviderRef> refs, ClassPathRegistry classPath) throws IOException {
        List<ProviderRef> providerRefs = generateDelegates(refs);
        providerRefs.sort(BY_PROVIDER_NAME);

        List<ProviderConfigurationFileLine> oldLines = classPath.readLinesByService(service);
        List<ProviderConfigurationFileLine> newLines = classPath.formatAll(service, providerRefs);
        classPath.writeLinesByService(concat(oldLines, newLines), service);
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

    private List<BatchProviderRef> collectBatchProviderRefs(List<ProviderRef> refs) {
        List<BatchProviderRef> result = new ArrayList<>();
        for (ProviderRef ref : refs) {
            if (ref.getProvider().getKind() == ElementKind.ENUM) {
                getBatchProviderRef(ref).ifPresent(result::add);
            }
        }
        return result;
    }

    private List<ProviderRef> filterOutEnumsWithBatchProviders(List<ProviderRef> annotationRefs, List<BatchProviderRef> batchRefs) {
        // Create a set of enum providers that have batch providers for quick lookup
        Set<TypeElement> enumsWithBatchProviders = batchRefs.stream()
                .map(BatchProviderRef::getEnumProvider)
                .collect(Collectors.toSet());

        // Filter out those enum providers
        return annotationRefs.stream()
                .filter(ref -> !enumsWithBatchProviders.contains(ref.getProvider()))
                .collect(Collectors.toList());
    }

    private Optional<BatchProviderRef> getBatchProviderRef(ProviderRef ref) {
        ServiceDefinition annotation = ref.getService().getAnnotation(ServiceDefinition.class);
        if (annotation == null) {
            return Optional.empty();
        }

        TypeMirror batchType = ProcessorUtil.extractResultType(annotation::batchType);
        if (batchType.toString().equals(Void.class.getName())) {
            return Optional.empty();
        }

        TypeElement batchTypeElement = getEnv().asTypeElement(batchType);
        return findBatchMethod(batchTypeElement, ref.getService())
                .map(method -> new BatchProviderRef(
                        batchTypeElement,
                        ref.getProvider(),
                        ref.getService(),
                        method.getSimpleName().toString(),
                        method.getReturnType()
                ));
    }

    private Optional<ExecutableElement> findBatchMethod(TypeElement batchType, TypeElement serviceType) {
        Types types = getEnv().getTypeUtils();
        return ElementFilter.methodsIn(batchType.getEnclosedElements())
                .stream()
                .filter(method -> method.getModifiers().contains(Modifier.PUBLIC))
                .filter(method -> !method.getModifiers().contains(Modifier.STATIC))
                .filter(method -> method.getParameters().isEmpty())
                .filter(method -> isBatchReturnType(method.getReturnType(), serviceType, types))
                .findFirst();
    }

    private boolean isBatchReturnType(TypeMirror returnType, TypeElement serviceType, Types types) {
        return BatchMethodHelper.resolve(returnType, serviceType, types, getEnv().getElementUtils()) != null;
    }

    private List<BatchProviderRegistration> generateBatchProviders(List<BatchProviderRef> batchRefs) {
        List<BatchProviderRegistration> result = new ArrayList<>();
        for (BatchProviderRef ref : batchRefs) {
            String providerClassName = generateBatchProvider(ref);
            result.add(new BatchProviderRegistration(ref.getBatchType(), providerClassName));
        }
        return result;
    }

    private String generateBatchProvider(BatchProviderRef ref) {
        TypeSpec batchImpl = buildBatchClass(ref);
        String packageName = getEnv().getElementUtils().getPackageOf(ref.getEnumProvider()).getQualifiedName().toString();
        JavaFile javaFile = JavaFile.builder(packageName, batchImpl).build();
        ProcessorUtil.write(getEnv(), javaFile);

        // Return the fully qualified class name
        return packageName.isEmpty()
                ? ref.getEnumProvider().getSimpleName() + "BatchProvider"
                : packageName + "." + ref.getEnumProvider().getSimpleName() + "BatchProvider";
    }

    private void registerBatchProviders(List<BatchProviderRegistration> registrations, ClassPathRegistry classPath) throws IOException {
        for (BatchProviderRegistration registration : registrations) {
            List<ProviderConfigurationFileLine> oldLines = classPath.readLinesByService(registration.getBatchService());
            List<ProviderConfigurationFileLine> newLines = new ArrayList<>();
            newLines.add(ProviderConfigurationFileLine.ofProviderBinaryName(registration.getProviderClassName()));
            classPath.writeLinesByService(concat(oldLines, newLines), registration.getBatchService());
        }
    }

    private TypeSpec buildBatchClass(BatchProviderRef ref) {
        String className = ref.getEnumProvider().getSimpleName() + "BatchProvider";
        ClassName enumName = ClassName.get(ref.getEnumProvider());
        ClassName batchName = ClassName.get(ref.getBatchType());

        MethodSpec method = MethodSpec.methodBuilder(ref.getBatchMethodName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(TypeName.get(ref.getBatchMethodReturnType()))
                .addStatement("return $L", buildBatchMethodBody(enumName, ref))
                .build();

        return TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(batchName)
                .addMethod(method)
                .build();
    }

    private CodeBlock buildBatchMethodBody(ClassName enumName, BatchProviderRef ref) {
        BatchMethodHelper.ReturnKind kind = BatchMethodHelper.resolve(
                ref.getBatchMethodReturnType(), ref.getService(),
                getEnv().getTypeUtils(), getEnv().getElementUtils()
        );

        if (kind == BatchMethodHelper.ReturnKind.STREAM) {
            return CodeBlock.of("$T.stream($T.values())", Arrays.class, enumName);
        } else if (kind == BatchMethodHelper.ReturnKind.ARRAY) {
            return CodeBlock.of("$T.values()", enumName);
        } else {
            return CodeBlock.of("$T.asList($T.values())", Arrays.class, enumName);
        }
    }

    private static boolean isStaticMethod(Instantiator o) {
        return o.getKind() == Instantiator.Kind.STATIC_METHOD;
    }

    private static Map<TypeElement, List<ProviderRef>> getRefByService(List<ProviderRef> annotationRefs) {
        return annotationRefs.stream().collect(groupingBy(ProviderRef::getService));
    }

    static List<ProviderConfigurationFileLine> concat(List<ProviderConfigurationFileLine> first, List<ProviderConfigurationFileLine> second) {
        return Stream.concat(first.stream(), second.stream()).collect(Collectors.toList());
    }

    private static final Comparator<ProviderRef> BY_PROVIDER_NAME = Comparator.comparing(ref -> ref.getProvider().getQualifiedName().toString());
}

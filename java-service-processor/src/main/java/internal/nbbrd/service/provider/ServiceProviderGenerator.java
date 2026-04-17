package internal.nbbrd.service.provider;

import com.squareup.javapoet.*;
import internal.nbbrd.service.ProcessorTool;
import internal.nbbrd.service.ProcessorUtil;
import nbbrd.service.ServiceDefinition;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
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

        // Generate delegates for enum constants when batchType is not defined
        List<ProviderRef> enumDelegateRefs = generateEnumDelegates(annotationRefs, batchRefs);

        // Filter out enum providers that have batch providers or enum delegates generated
        Set<TypeElement> enumsWithBatchProviders = batchRefs.stream()
                .map(BatchProviderRef::getEnumProvider)
                .collect(Collectors.toSet());
        Set<TypeElement> enumsWithDelegates = enumDelegateRefs.stream()
                .map(ProviderRef::getProvider)
                .collect(Collectors.toSet());

        List<ProviderRef> refsToRegister = annotationRefs.stream()
                .filter(ref -> !enumsWithBatchProviders.contains(ref.getProvider()))
                .filter(ref -> !enumsWithDelegates.contains(ref.getProvider()))
                .collect(Collectors.toList());

        // Add enum delegate refs to the list to register
        refsToRegister.addAll(enumDelegateRefs);

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

        // Filter out duplicates - only add new lines that aren't already in oldLines
        List<ProviderConfigurationFileLine> linesToAdd = newLines.stream()
                .filter(newLine -> !oldLines.contains(newLine))
                .collect(Collectors.toList());

        classPath.writeLinesByService(concat(oldLines, linesToAdd), service);
    }

    private List<ProviderRef> generateDelegates(List<ProviderRef> refs) {
        List<ProviderRef> result = new ArrayList<>();
        for (ProviderRef ref : refs) {
            if (ref.getDelegateSource().isPresent()) {
                // Generate delegate wrapper for static field/method
                Element source = ref.getDelegateSource().get();
                String generatedClassName = generateDelegateWrapper(ref, source);
                result.add(new ProviderRef(ref.getService(), ref.getProvider(), Optional.empty(), Optional.of(generatedClassName)));
            } else {
                result.add(ref);
            }
        }
        return result;
    }

    private String generateDelegateWrapper(ProviderRef ref, Element source) {
        ClassName serviceName = ClassName.get(ref.getService());
        String delegateClassName = ref.getProvider().getSimpleName() + "_" + source.getSimpleName() + "Delegate";

        // Get all methods from the service interface
        List<ExecutableElement> methods = ElementFilter.methodsIn(
                getEnv().getElementUtils().getAllMembers(ref.getService()))
                .stream()
                .filter(m -> !m.getModifiers().contains(Modifier.FINAL))
                .filter(m -> !m.getModifiers().contains(Modifier.NATIVE))
                .filter(m -> !m.getModifiers().contains(Modifier.STATIC))
                .collect(Collectors.toList());

        // Generate delegate wrapper with custom name and constructor
        TypeSpec delegateSpec = buildDelegateClass(serviceName, delegateClassName, methods, ref.getProvider(), source);

        // Write the file
        String packageName = getEnv().getElementUtils().getPackageOf(ref.getProvider()).getQualifiedName().toString();
        JavaFile javaFile = JavaFile.builder(packageName, delegateSpec).build();
        ProcessorUtil.write(getEnv(), javaFile);

        // Return the fully qualified class name
        return packageName.isEmpty() ? delegateClassName : packageName + "." + delegateClassName;
    }

    private TypeSpec buildDelegateClass(ClassName serviceType, String className, List<ExecutableElement> methods,
                                        TypeElement provider, Element source) {
        FieldSpec delegateField = FieldSpec
                .builder(serviceType, "delegate", Modifier.PRIVATE, Modifier.FINAL)
                .build();

        MethodSpec constructor = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Creates a new delegate instance.\n")
                .addJavadoc("<p>The delegate wraps {@code $T.$L} which serves as the source for all method calls.</p>\n",
                        provider, source.getSimpleName())
                .addStatement("this.$N = $T.$L$L", delegateField, provider, source.getSimpleName(),
                        source.getKind() == ElementKind.METHOD ? "()" : "")
                .build();

        TypeSpec.Builder result = TypeSpec
                .classBuilder(className)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc("Generated delegate wrapper for {@link $T}.\n", serviceType)
                .addJavadoc("<p>This class delegates all method calls to {@code $T.$L}.</p>\n",
                        provider, source.getSimpleName())
                .addJavadoc("<p>Generated by {@code @ServiceProvider} annotation processor.</p>\n")
                .addSuperinterface(serviceType)
                .addField(delegateField)
                .addMethod(constructor);

        methods.forEach(method -> result.addMethod(buildDelegateMethod(method)));

        return result.build();
    }

    private MethodSpec buildDelegateMethod(ExecutableElement method) {
        MethodSpec.Builder builder = MethodSpec.overriding(method);

        String args = method.getParameters()
                .stream()
                .map(p -> p.getSimpleName().toString())
                .collect(Collectors.joining(", "));

        if (method.getReturnType().getKind() == TypeKind.VOID) {
            builder.addStatement("delegate.$L($L)", method.getSimpleName(), args);
        } else {
            builder.addStatement("return delegate.$L($L)", method.getSimpleName(), args);
        }

        return builder.build();
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

    private List<ProviderRef> generateEnumDelegates(List<ProviderRef> annotationRefs, List<BatchProviderRef> batchRefs) {
        // Get enum providers that have batch providers
        Set<TypeElement> enumsWithBatchProviders = batchRefs.stream()
                .map(BatchProviderRef::getEnumProvider)
                .collect(Collectors.toSet());

        List<ProviderRef> result = new ArrayList<>();
        
        for (ProviderRef ref : annotationRefs) {
            // Only process enums that don't have batch providers
            if (ref.getProvider().getKind() == ElementKind.ENUM 
                    && !enumsWithBatchProviders.contains(ref.getProvider())) {
                
                // Get all enum constants
                List<VariableElement> enumConstants = ElementFilter.fieldsIn(
                        ref.getProvider().getEnclosedElements())
                        .stream()
                        .filter(field -> field.getKind() == ElementKind.ENUM_CONSTANT)
                        .collect(Collectors.toList());
                
                // Generate a delegate for each enum constant
                for (VariableElement enumConstant : enumConstants) {
                    String generatedClassName = generateDelegateWrapper(ref, enumConstant);
                    result.add(new ProviderRef(
                            ref.getService(), 
                            ref.getProvider(), 
                            Optional.empty(), 
                            Optional.of(generatedClassName)
                    ));
                }
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

            // Filter out duplicates
            List<ProviderConfigurationFileLine> linesToAdd = newLines.stream()
                    .filter(newLine -> !oldLines.contains(newLine))
                    .collect(Collectors.toList());

            classPath.writeLinesByService(concat(oldLines, linesToAdd), registration.getBatchService());
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


    private static Map<TypeElement, List<ProviderRef>> getRefByService(List<ProviderRef> annotationRefs) {
        return annotationRefs.stream().collect(groupingBy(ProviderRef::getService));
    }

    static List<ProviderConfigurationFileLine> concat(List<ProviderConfigurationFileLine> first, List<ProviderConfigurationFileLine> second) {
        return Stream.concat(first.stream(), second.stream()).collect(Collectors.toList());
    }

    private static final Comparator<ProviderRef> BY_PROVIDER_NAME = Comparator.comparing(ref -> ref.getProvider().getQualifiedName().toString());
}

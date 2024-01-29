/*
 * Copyright 2019 National Bank of Belgium
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */
package internal.nbbrd.service.definition;

import com.squareup.javapoet.ClassName;
import internal.nbbrd.service.ExtEnvironment;
import internal.nbbrd.service.Instantiator;
import internal.nbbrd.service.ProcessorUtil;
import internal.nbbrd.service.Wrapper;
import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceFilter;
import nbbrd.service.ServiceId;
import nbbrd.service.ServiceSorter;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author Philippe Charles
 */
final class ServiceDefinitionCollector {

    private final ExtEnvironment env;
    private final PrimitiveType intType;
    private final PrimitiveType longType;
    private final PrimitiveType doubleType;
    private final DeclaredType comparableType;

    public ServiceDefinitionCollector(ProcessingEnvironment env) {
        this.env = new ExtEnvironment(env);
        Types types = env.getTypeUtils();
        this.intType = types.getPrimitiveType(TypeKind.INT);
        this.longType = types.getPrimitiveType(TypeKind.LONG);
        this.doubleType = types.getPrimitiveType(TypeKind.DOUBLE);
        this.comparableType = types.getDeclaredType(this.env.asTypeElement(Comparable.class));
    }

    public LoadData collect(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        LoadData.Builder result = LoadData.builder();
        for (TypeElement annotation : annotations) {
            switch (annotation.getSimpleName().toString()) {
                case "ServiceDefinition":
                    roundEnv.getElementsAnnotatedWith(annotation)
                            .stream()
                            .map(TypeElement.class::cast)
                            .map(this::definitionOf)
                            .forEach(result::definition);
                    break;
                case "ServiceFilter":
                    roundEnv.getElementsAnnotatedWith(annotation)
                            .stream()
                            .map(ExecutableElement.class::cast)
                            .map(this::filterOf)
                            .forEach(result::filter);
                    break;
                case "ServiceSorter":
                    roundEnv.getElementsAnnotatedWith(annotation)
                            .stream()
                            .map(ExecutableElement.class::cast)
                            .map(this::sorterOf)
                            .forEach(result::sorter);
                    break;
                case "ServiceId":
                    roundEnv.getElementsAnnotatedWith(annotation)
                            .stream()
                            .map(ExecutableElement.class::cast)
                            .map(this::idOf)
                            .forEach(result::id);
                    break;
            }
        }
        return result.build();
    }

    private LoadDefinition definitionOf(TypeElement serviceType) {
        ServiceDefinition annotation = serviceType.getAnnotation(ServiceDefinition.class);
        Types types = env.getTypeUtils();

        Optional<TypeInstantiator> fallback = nonNull(annotation::fallback, Void.class)
                .map(fallbackType -> new TypeInstantiator(fallbackType, Instantiator.allOf(types, serviceType, env.asTypeElement(fallbackType))));

        Optional<TypeWrapper> wrapper = nonNull(annotation::wrapper, Void.class)
                .map(wrapperType -> new TypeWrapper(wrapperType, Wrapper.allOf(types, serviceType, env.asTypeElement(wrapperType))));

        Optional<TypeInstantiator> preprocessor = nonNull(annotation::preprocessor, ServiceDefinition.NoProcessing.class)
                .map(preprocessorType -> new TypeInstantiator(preprocessorType, Instantiator.allOf(types, env.asTypeElement(preprocessorType), env.asTypeElement(preprocessorType))));

        Optional<TypeInstantiator> backend = nonNull(annotation::backend, ServiceDefinition.DefaultBackend.class)
                .map(type -> new TypeInstantiator(type, Instantiator.allOf(types, env.asTypeElement(type), env.asTypeElement(type))));

        Optional<TypeInstantiator> cleaner = nonNull(annotation::cleaner, ServiceDefinition.DefaultCleaner.class)
                .map(type -> new TypeInstantiator(type, Instantiator.allOf(types, env.asTypeElement(type), env.asTypeElement(type))));

        return LoadDefinition
                .builder()
                .quantifier(annotation.quantifier())
                .lifecycle(Lifecycle.of(annotation.mutability(), annotation.singleton()))
                .serviceType(ClassName.get(serviceType))
                .fallback(fallback)
                .noFallback(annotation.noFallback())
                .wrapper(wrapper)
                .preprocessor(preprocessor)
                .loaderName(annotation.loaderName())
                .backend(backend)
                .cleaner(cleaner)
                .batch(annotation.batch())
                .batchName(annotation.batchName())
                .build();
    }

    private LoadFilter filterOf(ExecutableElement x) {
        ServiceFilter annotation = x.getAnnotation(ServiceFilter.class);
        return new LoadFilter(x, annotation.negate(), annotation.position(),
                Optional.ofNullable(getServiceTypeOrNull(x))
        );
    }

    private LoadSorter sorterOf(ExecutableElement x) {
        ServiceSorter annotation = x.getAnnotation(ServiceSorter.class);
        return new LoadSorter(x, annotation.reverse(), annotation.position(),
                Optional.ofNullable(getKeyTypeOrNull(x)),
                Optional.ofNullable(getServiceTypeOrNull(x))
        );
    }

    private LoadId idOf(ExecutableElement x) {
        ServiceId annotation = x.getAnnotation(ServiceId.class);
        return new LoadId(x,
                Optional.ofNullable(getServiceTypeOrNull(x)),
                annotation.pattern()
        );
    }

    private LoadSorter.KeyType getKeyTypeOrNull(ExecutableElement x) {
        Types types = env.getTypeUtils();
        if (types.isSameType(x.getReturnType(), doubleType)) {
            return LoadSorter.KeyType.DOUBLE;
        }
        if (types.isSameType(x.getReturnType(), intType)) {
            return LoadSorter.KeyType.INT;
        }
        if (types.isSameType(x.getReturnType(), longType)) {
            return LoadSorter.KeyType.LONG;
        }
        if (types.isAssignable(x.getReturnType(), comparableType)) {
            return LoadSorter.KeyType.COMPARABLE;
        }
        return null;
    }

    private TypeElement getServiceTypeOrNull(ExecutableElement x) {
        return (TypeElement) x.getEnclosingElement();
    }

    private Optional<TypeMirror> nonNull(Supplier<Class<?>> type, Class<?> nullType) {
        TypeMirror nullTypeMirror = env.asTypeElement(nullType).asType();
        return Optional.of(ProcessorUtil.extractResultType(type))
                .filter(typeMirror -> !env.getTypeUtils().isSameType(typeMirror, nullTypeMirror));
    }
}

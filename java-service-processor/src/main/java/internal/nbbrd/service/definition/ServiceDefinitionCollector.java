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
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceFilter;
import nbbrd.service.ServiceSorter;

/**
 *
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
            }
        }
        return result.build();
    }

    private LoadDefinition definitionOf(TypeElement serviceType) {
        ServiceDefinition annotation = serviceType.getAnnotation(ServiceDefinition.class);
        Types types = env.getTypeUtils();

        Optional<TypeInstantiator> fallback = nonNull(annotation::fallback)
                .map(fallbackType -> new TypeInstantiator(fallbackType, Instantiator.allOf(types, serviceType, env.asTypeElement(fallbackType))));

        Optional<TypeWrapper> wrapper = nonNull(annotation::wrapper)
                .map(wrapperType -> new TypeWrapper(wrapperType, Wrapper.allOf(types, serviceType, env.asTypeElement(wrapperType))));

        Optional<TypeInstantiator> preprocessor = nonNull(annotation::preprocessor)
                .map(preprocessorType -> new TypeInstantiator(preprocessorType, Instantiator.allOf(types, env.asTypeElement(preprocessorType), env.asTypeElement(preprocessorType))));

        return LoadDefinition
                .builder()
                .quantifier(annotation.quantifier())
                .lifecycle(Lifecycle.of(annotation.mutability(), annotation.singleton()))
                .serviceType(ClassName.get(serviceType))
                .fallback(fallback)
                .wrapper(wrapper)
                .preprocessor(preprocessor)
                .loaderName(annotation.loaderName())
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
        return x.getModifiers().contains(Modifier.STATIC) ? null : (TypeElement) x.getEnclosingElement();
    }

    private Optional<TypeMirror> nonNull(Supplier<Class<?>> type) {
        return Optional.of(ProcessorUtil.extractResultType(type)).filter(this::isNonNullValue);
    }

    private boolean isNonNullValue(TypeMirror type) {
        switch (type.toString()) {
            case "nbbrd.service.ServiceDefinition.NoProcessing":
            case "java.lang.Void":
                return false;
            default:
                return true;
        }
    }
}

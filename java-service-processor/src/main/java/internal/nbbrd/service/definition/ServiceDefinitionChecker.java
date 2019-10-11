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
import internal.nbbrd.service.ModuleInfoEntries;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import nbbrd.service.Quantifier;
import nbbrd.service.ServiceDefinition;

/**
 *
 * @author Philippe Charles
 */
final class ServiceDefinitionChecker {

    private final ProcessingEnvironment env;
    private final PrimitiveType booleanType;

    public ServiceDefinitionChecker(ProcessingEnvironment env) {
        this.env = env;
        Types types = env.getTypeUtils();
        this.booleanType = types.getPrimitiveType(TypeKind.BOOLEAN);
    }

    public void checkModuleInfo(List<LoadDefinition> definitions) {
        List<String> serviceTypes = definitions.stream()
                .map(definition -> definition.getServiceType().toString())
                .collect(Collectors.toList());

        try {
            Optional<ModuleInfoEntries> entries = ModuleInfoEntries.parse(env.getFiler());

            entries.map(ModuleInfoEntries::getUsages).ifPresent(usages -> {
                serviceTypes
                        .stream()
                        .filter(serviceType -> (!usages.contains(serviceType)))
                        .map(this::asTypeElement)
                        .forEachOrdered(ref -> error(ref, "Missing module-info.java 'uses' directive for '" + ref + "'"));

//                uses
//                        .stream()
//                        .filter(use -> (!serviceTypes.contains(use)))
//                        .map(this::asTypeElement)
//                        .forEachOrdered(ref -> error(ref, "Missing annotation for '" + ref + "'"));
            });

        } catch (IOException ex) {
            String msg = ex.getClass().getSimpleName() + ": " + ex.getMessage();
            env.getMessager().printMessage(Diagnostic.Kind.ERROR, msg);
        }
    }

    public boolean checkFilter(LoadFilter filter) {
        ExecutableElement x = filter.getTarget();
        if (x.getModifiers().contains(Modifier.STATIC)) {
            error(x, "Filter method does not apply to static methods");
            return false;
        }
        if (!filter.getServiceType().isPresent() || filter.getServiceType().get().getAnnotation(ServiceDefinition.class) == null) {
            error(x, "Filter method only applies to methods of a service");
            return false;
        }
        if (!x.getParameters().isEmpty()) {
            error(x, "Filter method must have no-args");
            return false;
        }
        if (!x.getReturnType().equals(booleanType)) {
            error(x, "Filter method must return boolean");
            return false;
        }
        return true;
    }

    public boolean checkSorter(LoadSorter sorter) {
        ExecutableElement x = sorter.getTarget();
        if (x.getModifiers().contains(Modifier.STATIC)) {
            error(x, "Sorter method does not apply to static methods");
            return false;
        }
        if (!sorter.getServiceType().isPresent() || sorter.getServiceType().get().getAnnotation(ServiceDefinition.class) == null) {
            error(x, "Sorter method only applies to methods of a service");
            return false;
        }
        if (!x.getParameters().isEmpty()) {
            error(x, "Sorter method must have no-args");
            return false;
        }
        if (!sorter.getKeyType().isPresent()) {
            error(x, "Sorter method must return double, int, long or comparable");
            return false;
        }
        return true;
    }

    public boolean checkConstraints(LoadDefinition definition) {
        Types types = env.getTypeUtils();
        TypeElement service = asTypeElement(definition.getServiceType());

        if (!checkFallback(definition.getQuantifier(), definition.getFallback(), service, types)) {
            return false;
        }
        if (!checkPreprocessor(definition.getPreprocessor(), service, types)) {
            return false;
        }
        if (!checkMutability(definition, service, types)) {
            return false;
        }
        return true;
    }

    private boolean checkFallback(Quantifier quantifier, Optional<TypeHandler> fallback, TypeElement service, Types types) {
        switch (quantifier) {
            case SINGLE:
                if (!fallback.isPresent()) {
                    warn(service, String.format("Missing fallback for service '%1$s'", service));
                }
                break;
            case MULTIPLE:
            case OPTIONAL:
                if (fallback.isPresent()) {
                    warn(service, String.format("Useless fallback for service '%1$s'", service));
                }
                break;
        }

        if (fallback.isPresent()) {
            if (!checkFallbackTypeHandler(fallback.get(), service, types)) {
                return false;
            }
        }

        return true;
    }

    private boolean checkFallbackTypeHandler(TypeHandler handler, TypeElement service, Types types) {
        if (!types.isAssignable(handler.getType(), types.erasure(service.asType()))) {
            error(service, String.format("Fallback '%1$s' doesn't extend nor implement service '%2$s'", handler.getType(), service));
            return false;
        }

        if (!checkInstanceFactories(service, handler.getType(), handler)) {
            return false;
        }
        return true;
    }

    private boolean checkPreprocessor(Optional<TypeHandler> preprocessor, TypeElement service, Types types) {
        if (preprocessor.isPresent()) {
            if (!checkPreprocessorTypeHandler(preprocessor.get(), service, types)) {
                return false;
            }
        }
        return true;
    }

    private boolean checkPreprocessorTypeHandler(TypeHandler handler, TypeElement service, Types types) {
        DeclaredType streamOf = types.getDeclaredType(asTypeElement(Stream.class), service.asType());
        DeclaredType unaryOperatorOf = types.getDeclaredType(asTypeElement(UnaryOperator.class), streamOf);
        if (!types.isAssignable(handler.getType(), unaryOperatorOf)) {
            error(service, String.format("Preprocessor '%1$s' doesn't extend nor implement 'UnaryOperator<Stream<%2$s>>'", handler.getType(), service));
            return false;
        }

        if (!checkInstanceFactories(service, handler.getType(), handler)) {
            return false;
        }
        return true;
    }

    private boolean checkMutability(LoadDefinition definition, TypeElement service, Types types) {
        if (definition.getLifecycle() == Lifecycle.UNSAFE_MUTABLE) {
            warn(service, String.format("Thread-unsafe singleton for '%1$s'", service));
        }
        return true;
    }

    private boolean checkInstanceFactories(TypeElement annotatedElement, TypeMirror type, TypeHandler instance) {
        if (!instance.select().isPresent()) {
            error(annotatedElement, String.format("Don't know how to create '%1$s'", type));
            return false;
        }
        return true;
    }

    private TypeElement asTypeElement(String o) {
        return env.getElementUtils().getTypeElement(o);
    }

    private TypeElement asTypeElement(Class o) {
        return env.getElementUtils().getTypeElement(o.getName());
    }

    private TypeElement asTypeElement(ClassName o) {
        return env.getElementUtils().getTypeElement(o.toString());
    }

    private TypeElement asTypeElement(TypeMirror o) {
        return (TypeElement) env.getTypeUtils().asElement(o);
    }

    private void error(Element annotatedElement, String message) {
        env.getMessager().printMessage(Diagnostic.Kind.ERROR, message, annotatedElement);
    }

    private void warn(Element annotatedElement, String message) {
        env.getMessager().printMessage(Diagnostic.Kind.WARNING, message, annotatedElement);
    }
}

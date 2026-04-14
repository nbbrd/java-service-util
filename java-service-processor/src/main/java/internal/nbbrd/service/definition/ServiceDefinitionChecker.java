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
import internal.nbbrd.service.ModuleInfoEntries;
import nbbrd.service.Quantifier;
import nbbrd.service.ServiceDefinition;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static javax.lang.model.element.Modifier.*;

/**
 * @author Philippe Charles
 */
@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "RedundantIfStatement"})
final class ServiceDefinitionChecker {

    private final ExtEnvironment env;
    private final PrimitiveType booleanType;
    private final TypeMirror runtimeExceptionType;

    public ServiceDefinitionChecker(ProcessingEnvironment env) {
        this.env = new ExtEnvironment(env);
        this.booleanType = env.getTypeUtils().getPrimitiveType(TypeKind.BOOLEAN);
        this.runtimeExceptionType = this.env.asTypeElement(RuntimeException.class).asType();
    }

    public void checkModuleInfo(List<LoadDefinition> definitions) {
        try {
            ModuleInfoEntries.parse(env.getFiler(), env.getElementUtils())
                    .map(ModuleInfoEntries::getUsages)
                    .ifPresent(usages -> checkModuleInfoUsages(usages, definitions));
        } catch (IOException ex) {
            String msg = ex.getClass().getSimpleName() + ": " + ex.getMessage();
            env.getMessager().printMessage(Diagnostic.Kind.ERROR, msg);
        }
    }

    private void checkModuleInfoUsages(List<String> usages, List<LoadDefinition> definitions) {
        definitions.stream()
                .map(definition -> definition.getServiceType().toString())
                .filter(serviceType -> (!usages.contains(serviceType)))
                .map(env::asTypeElement)
                .forEachOrdered(ref -> env.error(ref, "Missing module-info directive 'uses " + ref + "'"));
    }

    public boolean checkFilter(LoadFilter filter) {
        Types types = env.getTypeUtils();
        ExecutableElement method = filter.getMethod();
        if (!filter.getServiceType().isPresent() || filter.getServiceType().get().getAnnotation(ServiceDefinition.class) == null) {
            env.error(method, "[RULE_F1] Filter method only applies to methods of a service");
            return false;
        }
        if (method.getModifiers().contains(Modifier.STATIC)) {
            env.error(method, "[RULE_F2] Filter method does not apply to static methods");
            return false;
        }
        if (!method.getParameters().isEmpty()) {
            env.error(method, "[RULE_F3] Filter method must have no-args");
            return false;
        }
        if (!types.isSameType(method.getReturnType(), booleanType)) {
            env.error(method, "[RULE_F4] Filter method must return boolean");
            return false;
        }
        if (hasCheckedExceptions(method)) {
            env.error(method, "[RULE_F5] Filter method must not throw checked exceptions");
            return false;
        }
        return true;
    }

    public boolean checkSorter(LoadSorter sorter) {
        ExecutableElement method = sorter.getMethod();
        if (!sorter.getServiceType().isPresent() || sorter.getServiceType().get().getAnnotation(ServiceDefinition.class) == null) {
            env.error(method, "[RULE_S1] Sorter method only applies to methods of a service");
            return false;
        }
        if (method.getModifiers().contains(Modifier.STATIC)) {
            env.error(method, "[RULE_S2] Sorter method does not apply to static methods");
            return false;
        }
        if (!method.getParameters().isEmpty()) {
            env.error(method, "[RULE_S3] Sorter method must have no-args");
            return false;
        }
        if (!sorter.getKeyType().isPresent()) {
            env.error(method, "[RULE_S4] Sorter method must return double, int, long or comparable");
            return false;
        }
        if (hasCheckedExceptions(method)) {
            env.error(method, "[RULE_S5] Sorter method must not throw checked exceptions");
            return false;
        }
        return true;
    }

    public boolean checkId(LoadId id) {
        Types types = env.getTypeUtils();
        ExecutableElement method = id.getMethod();
        if (!id.getServiceType().isPresent() || id.getServiceType().get().getAnnotation(ServiceDefinition.class) == null) {
            env.error(method, "[RULE_I1] Id method only applies to methods of a service");
            return false;
        }
        if (method.getModifiers().contains(Modifier.STATIC)) {
            env.error(method, "[RULE_I2] Id method does not apply to static methods");
            return false;
        }
        if (!method.getParameters().isEmpty()) {
            env.error(method, "[RULE_I3] Id method must have no-args");
            return false;
        }
        if (!types.isSameType(method.getReturnType(), env.asTypeElement(String.class).asType())) {
            env.error(method, "[RULE_I4] Id method must return String");
            return false;
        }
        if (hasCheckedExceptions(method)) {
            env.error(method, "[RULE_I6] Id method must not throw checked exceptions");
            return false;
        }
        if (!isValidPattern(id.getPattern())) {
            env.error(method, "[RULE_I7] Id pattern must be valid");
            return false;
        }
        return true;
    }

    public boolean checkIds(Map<ClassName, List<LoadId>> idsByService) {
        for (Map.Entry<ClassName, List<LoadId>> o : idsByService.entrySet()) {
            if (o.getValue().size() > 1) {
                env.error(o.getValue().get(1).getMethod(), "[RULE_I5] Id method must be unique");
                return false;
            }
        }
        return true;
    }

    public boolean checkDefinition(LoadDefinition definition) {
        Types types = env.getTypeUtils();
        TypeElement service = env.asTypeElement(definition.getServiceType());

        if (!checkFallback(definition.getQuantifier(), definition.getFallback(), isSingleFallbackNotExpected(service), service, types)) {
            return false;
        }
        if (!checkBatch(definition, service)) {
            return false;
        }
        return true;
    }

    private boolean checkFallback(Quantifier quantifier, Optional<TypeInstantiator> fallback, boolean noFallback, TypeElement service, Types types) {
        switch (quantifier) {
            case SINGLE:
                if (!fallback.isPresent() && !noFallback) {
                    env.warn(service, String.format(Locale.ROOT, "Missing fallback for service '%1$s'", service));
                }
                break;
            case MULTIPLE:
            case OPTIONAL:
                if (fallback.isPresent()) {
                    env.warn(service, String.format(Locale.ROOT, "Useless fallback for service '%1$s'", service));
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

    private boolean checkFallbackTypeHandler(TypeInstantiator handler, TypeElement service, Types types) {
        if (!types.isAssignable(handler.getType(), types.erasure(service.asType()))) {
            env.error(service, String.format(Locale.ROOT, "Fallback '%1$s' doesn't extend nor implement service '%2$s'", handler.getType(), service));
            return false;
        }

        if (!checkInstanceFactories(service, handler.getType(), handler)) {
            return false;
        }
        return true;
    }

    private boolean checkBatch(LoadDefinition definition, TypeElement service) {
        if (definition.getBatch().isPresent()) {
            TypeElement x = env.asTypeElement(definition.getBatch().get().getType());
            if (x.getKind() != ElementKind.INTERFACE && !x.getModifiers().contains(ABSTRACT)) {
                env.error(service, "[RULE_B1] Batch type must be an interface or an abstract class");
                return false;
            }
            if (ElementFilter.methodsIn(x.getEnclosedElements()).stream().filter(batchMethodFilter(service)).count() != 1) {
                env.error(service, "[RULE_B2] Batch method must be unique");
                return false;
            }
        }
        return true;
    }

    private Predicate<ExecutableElement> batchMethodFilter(TypeElement service) {
        return method -> method.getModifiers().contains(PUBLIC)
                && !method.getModifiers().contains(STATIC)
                && method.getParameters().isEmpty()
                && isBatchReturnType(method.getReturnType(), service)
                && !hasCheckedExceptions(method);
    }

    private boolean isBatchReturnType(TypeMirror returnType, TypeElement service) {
        return BatchDefinition.MethodReturnKind.resolve(returnType, service, env.getTypeUtils(), env.getElementUtils()) != null;
    }

    private boolean checkInstanceFactories(TypeElement annotatedElement, TypeMirror type, TypeInstantiator instance) {
        if (!instance.select().isPresent()) {
            env.error(annotatedElement, String.format(Locale.ROOT, "Don't know how to instantiate '%1$s'", type));
            return false;
        }
        return true;
    }

    private boolean hasCheckedExceptions(ExecutableElement method) {
        return method.getThrownTypes().stream().anyMatch(type -> !env.getTypeUtils().isAssignable(type, runtimeExceptionType));
    }

    private static boolean isValidPattern(String value) {
        try {
            Pattern.compile(value);
            return true;
        } catch (PatternSyntaxException ex) {
            return false;
        }
    }

    private static boolean isSingleFallbackNotExpected(TypeElement service) {
        SuppressWarnings annotation = service.getAnnotation(SuppressWarnings.class);
        return annotation != null && Arrays.asList(annotation.value()).contains(ServiceDefinition.SINGLE_FALLBACK_NOT_EXPECTED);
    }
}

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

import internal.nbbrd.service.ExtEnvironment;
import internal.nbbrd.service.ModuleInfoEntries;
import nbbrd.service.Quantifier;
import nbbrd.service.ServiceDefinition;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author Philippe Charles
 */
final class ServiceDefinitionChecker {

    private final ExtEnvironment env;
    private final PrimitiveType booleanType;

    public ServiceDefinitionChecker(ProcessingEnvironment env) {
        this.env = new ExtEnvironment(env);
        this.booleanType = env.getTypeUtils().getPrimitiveType(TypeKind.BOOLEAN);
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
        ExecutableElement x = filter.getMethod();
        if (!filter.getServiceType().isPresent() || filter.getServiceType().get().getAnnotation(ServiceDefinition.class) == null) {
            env.error(x, "[RULE_F1] Filter method only applies to methods of a service");
            return false;
        }
        if (x.getModifiers().contains(Modifier.STATIC)) {
            env.error(x, "[RULE_F2] Filter method does not apply to static methods");
            return false;
        }
        if (!x.getParameters().isEmpty()) {
            env.error(x, "[RULE_F3] Filter method must have no-args");
            return false;
        }
        if (!types.isSameType(x.getReturnType(), booleanType)) {
            env.error(x, "[RULE_F4] Filter method must return boolean");
            return false;
        }
        return true;
    }

    public boolean checkSorter(LoadSorter sorter) {
        ExecutableElement x = sorter.getMethod();
        if (!sorter.getServiceType().isPresent() || sorter.getServiceType().get().getAnnotation(ServiceDefinition.class) == null) {
            env.error(x, "[RULE_S1] Sorter method only applies to methods of a service");
            return false;
        }
        if (x.getModifiers().contains(Modifier.STATIC)) {
            env.error(x, "[RULE_S2] Sorter method does not apply to static methods");
            return false;
        }
        if (!x.getParameters().isEmpty()) {
            env.error(x, "[RULE_S3] Sorter method must have no-args");
            return false;
        }
        if (!sorter.getKeyType().isPresent()) {
            env.error(x, "[RULE_S4] Sorter method must return double, int, long or comparable");
            return false;
        }
        return true;
    }

    public boolean checkId(LoadId id) {
        Types types = env.getTypeUtils();
        ExecutableElement x = id.getMethod();
        if (!id.getServiceType().isPresent() || id.getServiceType().get().getAnnotation(ServiceDefinition.class) == null) {
            env.error(x, "[RULE_I1] Id method only applies to methods of a service");
            return false;
        }
        if (x.getModifiers().contains(Modifier.STATIC)) {
            env.error(x, "[RULE_I2] Id method does not apply to static methods");
            return false;
        }
        if (!x.getParameters().isEmpty()) {
            env.error(x, "[RULE_I3] Id method must have no-args");
            return false;
        }
        if (!types.isSameType(x.getReturnType(), env.asTypeElement(String.class).asType())) {
            env.error(x, "[RULE_I4] Id method must return String");
            return false;
        }
        if (!isValidPattern(id.getPattern())) {
            env.error(x, "[RULE_I5] Id pattern must be valid");
            return false;
        }
        return true;
    }

    public boolean checkDefinition(LoadDefinition definition) {
        Types types = env.getTypeUtils();
        TypeElement service = env.asTypeElement(definition.getServiceType());

        if (!checkFallback(definition.getQuantifier(), definition.getFallback(), definition.isNoFallback(), service, types)) {
            return false;
        }
        if (!checkWrapper(definition.getWrapper(), service, types)) {
            return false;
        }
        if (!checkPreprocessor(definition.getPreprocessor(), service, types)) {
            return false;
        }
        if (!checkBackend(definition.getBackend(), service, types)) {
            return false;
        }
        if (!checkCleaner(definition.getCleaner(), service, types)) {
            return false;
        }
        if (!checkMutability(definition, service, types)) {
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

    private boolean checkWrapper(Optional<TypeWrapper> wrapper, TypeElement service, Types types) {
        if (wrapper.isPresent()) {
            if (!checkWrapperTypeHandler(wrapper.get(), service, types)) {
                return false;
            }
        }
        return true;
    }

    private boolean checkWrapperTypeHandler(TypeWrapper handler, TypeElement service, Types types) {
        if (!types.isAssignable(handler.getType(), types.erasure(service.asType()))) {
            env.error(service, String.format(Locale.ROOT, "Wrapper '%1$s' doesn't extend nor implement service '%2$s'", handler.getType(), service));
            return false;
        }

        if (!checkWrapperFactories(service, handler.getType(), handler)) {
            return false;
        }
        return true;
    }

    private boolean checkPreprocessor(Optional<TypeInstantiator> preprocessor, TypeElement service, Types types) {
        if (preprocessor.isPresent()) {
            if (!checkPreprocessorTypeHandler(preprocessor.get(), service, types)) {
                return false;
            }
        }
        return true;
    }

    private boolean checkPreprocessorTypeHandler(TypeInstantiator handler, TypeElement service, Types types) {
        TypeMirror expectedType = LoadDefinition.getPreprocessorType(env, service.asType());
        if (!types.isAssignable(handler.getType(), expectedType)) {
            env.error(service, String.format(Locale.ROOT, "Preprocessor '%1$s' doesn't extend nor implement '%2$s'", handler.getType(), expectedType));
            return false;
        }

        if (!checkInstanceFactories(service, handler.getType(), handler)) {
            return false;
        }
        return true;
    }

    private boolean checkBackend(Optional<TypeInstantiator> backend, TypeElement service, Types types) {
        if (backend.isPresent()) {
            if (!checkBackendTypeHandler(backend.get(), service, types)) {
                return false;
            }
        }
        return true;
    }

    private boolean checkBackendTypeHandler(TypeInstantiator handler, TypeElement service, Types types) {
        TypeMirror expectedType = LoadDefinition.getBackendType(env, service.asType());
        if (!types.isAssignable(handler.getType(), expectedType)) {
            env.error(service, String.format(Locale.ROOT, "Backend '%1$s' doesn't extend nor implement '%2$s'", handler.getType(), expectedType));
            return false;
        }

        if (!checkInstanceFactories(service, handler.getType(), handler)) {
            return false;
        }
        return true;
    }

    private boolean checkCleaner(Optional<TypeInstantiator> cleaner, TypeElement service, Types types) {
        if (cleaner.isPresent()) {
            if (!checkCleanerTypeHandler(cleaner.get(), service, types)) {
                return false;
            }
        }
        return true;
    }

    private boolean checkCleanerTypeHandler(TypeInstantiator handler, TypeElement service, Types types) {
        TypeMirror expectedType = LoadDefinition.getCleanerType(env, service.asType());
        if (!types.isAssignable(handler.getType(), expectedType)) {
            env.error(service, String.format(Locale.ROOT, "Cleaner '%1$s' doesn't extend nor implement '%2$s'", handler.getType(), expectedType));
            return false;
        }

        if (!checkInstanceFactories(service, handler.getType(), handler)) {
            return false;
        }
        return true;
    }

    private boolean checkMutability(LoadDefinition definition, TypeElement service, Types types) {
        if (definition.getLifecycle() == Lifecycle.UNSAFE_MUTABLE) {
            env.warn(service, String.format(Locale.ROOT, "Thread-unsafe singleton for '%1$s'", service));
        }
        return true;
    }

    private boolean checkInstanceFactories(TypeElement annotatedElement, TypeMirror type, TypeInstantiator instance) {
        if (!instance.select().isPresent()) {
            env.error(annotatedElement, String.format(Locale.ROOT, "Don't know how to instantiate '%1$s'", type));
            return false;
        }
        return true;
    }

    private boolean checkWrapperFactories(TypeElement annotatedElement, TypeMirror type, TypeWrapper instance) {
        if (!instance.select().isPresent()) {
            env.error(annotatedElement, String.format(Locale.ROOT, "Don't know how to wrap '%1$s'", type));
            return false;
        }
        return true;
    }

    private static boolean isValidPattern(String value) {
        try {
            Pattern.compile(value);
            return true;
        } catch (PatternSyntaxException ex) {
            return false;
        }
    }
}

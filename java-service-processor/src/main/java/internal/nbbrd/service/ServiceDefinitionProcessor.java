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
package internal.nbbrd.service;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import nbbrd.service.ServiceDefinition;

/**
 *
 * @author Philippe Charles
 */
@org.openide.util.lookup.ServiceProvider(service = Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({"nbbrd.service.ServiceDefinition"})
public final class ServiceDefinitionProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
//        if (roundEnv.processingOver()) {
//            return false;
//        }

        annotations.stream()
                .map(roundEnv::getElementsAnnotatedWith)
                .flatMap(Set::stream)
                .map(TypeElement.class::cast)
                .map(this::getGenerator)
                .collect(Collectors.groupingBy(o -> resolveLoaderName(o).topLevelClassName()))
                .forEach(this::process);

        return true;
    }

    private ServiceLoaderGenerator getGenerator(TypeElement serviceType) {
        ServiceDefinition definition = serviceType.getAnnotation(ServiceDefinition.class);
        return ServiceLoaderGenerator.of(definition, ClassName.get(serviceType));
    }

    private void process(ClassName top, List<ServiceLoaderGenerator> generators) {
        JavaFile file;
        if (generators.size() == 1 && top.equals(resolveLoaderName(generators.get(0)))) {
            ServiceLoaderGenerator generator = generators.get(0);
            file = JavaFile.builder(top.packageName(), generator.generate(top.simpleName())).build();
        } else {
            TypeSpec.Builder nestedTypes = TypeSpec.classBuilder(top)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
            generators
                    .stream()
                    .map(generator -> generator.generate(resolveLoaderName(generator).simpleName()))
                    .map(o -> o.toBuilder().addModifiers(Modifier.STATIC).build())
                    .forEach(nestedTypes::addType);
            file = JavaFile.builder(top.packageName(), nestedTypes.build()).build();
        }
        ProcessorUtil.write(processingEnv, file);
    }

    static ClassName resolveLoaderName(ServiceLoaderGenerator generator) {
        return resolveLoaderName(generator.getLoaderName(), generator.getServiceType());
    }

    static ClassName resolveLoaderName(String loaderName, ClassName serviceType) {
        if (!loaderName.isEmpty()) {
            return ClassName.bestGuess(loaderName);
        }
        ClassName top = serviceType.topLevelClassName();
        ClassName topLoader = ClassName.get(top.packageName(), top.simpleName() + "Loader");
        if (top.equals(serviceType)) {
            return topLoader;
        }
        return topLoader.nestedClass(serviceType.simpleName());
    }
}

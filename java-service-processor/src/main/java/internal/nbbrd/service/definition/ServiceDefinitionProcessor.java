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
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import internal.nbbrd.service.ProcessorUtil;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

/**
 *
 * @author Philippe Charles
 */
@org.openide.util.lookup.ServiceProvider(service = Processor.class)
@SupportedAnnotationTypes({"nbbrd.service.ServiceDefinition"})
public final class ServiceDefinitionProcessor extends AbstractProcessor {

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
//        if (roundEnv.processingOver()) {
//            return false;
//        }
        ServiceDefinitionCollector collector = new ServiceDefinitionCollector(processingEnv);
        ServiceDefinitionChecker checker = new ServiceDefinitionChecker(processingEnv);

        List<DefinitionValue> allDefinitions = collector.collect(annotations, roundEnv);

        if (!allDefinitions.isEmpty()) {
            checker.checkModuleInfo(allDefinitions);

            allDefinitions
                    .stream()
                    .filter(checker::checkConstraints)
                    .collect(Collectors.groupingBy(ServiceDefinitionProcessor::getTopLevelClassName))
                    .forEach(this::generate);
        }

        return true;
    }

    private void generate(ClassName topLevelClassName, List<DefinitionValue> definitions) {
        TypeSpec typeSpec = isNotNested(topLevelClassName, definitions)
                ? generate(definitions.get(0))
                : generateNested(topLevelClassName, definitions);
        ProcessorUtil.write(processingEnv, JavaFile.builder(topLevelClassName.packageName(), typeSpec).build());
    }

    private boolean isNotNested(ClassName topLevelClassName, List<DefinitionValue> definitions) {
        return definitions.size() == 1 && topLevelClassName.equals(definitions.get(0).resolveLoaderName());
    }

    private TypeSpec generate(DefinitionValue definition) {
        return new ServiceDefinitionGenerator(definition).generate(false);
    }

    private TypeSpec generateNested(ClassName topLevelClassName, List<DefinitionValue> definitions) {
        return TypeSpec.classBuilder(topLevelClassName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addTypes(definitions.stream().map(o -> new ServiceDefinitionGenerator(o).generate(true)).collect(Collectors.toList()))
                .build();
    }

    private static ClassName getTopLevelClassName(DefinitionValue definition) {
        return definition.resolveLoaderName().topLevelClassName();
    }
}

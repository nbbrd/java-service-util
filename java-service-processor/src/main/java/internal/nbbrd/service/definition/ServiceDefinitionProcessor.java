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
import internal.nbbrd.service.Unreachable;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Philippe Charles
 */
@org.openide.util.lookup.ServiceProvider(service = Processor.class)
@SupportedAnnotationTypes({
        "nbbrd.service.ServiceDefinition",
        "nbbrd.service.ServiceFilter",
        "nbbrd.service.ServiceSorter"
})
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

        LoadData data = collector.collect(annotations, roundEnv);

        if (!data.getDefinitions().isEmpty()) {
            checker.checkModuleInfo(data.getDefinitions());
        }

        Map<ClassName, List<LoadDefinition>> definitionsByTopLevel = data.getDefinitions()
                .stream()
                .filter(checker::checkDefinition)
                .collect(Collectors.groupingBy(definition -> definition.getServiceType().topLevelClassName()));

        Map<ClassName, List<LoadFilter>> filtersByService = data.getFilters()
                .stream()
                .filter(checker::checkFilter)
                .collect(Collectors.groupingBy(filter -> filter.getServiceType().map(ClassName::get).orElseThrow(Unreachable::new)));

        Map<ClassName, List<LoadSorter>> sortersByService = data.getSorters()
                .stream()
                .filter(checker::checkSorter)
                .collect(Collectors.groupingBy(sorter -> sorter.getServiceType().map(ClassName::get).orElseThrow(Unreachable::new)));

        definitionsByTopLevel.forEach((topLevel, definitions) -> generate(topLevel, ServiceDefinitionGenerator.allOf(definitions, filtersByService, sortersByService)));

        return true;
    }

    private void generate(ClassName topLevel, List<ServiceDefinitionGenerator> generators) {
        if (isNotNested(topLevel, generators)) {
            generateNotNested(generators.get(0));
        } else {
            generateNested(topLevel, generators);
        }
    }

    private boolean isNotNested(ClassName topLevel, List<ServiceDefinitionGenerator> generators) {
        return generators.size() == 1 && topLevel.equals(generators.get(0).getDefinition().getServiceType());
    }

    private void generateNotNested(ServiceDefinitionGenerator generator) {
        String loaderPackage = generator.getDefinition().resolveLoaderName().packageName();
        TypeSpec loaderClass = generator.generateLoader(false);
        writeFile(loaderPackage, loaderClass);

        String batchPackage = generator.getDefinition().resolveBatchName().packageName();
        Optional<TypeSpec> batchClass = generator.generateBatch(false);
        batchClass.ifPresent(batchSpec -> writeFile(batchPackage, batchSpec));
    }

    private void generateNested(ClassName topLevel, List<ServiceDefinitionGenerator> generators) {
        List<TypeSpec> nestedLoaders = generators.stream().map(o -> o.generateLoader(true)).collect(Collectors.toList());
        TypeSpec loaderClass = TypeSpec.classBuilder(ClassName.bestGuess(topLevel.canonicalName() + "Loader"))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addTypes(nestedLoaders)
                .build();
        writeFile(topLevel.packageName(), loaderClass);

        List<TypeSpec> nestedBatches = generators.stream().map(o -> o.generateBatch(true)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
        if (!nestedBatches.isEmpty()) {
            TypeSpec batchClass = TypeSpec.classBuilder(ClassName.bestGuess(topLevel.canonicalName() + "Batch"))
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addTypes(nestedBatches)
                    .build();
            writeFile(topLevel.packageName(), batchClass);
        }
    }

    private void writeFile(String loaderPackage, TypeSpec loaderClass) {
        ProcessorUtil.write(processingEnv, JavaFile.builder(loaderPackage, loaderClass).build());
    }
}

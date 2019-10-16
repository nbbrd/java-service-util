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
import java.util.List;
import java.util.Map;
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
                .filter(checker::checkConstraints)
                .collect(Collectors.groupingBy(definition -> definition.resolveLoaderName().topLevelClassName()));

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
        TypeSpec typeSpec = isNotNested(topLevel, generators)
                ? generators.get(0).generate(false)
                : generateNested(topLevel, generators);
        ProcessorUtil.write(processingEnv, JavaFile.builder(topLevel.packageName(), typeSpec).build());
    }

    private boolean isNotNested(ClassName topLevel, List<ServiceDefinitionGenerator> generators) {
        return generators.size() == 1 && topLevel.equals(generators.get(0).getDefinition().resolveLoaderName());
    }

    private TypeSpec generateNested(ClassName topLevel, List<ServiceDefinitionGenerator> generators) {
        return TypeSpec.classBuilder(topLevel)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addTypes(generators.stream().map(o -> o.generate(true)).collect(Collectors.toList()))
                .build();
    }
}

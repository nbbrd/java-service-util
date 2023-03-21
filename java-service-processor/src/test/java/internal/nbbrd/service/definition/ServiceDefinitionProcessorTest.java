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

import com.google.common.truth.StringSubject;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import internal.nbbrd.service.provider.ServiceProviderProcessor;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.assertj.core.api.HamcrestCondition;
import org.assertj.core.api.ListAssert;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.Processor;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;
import static javax.tools.StandardLocation.SOURCE_OUTPUT;
import static org.assertj.core.condition.MappedCondition.mappedCondition;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * @author Philippe Charles
 */
public class ServiceDefinitionProcessorTest {

    @Test
    public void testRegistration() {
        Assertions.assertThat(ServiceLoader.load(Processor.class))
                .hasAtLeastOneElementOfType(ServiceDefinitionProcessor.class);
    }

    @Test
    public void testNonNestedDef() {
        JavaFileObject file = JavaFileObjects.forResource("definition/NonNestedDef.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .succeededWithoutWarnings();

        assertThat(compilation)
                .generatedSourceFile("definition.NonNestedDefLoader")
                .contentsAsUtf8String()
                .contains("private final Iterable<NonNestedDef> source = ServiceLoader.load(NonNestedDef.class);");
    }

    @Test
    public void testNestedDef() {
        JavaFileObject file = JavaFileObjects.forResource("definition/NestedDef.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .succeededWithoutWarnings();

        assertThat(compilation)
                .generatedSourceFile("definition.NestedDefLoader")
                .contentsAsUtf8String()
                .contains("public static final class HelloService {");
    }

    @Test
    public void testSingleDef() {
        JavaFileObject file = JavaFileObjects.forResource("definition/SingleDef.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .succeeded();

        assertThat(compilation)
                .hadWarningContaining("Thread-unsafe singleton for 'definition.SingleDef.MutableSingleton'");

        StringSubject result = assertThat(compilation)
                .generatedSourceFile("definition.SingleDefLoader")
                .contentsAsUtf8String();

        result.contains("private final SingleDef.Immutable resource = doLoad();");

        result.contains("public static SingleDef.Immutable load()");

        result.contains("private SingleDef.Mutable resource = doLoad();");

        result.contains("private final AtomicReference<SingleDef.ThreadSafe> resource = new AtomicReference<>(doLoad());");

        result.contains("private static final SingleDef.ImmutableSingleton RESOURCE = doLoad();");

        result.contains("private static SingleDef.MutableSingleton RESOURCE = doLoad();");

        result.contains("private static final AtomicReference<SingleDef.ThreadSafeSingleton> RESOURCE = new AtomicReference<>(doLoad());");
    }

    @Test
    public void testOptionalDef() {
        JavaFileObject file = JavaFileObjects.forResource("definition/OptionalDef.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .succeeded();

        assertThat(compilation)
                .hadWarningContaining("Thread-unsafe singleton for 'definition.OptionalDef.MutableSingleton'");

        StringSubject result = assertThat(compilation)
                .generatedSourceFile("definition.OptionalDefLoader")
                .contentsAsUtf8String();

        result.contains("private final Optional<OptionalDef.Immutable> resource = doLoad();");

        result.contains("public static Optional<OptionalDef.Immutable> load()");

        result.contains("private Optional<OptionalDef.Mutable> resource = doLoad();");

        result.contains("private final AtomicReference<Optional<OptionalDef.ThreadSafe>> resource = new AtomicReference<>(doLoad());");

        result.contains("private static final Optional<OptionalDef.ImmutableSingleton> RESOURCE = doLoad();");

        result.contains("private static Optional<OptionalDef.MutableSingleton> RESOURCE = doLoad();");

        result.contains("private static final AtomicReference<Optional<OptionalDef.ThreadSafeSingleton>> RESOURCE = new AtomicReference<>(doLoad());");
    }

    @Test
    public void testMultipleDef() {
        JavaFileObject file = JavaFileObjects.forResource("definition/MultipleDef.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .succeeded();

        assertThat(compilation)
                .hadWarningContaining("Thread-unsafe singleton for 'definition.MultipleDef.MutableSingleton'");

        StringSubject result = assertThat(compilation)
                .generatedSourceFile("definition.MultipleDefLoader")
                .contentsAsUtf8String();

        result.contains("private final List<MultipleDef.Immutable> resource = doLoad();");

        result.contains("public static List<MultipleDef.Immutable> load()");

        result.contains("private List<MultipleDef.Mutable> resource = doLoad();");

        result.contains("private final AtomicReference<List<MultipleDef.ThreadSafe>> resource = new AtomicReference<>(doLoad());");

        result.contains("private static final List<MultipleDef.ImmutableSingleton> RESOURCE = doLoad();");

        result.contains("private static List<MultipleDef.MutableSingleton> RESOURCE = doLoad();");

        result.contains("private static final AtomicReference<List<MultipleDef.ThreadSafeSingleton>> RESOURCE = new AtomicReference<>(doLoad());");
    }

    @Test
    public void testAlternateFactories() {
        JavaFileObject file = JavaFileObjects.forResource("definition/AlternateFactories.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .succeededWithoutWarnings();
    }

    @Test
    public void testNonInstantiableFallback() {
        JavaFileObject file = JavaFileObjects.forResource("definition/NonInstantiableFallback.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .failed();

        assertThat(compilation)
                .hadErrorContaining("Don't know how to instantiate")
                .inFile(file)
                .onLine(9);
    }

    @Test
    public void testNonInstantiableWrapper() {
        JavaFileObject file = JavaFileObjects.forResource("definition/NonInstantiableWrapper.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .failed();

        assertThat(compilation)
                .hadErrorContaining("Don't know how to wrap 'definition.NonInstantiableWrapper.WrapperByPrivateConstructor'")
                .inFile(file)
                .onLine(8);

        assertThat(compilation)
                .hadErrorContaining("Don't know how to wrap 'definition.NonInstantiableWrapper.WrapperByNoArgConstructor'")
                .inFile(file)
                .onLine(18);

        assertThat(compilation)
                .hadErrorContaining("Don't know how to wrap 'definition.NonInstantiableWrapper.WrapperByInvalidArgConstructor'")
                .inFile(file)
                .onLine(28);

        assertThat(compilation)
                .hadErrorContaining("Don't know how to wrap 'definition.NonInstantiableWrapper.WrapperByToManyStaticMethods'")
                .inFile(file)
                .onLine(38);
    }

    @Test
    public void testNonInstantiablePreprocessor() {
        JavaFileObject file = JavaFileObjects.forResource("definition/NonInstantiablePreprocessor.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .failed();

        assertThat(compilation)
                .hadErrorContaining("Don't know how to instantiate")
                .inFile(file)
                .onLine(11);
    }

    @Test
    public void testNonAssignableFallback() {
        JavaFileObject file = JavaFileObjects.forResource("definition/NonAssignableFallback.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .failed();

        assertThat(compilation)
                .hadErrorContaining("doesn't extend nor implement service")
                .inFile(file)
                .onLine(8);
    }

    @Test
    public void testNonAssignablePreprocessor() {
        JavaFileObject file = JavaFileObjects.forResource("definition/NonAssignablePreprocessor.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .failed();

        assertThat(compilation)
                .hadErrorContaining("doesn't extend nor implement")
                .inFile(file)
                .onLine(10);
    }

    @Test
    public void testNonAssignableWrapper() {
        JavaFileObject file = JavaFileObjects.forResource("definition/NonAssignableWrapper.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .failed();

        assertThat(compilation)
                .hadErrorContaining("doesn't extend nor implement service")
                .inFile(file)
                .onLine(8);
    }

    @Test
    public void testFilters() {
        JavaFileObject file = JavaFileObjects.forResource("definition/Filters.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .succeededWithoutWarnings();

        StringSubject result = assertThat(compilation)
                .generatedSourceFile("definition.FiltersLoader")
                .contentsAsUtf8String();

        result.contains("private final Optional<Filters.SingleFilter> resource = doLoad();");
        result.contains(".filter(Filters.SingleFilter::isAvailable)");

        result.contains("private final Optional<Filters.MultiFilter> resource = doLoad();");
        result.contains(".filter(((Predicate<Filters.MultiFilter>)Filters.MultiFilter::isAvailable).and(Filters.MultiFilter::isFastEnough))");

        result.contains("private final Optional<Filters.ReversedFilter> resource = doLoad();");
        result.contains(".filter(((Predicate<Filters.ReversedFilter>)Filters.ReversedFilter::isAvailable).negate())");

        result.contains("private final Optional<Filters.MultiFilterWithPosition> resource = doLoad();");
        result.contains(".filter(((Predicate<Filters.MultiFilterWithPosition>)Filters.MultiFilterWithPosition::isFastEnough).and(Filters.MultiFilterWithPosition::isAvailable))");
    }

    @Test
    public void testNoArgFilter() {
        JavaFileObject file = JavaFileObjects.forResource("definition/NoArgFilter.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .failed();

        assertThat(compilation)
                .hadErrorContaining("Filter method must have no-args");
    }

    @Test
    public void testStaticFilter() {
        JavaFileObject file = JavaFileObjects.forResource("definition/StaticFilter.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .failed();

        assertThat(compilation)
                .hadErrorContaining("Filter method does not apply to static methods")
                .inFile(file)
                .onLine(10);
    }

    @Test
    public void testLostFilter() {
        JavaFileObject file = JavaFileObjects.forResource("definition/LostFilter.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .failed();

        assertThat(compilation)
                .hadErrorContaining("Filter method only applies to methods of a service")
                .inFile(file)
                .onLine(8);
    }

    @Test
    public void testNonBooleanFilter() {
        JavaFileObject file = JavaFileObjects.forResource("definition/NonBooleanFilter.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .failed();

        assertThat(compilation)
                .hadErrorContaining("Filter method must return boolean")
                .inFile(file)
                .onLine(10);
    }

    @Test
    public void testWrappers() {
        JavaFileObject file = JavaFileObjects.forResource("definition/Wrappers.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .succeededWithoutWarnings();

        StringSubject result = assertThat(compilation)
                .generatedSourceFile("definition.WrappersLoader")
                .contentsAsUtf8String();

        result.contains("private final Optional<Wrappers.ByConstructor> resource = doLoad();");
        result.contains(".map(Wrappers.WrapperByConstructor::new)");

        result.contains("private final Optional<Wrappers.ByStaticMethod> resource = doLoad();");
        result.contains(".map(Wrappers.WrapperByStaticMethod::wrap)");

        result.contains("private final Optional<Wrappers.ByStaticMethodX> resource = doLoad();");
        result.contains(".map(Wrappers.WrapperByStaticMethodX::wrap)");
    }

    @Test
    public void testSorters() {
        JavaFileObject file = JavaFileObjects.forResource("definition/Sorters.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .succeededWithoutWarnings();

        StringSubject result = assertThat(compilation)
                .generatedSourceFile("definition.SortersLoader")
                .contentsAsUtf8String();

        result.contains("private final Optional<Sorters.IntSorter> resource = doLoad();");
        result.contains(".sorted(Comparator.comparingInt(Sorters.IntSorter::getCost))");

        result.contains("private final Optional<Sorters.LongSorter> resource = doLoad();");
        result.contains(".sorted(Comparator.comparingLong(Sorters.LongSorter::getCost))");

        result.contains("private final Optional<Sorters.DoubleSorter> resource = doLoad();");
        result.contains(".sorted(Comparator.comparingDouble(Sorters.DoubleSorter::getCost))");

        result.contains("private final Optional<Sorters.ComparableSorter> resource = doLoad();");
        result.contains(".sorted(Comparator.comparing(Sorters.ComparableSorter::getCost))");

        result.contains("private final Optional<Sorters.MultiSorter> resource = doLoad();");
        result.contains(".sorted(((Comparator<Sorters.MultiSorter>)Comparator.comparingInt(Sorters.MultiSorter::getCost)).thenComparing(Comparator.comparingDouble(Sorters.MultiSorter::getAccuracy)))");

        result.contains("private final Optional<Sorters.ReversedSorter> resource = doLoad();");
        result.contains(".sorted(Collections.reverseOrder(Comparator.comparingInt(Sorters.ReversedSorter::getCost)))");

        result.contains("private final Optional<Sorters.MultiSorterWithPosition> resource = doLoad();");
        result.contains(".sorted(((Comparator<Sorters.MultiSorterWithPosition>)Comparator.comparingDouble(Sorters.MultiSorterWithPosition::getAccuracy)).thenComparing(Comparator.comparingInt(Sorters.MultiSorterWithPosition::getCost)))");
    }

    @Test
    public void testNoArgSorter() {
        JavaFileObject file = JavaFileObjects.forResource("definition/NoArgSorter.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .failed();

        assertThat(compilation)
                .hadErrorContaining("Sorter method must have no-args");
    }

    @Test
    public void testStaticSorter() {
        JavaFileObject file = JavaFileObjects.forResource("definition/StaticSorter.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .failed();

        assertThat(compilation)
                .hadErrorContaining("Sorter method does not apply to static methods")
                .inFile(file)
                .onLine(10);
    }

    @Test
    public void testLostSorter() {
        JavaFileObject file = JavaFileObjects.forResource("definition/LostSorter.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .failed();

        assertThat(compilation)
                .hadErrorContaining("Sorter method only applies to methods of a service")
                .inFile(file)
                .onLine(8);
    }

    @Test
    public void testNonComparableSorter() {
        JavaFileObject file = JavaFileObjects.forResource("definition/NonComparableSorter.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .failed();

        assertThat(compilation)
                .hadErrorContaining("Sorter method must return double, int, long or comparable")
                .inFile(file)
                .onLine(10);
    }

    @Test
    public void testCustomBackend() {
        JavaFileObject file = JavaFileObjects.forResource("definition/CustomBackend.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .succeededWithoutWarnings();

        assertThat(compilation)
                .generatedSourceFile("definition.CustomBackendLoader")
                .contentsAsUtf8String()
                .contains("private final Iterable<CustomBackend> source = CustomBackend.NetBeansLookup.INSTANCE.apply(CustomBackend.class);");
    }

    @Test
    public void testNonAssignableBackend() {
        JavaFileObject file = JavaFileObjects.forResource("definition/NonAssignableBackend.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .failed();

        assertThat(compilation)
                .hadErrorContaining("doesn't extend nor implement")
                .inFile(file)
                .onLine(11);
    }

    @Test
    public void testNonNestedBatch() {
        JavaFileObject file = JavaFileObjects.forResource("definition/NonNestedBatch.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .succeededWithoutWarnings();

        StringSubject loader = assertThat(compilation)
                .generatedSourceFile("definition.NonNestedBatchLoader")
                .contentsAsUtf8String();

        loader.contains("private final Iterable<NonNestedBatch> source = ServiceLoader.load(NonNestedBatch.class);");
        loader.contains("private final Iterable<NonNestedBatchBatch> batch = ServiceLoader.load(NonNestedBatchBatch.class);");

        assertThat(compilation)
                .generatedSourceFile("definition.NonNestedBatchBatch")
                .contentsAsUtf8String()
                .contains("Stream<NonNestedBatch> getProviders();");

        StringSubject content
                = assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "META-INF/services/definition.NonNestedBatchBatch")
                .contentsAsUtf8String();
        content.contains("definition.NonNestedBatch$ABC");
    }

    @Test
    public void testNestedBatch() {
        JavaFileObject file = JavaFileObjects.forResource("definition/NestedBatch.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .succeededWithoutWarnings();

        StringSubject loader = assertThat(compilation)
                .generatedSourceFile("definition.NestedBatchLoader")
                .contentsAsUtf8String();

        loader.contains("private final Iterable<NestedBatch.HelloService> source = ServiceLoader.load(NestedBatch.HelloService.class);");
        loader.contains("private final Iterable<NestedBatchBatch.HelloService> batch = ServiceLoader.load(NestedBatchBatch.HelloService.class);");

        assertThat(compilation)
                .generatedSourceFile("definition.NestedBatchBatch")
                .contentsAsUtf8String()
                .contains("Stream<NestedBatch.HelloService> getProviders();");

        StringSubject content
                = assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "META-INF/services/definition.NestedBatchBatch$HelloService")
                .contentsAsUtf8String();
        content.contains("definition.NestedBatch$ABC");
    }

    @Test
    public void testAlternateNames() {
        JavaFileObject file = JavaFileObjects.forResource("definition/AlternateNames.java");

        assertAbout(javaSource())
                .that(file)
                .processedWith(new ServiceDefinitionProcessor())
                .compilesWithoutWarnings()
                .and().generatesFileNamed(SOURCE_OUTPUT, "definition", "AlternateNamesLoader.java")
                .and().generatesFileNamed(SOURCE_OUTPUT, "definition", "AlternateNamesBatch.java")
                .and().generatesFileNamed(SOURCE_OUTPUT, "internal", "FooLoader.java")
                .and().generatesFileNamed(SOURCE_OUTPUT, "internal", "BarBatch.java")
        ;

        assertGeneratedSources(file)
                .hasSize(4);

        assertGeneratedSources(nested("@ServiceDefinition ( )"))
                .hasSize(1)
                .areAtLeastOne(named("definition", "NestedLoader.java"));

        assertGeneratedSources(nested("@ServiceDefinition ( loaderName = \"internal.LOADER\" )"))
                .hasSize(1)
                .areAtLeastOne(named("internal", "LOADER.java"));

        assertGeneratedSources(nested("@ServiceDefinition ( batch = true )"))
                .hasSize(2)
                .areAtLeastOne(named("definition", "NestedLoader.java"))
                .areAtLeastOne(named("definition", "NestedBatch.java"));

        assertGeneratedSources(nested("@ServiceDefinition ( batch = true, batchName = \"internal.BATCH\" )"))
                .hasSize(2)
                .areAtLeastOne(named("definition", "NestedLoader.java"))
                .areAtLeastOne(named("internal", "BATCH.java"));

        assertGeneratedSources(nested("@ServiceDefinition ( batch = true, loaderName = \"internal.LOADER\" )"))
                .hasSize(2)
                .areAtLeastOne(named("internal", "LOADER.java"))
                .areAtLeastOne(named("definition", "NestedBatch.java"));

        assertGeneratedSources(nested("@ServiceDefinition ( batch = true, loaderName = \"internal.LOADER\", batchName = \"internal.BATCH\" )"))
                .hasSize(2)
                .areAtLeastOne(named("internal", "LOADER.java"))
                .areAtLeastOne(named("internal", "BATCH.java"));
    }

    @Test
    public void testMultiRoundProcessing() {
        JavaFileObject file = JavaFileObjects.forResource("definition/MultiRoundProcessing.java");

        assertAbout(javaSource())
                .that(file)
                .processedWith(new ServiceDefinitionProcessor())
                .compilesWithoutWarnings()
                .and().generatesFileNamed(SOURCE_OUTPUT, "internal", "FirstLoader.java")
                .and().generatesFileNamed(SOURCE_OUTPUT, "internal", "SecondLoader.java")
        ;
    }

    private static Compilation compile(JavaFileObject file) {
        return com.google.testing.compile.Compiler.javac()
                .withProcessors(new ServiceDefinitionProcessor(), new ServiceProviderProcessor())
                .compile(file);
    }

    private static JavaFileObject nested(String line) {
        List<String> content = new ArrayList<>();
        content.add("package definition;");
        content.add("import nbbrd.service.ServiceDefinition;");
        content.add("public class Nested {");
        content.addAll(Arrays.asList(line));
        content.add("public interface Foo {}");
        content.add("}");
        return JavaFileObjects.forSourceLines("definition.Nested", content);
    }

    private static ListAssert<JavaFileObject> assertGeneratedSources(JavaFileObject file) {
        return Assertions.assertThat(compile(file).generatedSourceFiles());
    }

    private static Condition<JavaFileObject> named(String packageName, String relativeName) {
        String expected = getFileName(SOURCE_OUTPUT, packageName, relativeName);
        return mappedCondition(JavaFileObject::getName, new HamcrestCondition<>(equalTo(expected)));
    }

    private static String getFileName(JavaFileManager.Location location, String packageName, String relativeName) {
        String path = packageName.isEmpty() ? relativeName : packageName.replace('.', '/') + '/' + relativeName;
        return String.format("/%s/%s", location.getName(), path);
    }
}

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

import _test.Compilations;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import internal.nbbrd.service.provider.ServiceProviderProcessor;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.Processor;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import static _test.Compilations.*;
import static com.google.testing.compile.JavaFileObjects.forResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.assertj.core.groups.Tuple.tuple;

/**
 * @author Philippe Charles
 */
public class ServiceDefinitionProcessorTest {

    @Test
    public void testRegistration() {
        assertThat(ServiceLoader.load(Processor.class))
                .hasAtLeastOneElementOfType(ServiceDefinitionProcessor.class);
    }

    @Test
    public void testNonNestedDef() {
        Compilation compilation = compile(forResource("definition/TestNonNestedDef.java"));

        assertThat(compilation)
                .has(succeededWithoutWarnings())
                .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                .singleElement()
                .has(sourceFileNamed("definition", "TestNonNestedDefLoader.java"))
                .extracting(Compilations::contentsAsUtf8String, STRING)
                .isEqualToIgnoringNewLines(contentsAsUtf8String(forResource("definition/expected/TestNonNestedDefLoader.java")));
    }

    @Test
    public void testNestedDef() {
        Compilation compilation = compile(forResource("definition/NestedDef.java"));

        assertThat(compilation)
                .has(succeededWithoutWarnings())
                .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                .singleElement()
                .has(sourceFileNamed("definition", "NestedDefLoader.java"))
                .extracting(Compilations::contentsAsUtf8String, STRING)
                .contains("public static final class HelloService {");
    }

    @Test
    public void testSingleDef() {
        Compilation compilation = compile(forResource("definition/SingleDef.java"));

        assertThat(compilation)
                .has(succeeded());

        assertThat(compilation)
                .extracting(Compilation::warnings, DIAGNOSTICS)
                .extracting(Compilations::getDefaultMessage)
                .contains("Thread-unsafe singleton for 'definition.SingleDef.MutableSingleton'");

        assertThat(compilation)
                .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                .singleElement()
                .has(sourceFileNamed("definition", "SingleDefLoader.java"))
                .extracting(Compilations::contentsAsUtf8String, STRING)
                .contains(
                        "private final SingleDef.Immutable resource = doLoad();",
                        "public static SingleDef.Immutable load()",
                        "private SingleDef.Mutable resource = doLoad();",
                        "private final AtomicReference<SingleDef.ThreadSafe> resource = new AtomicReference<>(doLoad());",
                        "private static final SingleDef.ImmutableSingleton RESOURCE = doLoad();",
                        "private static SingleDef.MutableSingleton RESOURCE = doLoad();",
                        "private static final AtomicReference<SingleDef.ThreadSafeSingleton> RESOURCE = new AtomicReference<>(doLoad());"
                );
    }

    @Test
    public void testOptionalDef() {
        JavaFileObject file = forResource("definition/OptionalDef.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .has(succeeded());

        assertThat(compilation)
                .extracting(Compilation::warnings, DIAGNOSTICS)
                .extracting(Compilations::getDefaultMessage)
                .contains("Thread-unsafe singleton for 'definition.OptionalDef.MutableSingleton'");

        assertThat(compilation)
                .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                .singleElement()
                .has(sourceFileNamed("definition", "OptionalDefLoader.java"))
                .extracting(Compilations::contentsAsUtf8String, STRING)
                .contains(
                        "private final Optional<OptionalDef.Immutable> resource = doLoad();",
                        "public static Optional<OptionalDef.Immutable> load()",
                        "private Optional<OptionalDef.Mutable> resource = doLoad();",
                        "private final AtomicReference<Optional<OptionalDef.ThreadSafe>> resource = new AtomicReference<>(doLoad());",
                        "private static final Optional<OptionalDef.ImmutableSingleton> RESOURCE = doLoad();",
                        "private static Optional<OptionalDef.MutableSingleton> RESOURCE = doLoad();",
                        "private static final AtomicReference<Optional<OptionalDef.ThreadSafeSingleton>> RESOURCE = new AtomicReference<>(doLoad());"
                );
    }

    @Test
    public void testMultipleDef() {
        JavaFileObject file = forResource("definition/MultipleDef.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .has(succeeded());

        assertThat(compilation)
                .extracting(Compilation::warnings, DIAGNOSTICS)
                .extracting(Compilations::getDefaultMessage)
                .contains("Thread-unsafe singleton for 'definition.MultipleDef.MutableSingleton'");

        assertThat(compilation)
                .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                .singleElement()
                .has(sourceFileNamed("definition", "MultipleDefLoader.java"))
                .extracting(Compilations::contentsAsUtf8String, STRING)
                .contains(
                        "private final List<MultipleDef.Immutable> resource = doLoad();",
                        "public static List<MultipleDef.Immutable> load()",
                        "private List<MultipleDef.Mutable> resource = doLoad();",
                        "private final AtomicReference<List<MultipleDef.ThreadSafe>> resource = new AtomicReference<>(doLoad());",
                        "private static final List<MultipleDef.ImmutableSingleton> RESOURCE = doLoad();",
                        "private static List<MultipleDef.MutableSingleton> RESOURCE = doLoad();",
                        "private static final AtomicReference<List<MultipleDef.ThreadSafeSingleton>> RESOURCE = new AtomicReference<>(doLoad());"
                );
    }

    @Test
    public void testAlternateFactories() {
        JavaFileObject file = forResource("definition/AlternateFactories.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .has(succeededWithoutWarnings());
    }

    @Test
    public void testNonInstantiableFallback() {
        JavaFileObject file = forResource("definition/NonInstantiableFallback.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .has(failed());

        assertThat(compilation)
                .extracting(Compilation::errors, DIAGNOSTICS)
                .hasSize(1)
                .extracting(Compilations::getDefaultMessage, Diagnostic::getSource, Diagnostic::getLineNumber)
                .containsOnly(
                        tuple("Don't know how to instantiate 'definition.NonInstantiableFallback.SomeFallback'", file, 9L)
                );
    }

    @Test
    public void testNonInstantiableWrapper() {
        JavaFileObject file = forResource("definition/NonInstantiableWrapper.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .has(failed());

        assertThat(compilation)
                .extracting(Compilation::errors, DIAGNOSTICS)
                .hasSize(4)
                .extracting(Compilations::getDefaultMessage, Diagnostic::getSource, Diagnostic::getLineNumber)
                .containsOnly(
                        tuple("Don't know how to wrap 'definition.NonInstantiableWrapper.WrapperByPrivateConstructor'", file, 8L),
                        tuple("Don't know how to wrap 'definition.NonInstantiableWrapper.WrapperByNoArgConstructor'", file, 18L),
                        tuple("Don't know how to wrap 'definition.NonInstantiableWrapper.WrapperByInvalidArgConstructor'", file, 28L),
                        tuple("Don't know how to wrap 'definition.NonInstantiableWrapper.WrapperByToManyStaticMethods'", file, 38L)
                );
    }

    @Test
    public void testNonInstantiablePreprocessor() {
        JavaFileObject file = forResource("definition/NonInstantiablePreprocessor.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .has(failed());

        assertThat(compilation)
                .extracting(Compilation::errors, DIAGNOSTICS)
                .hasSize(1)
                .extracting(Compilations::getDefaultMessage, Diagnostic::getSource, Diagnostic::getLineNumber)
                .containsOnly(
                        tuple("Don't know how to instantiate 'definition.NonInstantiablePreprocessor.SomePreprocessor'", file, 11L)
                );
    }

    @Test
    public void testNonAssignableFallback() {
        JavaFileObject file = forResource("definition/NonAssignableFallback.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .has(failed());

        assertThat(compilation)
                .extracting(Compilation::errors, DIAGNOSTICS)
                .hasSize(1)
                .extracting(Compilations::getDefaultMessage, Diagnostic::getSource, Diagnostic::getLineNumber)
                .containsOnly(
                        tuple("Fallback 'java.lang.String' doesn't extend nor implement service 'definition.NonAssignableFallback.HelloService'", file, 8L)
                );
    }

    @Test
    public void testNonAssignablePreprocessor() {
        JavaFileObject file = forResource("definition/NonAssignablePreprocessor.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .has(failed());

        assertThat(compilation)
                .extracting(Compilation::errors, DIAGNOSTICS)
                .hasSize(1)
                .extracting(Compilations::getDefaultMessage, Diagnostic::getSource, Diagnostic::getLineNumber)
                .containsOnly(
                        tuple("Preprocessor 'definition.NonAssignablePreprocessor.HelloProc' doesn't extend nor implement 'java.util.function.UnaryOperator<java.util.stream.Stream<definition.NonAssignablePreprocessor.HelloService>>'", file, 10L)
                );
    }

    @Test
    public void testNonAssignableWrapper() {
        JavaFileObject file = forResource("definition/NonAssignableWrapper.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .has(failed());

        assertThat(compilation)
                .extracting(Compilation::errors, DIAGNOSTICS)
                .hasSize(1)
                .extracting(Compilations::getDefaultMessage, Diagnostic::getSource, Diagnostic::getLineNumber)
                .containsOnly(
                        tuple("Wrapper 'java.lang.String' doesn't extend nor implement service 'definition.NonAssignableWrapper.HelloService'", file, 8L)
                );
    }

    @Test
    public void testFilters() {
        JavaFileObject file = forResource("definition/Filters.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .has(succeededWithoutWarnings());

        assertThat(compilation)
                .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                .singleElement()
                .has(sourceFileNamed("definition", "FiltersLoader.java"))
                .extracting(Compilations::contentsAsUtf8String, STRING)
                .contains(
                        "private final Optional<Filters.SingleFilter> resource = doLoad();",
                        ".filter(Filters.SingleFilter::isAvailable)",
                        "private final Optional<Filters.MultiFilter> resource = doLoad();",
                        ".filter(((Predicate<Filters.MultiFilter>)Filters.MultiFilter::isAvailable).and(Filters.MultiFilter::isFastEnough))",
                        "private final Optional<Filters.ReversedFilter> resource = doLoad();",
                        ".filter(((Predicate<Filters.ReversedFilter>)Filters.ReversedFilter::isAvailable).negate())",
                        "private final Optional<Filters.MultiFilterWithPosition> resource = doLoad();",
                        ".filter(((Predicate<Filters.MultiFilterWithPosition>)Filters.MultiFilterWithPosition::isFastEnough).and(Filters.MultiFilterWithPosition::isAvailable))"
                );
    }

    @Test
    public void testNoArgFilter() {
        JavaFileObject file = forResource("definition/NoArgFilter.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .has(failed());

        assertThat(compilation)
                .extracting(Compilation::errors, DIAGNOSTICS)
                .hasSize(1)
                .extracting(Compilations::getDefaultMessage, Diagnostic::getSource, Diagnostic::getLineNumber)
                .containsOnly(
                        tuple("Filter method must have no-args", file, 10L)
                );
    }

    @Test
    public void testStaticFilter() {
        JavaFileObject file = forResource("definition/StaticFilter.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .has(failed());

        assertThat(compilation)
                .extracting(Compilation::errors, DIAGNOSTICS)
                .hasSize(1)
                .extracting(Compilations::getDefaultMessage, Diagnostic::getSource, Diagnostic::getLineNumber)
                .containsOnly(
                        tuple("Filter method does not apply to static methods", file, 10L)
                );
    }

    @Test
    public void testLostFilter() {
        JavaFileObject file = forResource("definition/LostFilter.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .has(failed());

        assertThat(compilation)
                .extracting(Compilation::errors, DIAGNOSTICS)
                .hasSize(1)
                .extracting(Compilations::getDefaultMessage, Diagnostic::getSource, Diagnostic::getLineNumber)
                .containsOnly(
                        tuple("Filter method only applies to methods of a service", file, 8L)
                );
    }

    @Test
    public void testNonBooleanFilter() {
        JavaFileObject file = forResource("definition/NonBooleanFilter.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .has(failed());

        assertThat(compilation)
                .extracting(Compilation::errors, DIAGNOSTICS)
                .hasSize(1)
                .extracting(Compilations::getDefaultMessage, Diagnostic::getSource, Diagnostic::getLineNumber)
                .containsOnly(
                        tuple("Filter method must return boolean", file, 10L)
                );
    }

    @Test
    public void testWrappers() {
        JavaFileObject file = forResource("definition/Wrappers.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .has(succeededWithoutWarnings());

        assertThat(compilation)
                .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                .singleElement()
                .has(sourceFileNamed("definition", "WrappersLoader.java"))
                .extracting(Compilations::contentsAsUtf8String, STRING)
                .contains(
                        "private final Optional<Wrappers.ByConstructor> resource = doLoad();",
                        ".map(Wrappers.WrapperByConstructor::new)",
                        "private final Optional<Wrappers.ByStaticMethod> resource = doLoad();",
                        ".map(Wrappers.WrapperByStaticMethod::wrap)",
                        "private final Optional<Wrappers.ByStaticMethodX> resource = doLoad();",
                        ".map(Wrappers.WrapperByStaticMethodX::wrap)"
                );
    }

    @Test
    public void testSorters() {
        JavaFileObject file = forResource("definition/Sorters.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .has(succeededWithoutWarnings());

        assertThat(compilation)
                .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                .singleElement()
                .has(sourceFileNamed("definition", "SortersLoader.java"))
                .extracting(Compilations::contentsAsUtf8String, STRING)
                .contains(
                        "private final Optional<Sorters.IntSorter> resource = doLoad();",
                        ".sorted(Comparator.comparingInt(Sorters.IntSorter::getCost))",
                        "private final Optional<Sorters.LongSorter> resource = doLoad();",
                        ".sorted(Comparator.comparingLong(Sorters.LongSorter::getCost))",
                        "private final Optional<Sorters.DoubleSorter> resource = doLoad();",
                        ".sorted(Comparator.comparingDouble(Sorters.DoubleSorter::getCost))",
                        "private final Optional<Sorters.ComparableSorter> resource = doLoad();",
                        ".sorted(Comparator.comparing(Sorters.ComparableSorter::getCost))",
                        "private final Optional<Sorters.MultiSorter> resource = doLoad();",
                        ".sorted(((Comparator<Sorters.MultiSorter>)Comparator.comparingInt(Sorters.MultiSorter::getCost)).thenComparing(Comparator.comparingDouble(Sorters.MultiSorter::getAccuracy)))",
                        "private final Optional<Sorters.ReversedSorter> resource = doLoad();",
                        ".sorted(Collections.reverseOrder(Comparator.comparingInt(Sorters.ReversedSorter::getCost)))",
                        "private final Optional<Sorters.MultiSorterWithPosition> resource = doLoad();",
                        ".sorted(((Comparator<Sorters.MultiSorterWithPosition>)Comparator.comparingDouble(Sorters.MultiSorterWithPosition::getAccuracy)).thenComparing(Comparator.comparingInt(Sorters.MultiSorterWithPosition::getCost)))"
                );
    }

    @Test
    public void testNoArgSorter() {
        JavaFileObject file = forResource("definition/NoArgSorter.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .has(failed());

        assertThat(compilation)
                .extracting(Compilation::errors, DIAGNOSTICS)
                .hasSize(1)
                .extracting(Compilations::getDefaultMessage, Diagnostic::getSource, Diagnostic::getLineNumber)
                .containsOnly(
                        tuple("Sorter method must have no-args", file, 10L)
                );
    }

    @Test
    public void testStaticSorter() {
        JavaFileObject file = forResource("definition/StaticSorter.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .has(failed());

        assertThat(compilation)
                .extracting(Compilation::errors, DIAGNOSTICS)
                .hasSize(1)
                .extracting(Compilations::getDefaultMessage, Diagnostic::getSource, Diagnostic::getLineNumber)
                .containsOnly(
                        tuple("Sorter method does not apply to static methods", file, 10L)
                );
    }

    @Test
    public void testLostSorter() {
        JavaFileObject file = forResource("definition/LostSorter.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .has(failed());

        assertThat(compilation)
                .extracting(Compilation::errors, DIAGNOSTICS)
                .hasSize(1)
                .extracting(Compilations::getDefaultMessage, Diagnostic::getSource, Diagnostic::getLineNumber)
                .containsOnly(
                        tuple("Sorter method only applies to methods of a service", file, 8L)
                );
    }

    @Test
    public void testNonComparableSorter() {
        JavaFileObject file = forResource("definition/NonComparableSorter.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .has(failed());

        assertThat(compilation)
                .extracting(Compilation::errors, DIAGNOSTICS)
                .hasSize(1)
                .extracting(Compilations::getDefaultMessage, Diagnostic::getSource, Diagnostic::getLineNumber)
                .containsOnly(
                        tuple("Sorter method must return double, int, long or comparable", file, 10L)
                );
    }

    @Test
    public void testCustomBackend() {
        assertThat(compile(forResource("definition/CustomBackend.java")))
                .has(succeededWithoutWarnings())
                .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                .filteredOn(sourceFileNamed("definition", "CustomBackendLoader.java"))
                .singleElement()
                .extracting(Compilations::contentsAsUtf8String, STRING)
                .contains("private final Iterable<CustomBackend> source = CustomBackend.NetBeansLookup.INSTANCE.apply(CustomBackend.class);");
    }

    @Test
    public void testNonAssignableBackend() {
        JavaFileObject file = forResource("definition/NonAssignableBackend.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .has(failed());

        assertThat(compilation)
                .extracting(Compilation::errors, DIAGNOSTICS)
                .hasSize(1)
                .extracting(Compilations::getDefaultMessage, Diagnostic::getSource, Diagnostic::getLineNumber)
                .containsOnly(
                        tuple("Backend 'definition.NonAssignableBackend.HelloProc' doesn't extend nor implement 'java.util.function.Function<java.lang.Class,? extends java.lang.Iterable>'", file, 11L)
                );
    }

    @Test
    public void testNonNestedBatch() {
        JavaFileObject file = forResource("definition/NonNestedBatch.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .has(succeededWithoutWarnings());

        assertThat(compilation)
                .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                .filteredOn(sourceFileNamed("definition", "NonNestedBatchLoader.java"))
                .singleElement()
                .extracting(Compilations::contentsAsUtf8String, STRING)
                .contains(
                        "private final Iterable<NonNestedBatch> source = ServiceLoader.load(NonNestedBatch.class);",
                        "private final Iterable<NonNestedBatchBatch> batch = ServiceLoader.load(NonNestedBatchBatch.class);"
                );

        assertThat(compilation)
                .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                .filteredOn(sourceFileNamed("definition", "NonNestedBatchBatch.java"))
                .singleElement()
                .extracting(Compilations::contentsAsUtf8String, STRING)
                .contains(
                        "Stream<NonNestedBatch> getProviders();"
                );

        assertThat(compilation)
                .extracting(Compilation::generatedFiles, JAVA_FILE_OBJECTS)
                .filteredOn(fileNamed("/CLASS_OUTPUT/META-INF/services/definition.NonNestedBatchBatch"))
                .singleElement()
                .extracting(Compilations::contentsAsUtf8String, STRING)
                .contains(
                        "definition.NonNestedBatch$ABC"
                );
    }

    @Test
    public void testNestedBatch() {
        JavaFileObject file = forResource("definition/NestedBatch.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .has(succeededWithoutWarnings());

        assertThat(compilation)
                .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                .filteredOn(sourceFileNamed("definition", "NestedBatchLoader.java"))
                .singleElement()
                .extracting(Compilations::contentsAsUtf8String, STRING)
                .contains(
                        "private final Iterable<NestedBatch.HelloService> source = ServiceLoader.load(NestedBatch.HelloService.class);",
                        "private final Iterable<NestedBatchBatch.HelloService> batch = ServiceLoader.load(NestedBatchBatch.HelloService.class);"
                );

        assertThat(compilation)
                .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                .filteredOn(sourceFileNamed("definition", "NestedBatchBatch.java"))
                .singleElement()
                .extracting(Compilations::contentsAsUtf8String, STRING)
                .contains(
                        "Stream<NestedBatch.HelloService> getProviders();"
                );

        assertThat(compilation)
                .extracting(Compilation::generatedFiles, JAVA_FILE_OBJECTS)
                .filteredOn(fileNamed("/CLASS_OUTPUT/META-INF/services/definition.NestedBatchBatch$HelloService"))
                .singleElement()
                .extracting(Compilations::contentsAsUtf8String, STRING)
                .contains(
                        "definition.NestedBatch$ABC"
                );
    }

    @Test
    public void testAlternateNames() {
        assertThat(compile(forResource("definition/AlternateNames.java")))
                .has(succeededWithoutWarnings())
                .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                .hasSize(4)
                .haveAtLeastOne(sourceFileNamed("definition", "AlternateNamesLoader.java"))
                .haveAtLeastOne(sourceFileNamed("definition", "AlternateNamesBatch.java"))
                .haveAtLeastOne(sourceFileNamed("internal", "FooLoader.java"))
                .haveAtLeastOne(sourceFileNamed("internal", "BarBatch.java"));

        assertThat(compile(nested("@ServiceDefinition ( )")))
                .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                .hasSize(1)
                .haveAtLeastOne(sourceFileNamed("definition", "NestedLoader.java"));

        assertThat(compile(nested("@ServiceDefinition ( loaderName = \"internal.LOADER\" )")))
                .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                .hasSize(1)
                .haveAtLeastOne(sourceFileNamed("internal", "LOADER.java"));

        assertThat(compile(nested("@ServiceDefinition ( batch = true )")))
                .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                .hasSize(2)
                .haveAtLeastOne(sourceFileNamed("definition", "NestedLoader.java"))
                .haveAtLeastOne(sourceFileNamed("definition", "NestedBatch.java"));

        assertThat(compile(nested("@ServiceDefinition ( batch = true, batchName = \"internal.BATCH\" )")))
                .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                .hasSize(2)
                .haveAtLeastOne(sourceFileNamed("definition", "NestedLoader.java"))
                .haveAtLeastOne(sourceFileNamed("internal", "BATCH.java"));

        assertThat(compile(nested("@ServiceDefinition ( batch = true, loaderName = \"internal.LOADER\" )")))
                .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                .hasSize(2)
                .haveAtLeastOne(sourceFileNamed("internal", "LOADER.java"))
                .haveAtLeastOne(sourceFileNamed("definition", "NestedBatch.java"));

        assertThat(compile(nested("@ServiceDefinition ( batch = true, loaderName = \"internal.LOADER\", batchName = \"internal.BATCH\" )")))
                .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                .hasSize(2)
                .haveAtLeastOne(sourceFileNamed("internal", "LOADER.java"))
                .haveAtLeastOne(sourceFileNamed("internal", "BATCH.java"));
    }

    @Test
    public void testMultiRoundProcessing() {
        JavaFileObject file = forResource("definition/TestMultiRoundProcessing.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .has(succeededWithoutWarnings());

        assertThat(compilation)
                .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                .haveAtLeastOne(sourceFileNamed("internal", "FirstLoader.java"))
                .haveAtLeastOne(sourceFileNamed("internal", "SecondLoader.java"));
    }

    @Test
    public void testBatchReloading() {
        Compilation compilation = compile(forResource("definition/BatchReloading.java"));

        assertThat(compilation)
                .has(succeeded());

        assertThat(compilation)
                .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                .filteredOn(sourceFileNamed("definition", "BatchReloadingLoader.java"))
                .singleElement()
                .extracting(Compilations::contentsAsUtf8String, STRING)
                .isEqualToIgnoringNewLines(contentsAsUtf8String(forResource("definition/expected/TestBatchReloadingLoader.java")));
    }

    private static Compilation compile(JavaFileObject file) {
        return Compiler.javac()
                .withProcessors(new ServiceDefinitionProcessor(), new ServiceProviderProcessor())
                .compile(file);
    }

    private static JavaFileObject nested(String line) {
        List<String> content = new ArrayList<>();
        content.add("package definition;");
        content.add("import nbbrd.service.ServiceDefinition;");
        content.add("public class Nested {");
        content.add(line);
        content.add("public interface Foo {}");
        content.add("}");
        return JavaFileObjects.forSourceLines("definition.Nested", content);
    }
}

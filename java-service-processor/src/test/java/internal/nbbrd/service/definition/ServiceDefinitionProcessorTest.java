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
import org.junit.jupiter.api.Nested;
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
    public void testAlternateFactories() {
        JavaFileObject file = forResource("definition/AlternateFactories.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .has(succeededWithoutWarnings());
    }

    @Test
    public void testMultiRoundProcessing() {
        JavaFileObject file = forResource("definition/TestMultiRoundProcessing.java");

        assertThat(compile(file))
                .has(succeededWithoutWarnings())
                .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                .haveAtLeastOne(sourceFileNamed("internal", "FirstLoader.java"))
                .haveAtLeastOne(sourceFileNamed("internal", "SecondLoader.java"));
    }

    @Test
    public void testAllOptions() {
        JavaFileObject file = forResource("definition/TestAllOptions.java");

        assertThat(compile(file))
                .has(succeededWithoutWarnings())
                .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                .filteredOn(sourceFileNamed("definition", "TestAllOptionsLoader.java"))
                .singleElement()
                .extracting(Compilations::contentsAsUtf8String, STRING)
                .isEqualToIgnoringNewLines(contentsAsUtf8String(forResource("definition/expected/TestAllOptionsLoader.java")));
    }

    @Nested
    class QuantifierTest {

        @Test
        public void testSingle() {
            Compilation compilation = compile(forResource("definition/TestQuantifierSingle.java"));

            assertThat(compilation)
                    .has(succeeded());

            assertThat(compilation)
                    .extracting(Compilation::warnings, DIAGNOSTICS)
                    .extracting(Compilations::getDefaultMessage)
                    .contains("Thread-unsafe singleton for 'definition.TestQuantifierSingle.MutableSingleton'");

            assertThat(compilation)
                    .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                    .singleElement()
                    .has(sourceFileNamed("definition", "TestQuantifierSingleLoader.java"))
                    .extracting(Compilations::contentsAsUtf8String, STRING)
                    .contains(
                            "private final TestQuantifierSingle.Immutable resource = doLoad();",
                            "public static TestQuantifierSingle.Immutable load()",
                            "private TestQuantifierSingle.Mutable resource = doLoad();",
                            "private final AtomicReference<TestQuantifierSingle.ThreadSafe> resource = new AtomicReference<>(doLoad());",
                            "private static final TestQuantifierSingle.ImmutableSingleton RESOURCE = doLoad();",
                            "private static TestQuantifierSingle.MutableSingleton RESOURCE = doLoad();",
                            "private static final AtomicReference<TestQuantifierSingle.ThreadSafeSingleton> RESOURCE = new AtomicReference<>(doLoad());"
                    );
        }

        @Test
        public void testOptional() {
            JavaFileObject file = forResource("definition/TestQuantifierOptional.java");
            Compilation compilation = compile(file);

            assertThat(compilation)
                    .has(succeeded());

            assertThat(compilation)
                    .extracting(Compilation::warnings, DIAGNOSTICS)
                    .extracting(Compilations::getDefaultMessage)
                    .contains("Thread-unsafe singleton for 'definition.TestQuantifierOptional.MutableSingleton'");

            assertThat(compilation)
                    .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                    .singleElement()
                    .has(sourceFileNamed("definition", "TestQuantifierOptionalLoader.java"))
                    .extracting(Compilations::contentsAsUtf8String, STRING)
                    .contains(
                            "private final Optional<TestQuantifierOptional.Immutable> resource = doLoad();",
                            "public static Optional<TestQuantifierOptional.Immutable> load()",
                            "private Optional<TestQuantifierOptional.Mutable> resource = doLoad();",
                            "private final AtomicReference<Optional<TestQuantifierOptional.ThreadSafe>> resource = new AtomicReference<>(doLoad());",
                            "private static final Optional<TestQuantifierOptional.ImmutableSingleton> RESOURCE = doLoad();",
                            "private static Optional<TestQuantifierOptional.MutableSingleton> RESOURCE = doLoad();",
                            "private static final AtomicReference<Optional<TestQuantifierOptional.ThreadSafeSingleton>> RESOURCE = new AtomicReference<>(doLoad());"
                    );
        }

        @Test
        public void testMultiple() {
            JavaFileObject file = forResource("definition/TestQuantifierMultiple.java");
            Compilation compilation = compile(file);

            assertThat(compilation)
                    .has(succeeded());

            assertThat(compilation)
                    .extracting(Compilation::warnings, DIAGNOSTICS)
                    .extracting(Compilations::getDefaultMessage)
                    .contains("Thread-unsafe singleton for 'definition.TestQuantifierMultiple.MutableSingleton'");

            assertThat(compilation)
                    .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                    .singleElement()
                    .has(sourceFileNamed("definition", "TestQuantifierMultipleLoader.java"))
                    .extracting(Compilations::contentsAsUtf8String, STRING)
                    .contains(
                            "private final List<TestQuantifierMultiple.Immutable> resource = doLoad();",
                            "public static List<TestQuantifierMultiple.Immutable> load()",
                            "private List<TestQuantifierMultiple.Mutable> resource = doLoad();",
                            "private final AtomicReference<List<TestQuantifierMultiple.ThreadSafe>> resource = new AtomicReference<>(doLoad());",
                            "private static final List<TestQuantifierMultiple.ImmutableSingleton> RESOURCE = doLoad();",
                            "private static List<TestQuantifierMultiple.MutableSingleton> RESOURCE = doLoad();",
                            "private static final AtomicReference<List<TestQuantifierMultiple.ThreadSafeSingleton>> RESOURCE = new AtomicReference<>(doLoad());"
                    );
        }
    }

    @Nested
    class LoaderNameTest {

        @Test
        public void testValid() {
            assertThat(compile(forResource("definition/TestLoaderNameValid.java")))
                    .has(succeededWithoutWarnings())
                    .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                    .hasSize(4)
                    .haveAtLeastOne(sourceFileNamed("definition", "TestLoaderNameValidLoader.java"))
                    .haveAtLeastOne(sourceFileNamed("definition", "TestLoaderNameValidBatch.java"))
                    .haveAtLeastOne(sourceFileNamed("internal", "FooLoader.java"))
                    .haveAtLeastOne(sourceFileNamed("internal", "BarBatch.java"));
        }

        @Test
        public void testNestedNames() {
            assertThat(compile(nested("@ServiceDefinition ( )")))
                    .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                    .hasSize(1)
                    .haveAtLeastOne(sourceFileNamed("definition", "NestedLoader.java"));

            assertThat(compile(nested("@ServiceDefinition ( loaderName = \"internal.LOADER\" )")))
                    .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                    .hasSize(1)
                    .haveAtLeastOne(sourceFileNamed("internal", "LOADER.java"));
        }
    }

    @Nested
    class ServiceFilterTest {

        @Test
        public void testValid() {
            JavaFileObject file = forResource("definition/TestFilterValid.java");

            assertThat(compile(file))
                    .has(succeededWithoutWarnings())
                    .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                    .singleElement()
                    .has(sourceFileNamed("definition", "TestFilterValidLoader.java"))
                    .extracting(Compilations::contentsAsUtf8String, STRING)
                    .contains(
                            ".filter(filter)",
                            "private final Optional<TestFilterValid.SingleFilter> resource = doLoad();",
                            "private final Predicate<TestFilterValid.SingleFilter> filter = TestFilterValid.SingleFilter::isAvailable",
                            "private final Optional<TestFilterValid.MultiFilter> resource = doLoad();",
                            "private final Predicate<TestFilterValid.MultiFilter> filter = ((Predicate<TestFilterValid.MultiFilter>)TestFilterValid.MultiFilter::isAvailable).and(TestFilterValid.MultiFilter::isFastEnough)",
                            "private final Optional<TestFilterValid.ReversedFilter> resource = doLoad();",
                            "private final Predicate<TestFilterValid.ReversedFilter> filter = ((Predicate<TestFilterValid.ReversedFilter>)TestFilterValid.ReversedFilter::isAvailable).negate()",
                            "private final Optional<TestFilterValid.MultiFilterWithPosition> resource = doLoad();",
                            "private final Predicate<TestFilterValid.MultiFilterWithPosition> filter = ((Predicate<TestFilterValid.MultiFilterWithPosition>)TestFilterValid.MultiFilterWithPosition::isFastEnough).and(TestFilterValid.MultiFilterWithPosition::isAvailable)"
                    );
        }

        @Test
        public void testLost() {
            JavaFileObject file = forResource("definition/TestFilterLost.java");

            assertThat(compile(file))
                    .has(failed())
                    .extracting(Compilation::errors, DIAGNOSTICS)
                    .singleElement()
                    .returns("[RULE_F1] Filter method only applies to methods of a service", Compilations::getDefaultMessage)
                    .returns(file, Diagnostic::getSource)
                    .returns(8L, Diagnostic::getLineNumber);
        }

        @Test
        public void testStatic() {
            JavaFileObject file = forResource("definition/TestFilterStatic.java");

            assertThat(compile(file))
                    .has(failed())
                    .extracting(Compilation::errors, DIAGNOSTICS)
                    .singleElement()
                    .returns("[RULE_F2] Filter method does not apply to static methods", Compilations::getDefaultMessage)
                    .returns(file, Diagnostic::getSource)
                    .returns(10L, Diagnostic::getLineNumber);
        }

        @Test
        public void testNoArg() {
            JavaFileObject file = forResource("definition/TestFilterNoArg.java");

            assertThat(compile(file))
                    .has(failed())
                    .extracting(Compilation::errors, DIAGNOSTICS)
                    .singleElement()
                    .returns("[RULE_F3] Filter method must have no-args", Compilations::getDefaultMessage)
                    .returns(file, Diagnostic::getSource)
                    .returns(10L, Diagnostic::getLineNumber);
        }

        @Test
        public void testNonBoolean() {
            JavaFileObject file = forResource("definition/TestFilterNonBoolean.java");

            assertThat(compile(file))
                    .has(failed())
                    .extracting(Compilation::errors, DIAGNOSTICS)
                    .singleElement()
                    .returns("[RULE_F4] Filter method must return boolean", Compilations::getDefaultMessage)
                    .returns(file, Diagnostic::getSource)
                    .returns(10L, Diagnostic::getLineNumber);
        }

        @Test
        public void testCheckedException() {
            JavaFileObject file = forResource("definition/TestFilterCheckedException.java");

            assertThat(compile(file))
                    .has(failed())
                    .extracting(Compilation::errors, DIAGNOSTICS)
                    .singleElement()
                    .returns("[RULE_F5] Filter method must not throw checked exceptions", Compilations::getDefaultMessage)
                    .returns(file, Diagnostic::getSource)
                    .returns(12L, Diagnostic::getLineNumber);
        }

        @Test
        public void testUncheckedException() {
            JavaFileObject file = forResource("definition/TestFilterUncheckedException.java");

            assertThat(compile(file))
                    .has(succeededWithoutWarnings());
        }
    }

    @Nested
    class ServiceSorterTest {

        @Test
        public void testValid() {
            JavaFileObject file = forResource("definition/TestSorterValid.java");

            assertThat(compile(file))
                    .has(succeededWithoutWarnings())
                    .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                    .singleElement()
                    .has(sourceFileNamed("definition", "TestSorterValidLoader.java"))
                    .extracting(Compilations::contentsAsUtf8String, STRING)
                    .contains(
                            ".sorted(sorter)",
                            "private final Optional<TestSorterValid.IntSorter> resource = doLoad();",
                            "Comparator.comparingInt(TestSorterValid.IntSorter::getCost)",
                            "private final Optional<TestSorterValid.LongSorter> resource = doLoad();",
                            "Comparator.comparingLong(TestSorterValid.LongSorter::getCost)",
                            "private final Optional<TestSorterValid.DoubleSorter> resource = doLoad();",
                            "Comparator.comparingDouble(TestSorterValid.DoubleSorter::getCost)",
                            "private final Optional<TestSorterValid.ComparableSorter> resource = doLoad();",
                            "Comparator.comparing(TestSorterValid.ComparableSorter::getCost)",
                            "private final Optional<TestSorterValid.MultiSorter> resource = doLoad();",
                            "((Comparator<TestSorterValid.MultiSorter>)Comparator.comparingInt(TestSorterValid.MultiSorter::getCost)).thenComparing(Comparator.comparingDouble(TestSorterValid.MultiSorter::getAccuracy))",
                            "private final Optional<TestSorterValid.ReversedSorter> resource = doLoad();",
                            "Collections.reverseOrder(Comparator.comparingInt(TestSorterValid.ReversedSorter::getCost))",
                            "private final Optional<TestSorterValid.MultiSorterWithPosition> resource = doLoad();",
                            "((Comparator<TestSorterValid.MultiSorterWithPosition>)Comparator.comparingDouble(TestSorterValid.MultiSorterWithPosition::getAccuracy)).thenComparing(Comparator.comparingInt(TestSorterValid.MultiSorterWithPosition::getCost))"
                    );
        }

        @Test
        public void testLost() {
            JavaFileObject file = forResource("definition/TestSorterLost.java");

            assertThat(compile(file))
                    .has(failed())
                    .extracting(Compilation::errors, DIAGNOSTICS)
                    .singleElement()
                    .returns("[RULE_S1] Sorter method only applies to methods of a service", Compilations::getDefaultMessage)
                    .returns(file, Diagnostic::getSource)
                    .returns(8L, Diagnostic::getLineNumber);
        }

        @Test
        public void testStatic() {
            JavaFileObject file = forResource("definition/TestSorterStatic.java");

            assertThat(compile(file))
                    .has(failed())
                    .extracting(Compilation::errors, DIAGNOSTICS)
                    .singleElement()
                    .returns("[RULE_S2] Sorter method does not apply to static methods", Compilations::getDefaultMessage)
                    .returns(file, Diagnostic::getSource)
                    .returns(10L, Diagnostic::getLineNumber);
        }

        @Test
        public void testNoArg() {
            JavaFileObject file = forResource("definition/TestSorterNoArg.java");

            assertThat(compile(file))
                    .has(failed())
                    .extracting(Compilation::errors, DIAGNOSTICS)
                    .singleElement()
                    .returns("[RULE_S3] Sorter method must have no-args", Compilations::getDefaultMessage)
                    .returns(file, Diagnostic::getSource)
                    .returns(10L, Diagnostic::getLineNumber);
        }

        @Test
        public void testNonComparable() {
            JavaFileObject file = forResource("definition/TestSorterNonComparable.java");

            assertThat(compile(file))
                    .has(failed())
                    .extracting(Compilation::errors, DIAGNOSTICS)
                    .singleElement()
                    .returns("[RULE_S4] Sorter method must return double, int, long or comparable", Compilations::getDefaultMessage)
                    .returns(file, Diagnostic::getSource)
                    .returns(10L, Diagnostic::getLineNumber);
        }

        @Test
        public void testCheckedException() {
            JavaFileObject file = forResource("definition/TestSorterCheckedException.java");

            assertThat(compile(file))
                    .has(failed())
                    .extracting(Compilation::errors, DIAGNOSTICS)
                    .singleElement()
                    .returns("[RULE_S5] Sorter method must not throw checked exceptions", Compilations::getDefaultMessage)
                    .returns(file, Diagnostic::getSource)
                    .returns(12L, Diagnostic::getLineNumber);
        }

        @Test
        public void testUncheckedException() {
            JavaFileObject file = forResource("definition/TestSorterUncheckedException.java");

            assertThat(compile(file))
                    .has(succeededWithoutWarnings());
        }
    }

    @Nested
    class ServiceIdTest {

        @Test
        public void testNestedClass() {
            assertThat(compile(forResource("definition/TestIdNestedClass.java")))
                    .has(succeededWithoutWarnings());
        }

        @Test
        public void testLost() {
            JavaFileObject file = forResource("definition/TestIdLost.java");

            assertThat(compile(file))
                    .has(failed())
                    .extracting(Compilation::errors, DIAGNOSTICS)
                    .singleElement()
                    .returns("[RULE_I1] Id method only applies to methods of a service", Compilations::getDefaultMessage)
                    .returns(file, Diagnostic::getSource)
                    .returns(8L, Diagnostic::getLineNumber);
        }

        @Test
        public void testStaticMethod() {
            JavaFileObject file = forResource("definition/TestIdStaticMethod.java");

            assertThat(compile(file))
                    .has(failed())
                    .extracting(Compilation::errors, DIAGNOSTICS)
                    .singleElement()
                    .returns("[RULE_I2] Id method does not apply to static methods", Compilations::getDefaultMessage)
                    .returns(file, Diagnostic::getSource)
                    .returns(10L, Diagnostic::getLineNumber);
        }

        @Test
        public void testNoArg() {
            JavaFileObject file = forResource("definition/TestIdNoArg.java");

            assertThat(compile(file))
                    .has(failed())
                    .extracting(Compilation::errors, DIAGNOSTICS)
                    .singleElement()
                    .returns("[RULE_I3] Id method must have no-args", Compilations::getDefaultMessage)
                    .returns(file, Diagnostic::getSource)
                    .returns(10L, Diagnostic::getLineNumber);
        }

        @Test
        public void testNonString() {
            JavaFileObject file = forResource("definition/TestIdNonString.java");

            assertThat(compile(file))
                    .has(failed())
                    .extracting(Compilation::errors, DIAGNOSTICS)
                    .singleElement()
                    .returns("[RULE_I4] Id method must return String", Compilations::getDefaultMessage)
                    .returns(file, Diagnostic::getSource)
                    .returns(10L, Diagnostic::getLineNumber);
        }

        @Test
        public void testUniqueness() {
            JavaFileObject file = forResource("definition/TestIdUniqueness.java");

            assertThat(compile(file))
                    .has(failed())
                    .extracting(Compilation::errors, DIAGNOSTICS)
                    .singleElement()
                    .returns("[RULE_I5] Id method must be unique", Compilations::getDefaultMessage)
                    .returns(file, Diagnostic::getSource)
                    .returns(13L, Diagnostic::getLineNumber);
        }

        @Test
        public void testCheckedException() {
            JavaFileObject file = forResource("definition/TestIdCheckedException.java");

            assertThat(compile(file))
                    .has(failed())
                    .extracting(Compilation::errors, DIAGNOSTICS)
                    .singleElement()
                    .returns("[RULE_I6] Id method must not throw checked exceptions", Compilations::getDefaultMessage)
                    .returns(file, Diagnostic::getSource)
                    .returns(12L, Diagnostic::getLineNumber);
        }

        @Test
        public void testUncheckedException() {
            JavaFileObject file = forResource("definition/TestIdUncheckedException.java");

            assertThat(compile(file))
                    .has(succeededWithoutWarnings());
        }

        @Test
        public void testEmptyPattern() {
            JavaFileObject file = forResource("definition/TestIdEmptyPattern.java");

            assertThat(compile(file))
                    .has(succeededWithoutWarnings())
                    .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                    .singleElement()
                    .has(sourceFileNamed("definition", "TestIdEmptyPatternLoader.java"))
                    .extracting(Compilations::contentsAsUtf8String, STRING)
                    .doesNotContain("public static final Pattern ID_PATTERN");
        }

        @Test
        public void testWithValidPattern() {
            JavaFileObject file = forResource("definition/TestIdValidPattern.java");

            assertThat(compile(file))
                    .has(succeededWithoutWarnings())
                    .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                    .singleElement()
                    .has(sourceFileNamed("definition", "TestIdValidPatternLoader.java"))
                    .extracting(Compilations::contentsAsUtf8String, STRING)
                    .contains(
                            "public static final Pattern ID_PATTERN = Pattern.compile(\"^[A-Z0-9]+(?:_[A-Z0-9]+)*$\");",
                            "private final Predicate<TestIdValidPattern> filter = o -> ID_PATTERN.matcher(o.getName()).matches()",
                            ".filter(filter)"
                    );
        }

        @Test
        public void testInvalidPattern() {
            JavaFileObject file = forResource("definition/TestIdInvalidPattern.java");

            assertThat(compile(file))
                    .has(failed())
                    .extracting(Compilation::errors, DIAGNOSTICS)
                    .singleElement()
                    .returns("[RULE_I7] Id pattern must be valid", Compilations::getDefaultMessage)
                    .returns(file, Diagnostic::getSource)
                    .returns(10L, Diagnostic::getLineNumber);
        }
    }

    @Nested
    class BackendTest {

        @Test
        public void testNetBeans() {
            assertThat(compile(forResource("definition/TestBackendNetBeans.java")))
                    .has(succeededWithoutWarnings())
                    .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                    .filteredOn(sourceFileNamed("definition", "TestBackendNetBeansLoader.java"))
                    .singleElement()
                    .extracting(Compilations::contentsAsUtf8String, STRING)
                    .contains("private final Iterable<TestBackendNetBeans> source = TestBackendNetBeans.NetBeansLookup.INSTANCE.apply(TestBackendNetBeans.class);");
        }

        @Test
        public void testNonAssignable() {
            JavaFileObject file = forResource("definition/TestBackendNonAssignable.java");

            assertThat(compile(file))
                    .has(failed())
                    .extracting(Compilation::errors, DIAGNOSTICS)
                    .singleElement()
                    .returns("Backend 'definition.TestBackendNonAssignable.HelloProc' doesn't extend nor implement 'java.util.function.Function<java.lang.Class,? extends java.lang.Iterable>'", Compilations::getDefaultMessage)
                    .returns(file, Diagnostic::getSource)
                    .returns(11L, Diagnostic::getLineNumber);
        }
    }

    @Nested
    class BatchTest {

        @Test
        public void testNestedNames() {
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
        }

        @Test
        public void testNonNested() {
            JavaFileObject file = forResource("definition/TestBatchNonNested.java");
            Compilation compilation = compile(file);

            assertThat(compilation)
                    .has(succeededWithoutWarnings());

            assertThat(compilation)
                    .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                    .filteredOn(sourceFileNamed("definition", "TestBatchNonNestedLoader.java"))
                    .singleElement()
                    .extracting(Compilations::contentsAsUtf8String, STRING)
                    .contains(
                            "private final Iterable<TestBatchNonNested> source = ServiceLoader.load(TestBatchNonNested.class);",
                            "private final Iterable<TestBatchNonNestedBatch> batch = ServiceLoader.load(TestBatchNonNestedBatch.class);"
                    );

            assertThat(compilation)
                    .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                    .filteredOn(sourceFileNamed("definition", "TestBatchNonNestedBatch.java"))
                    .singleElement()
                    .extracting(Compilations::contentsAsUtf8String, STRING)
                    .contains(
                            "Stream<TestBatchNonNested> getProviders();"
                    );

            assertThat(compilation)
                    .extracting(Compilation::generatedFiles, JAVA_FILE_OBJECTS)
                    .filteredOn(fileNamed("/CLASS_OUTPUT/META-INF/services/definition.TestBatchNonNestedBatch"))
                    .singleElement()
                    .extracting(Compilations::contentsAsUtf8String, STRING)
                    .contains(
                            "definition.TestBatchNonNested$ABC"
                    );
        }

        @Test
        public void testNested() {
            JavaFileObject file = forResource("definition/TestBatchNested.java");
            Compilation compilation = compile(file);

            assertThat(compilation)
                    .has(succeededWithoutWarnings());

            assertThat(compilation)
                    .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                    .filteredOn(sourceFileNamed("definition", "TestBatchNestedLoader.java"))
                    .singleElement()
                    .extracting(Compilations::contentsAsUtf8String, STRING)
                    .contains(
                            "private final Iterable<TestBatchNested.HelloService> source = ServiceLoader.load(TestBatchNested.HelloService.class);",
                            "private final Iterable<TestBatchNestedBatch.HelloService> batch = ServiceLoader.load(TestBatchNestedBatch.HelloService.class);"
                    );

            assertThat(compilation)
                    .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                    .filteredOn(sourceFileNamed("definition", "TestBatchNestedBatch.java"))
                    .singleElement()
                    .extracting(Compilations::contentsAsUtf8String, STRING)
                    .contains(
                            "Stream<TestBatchNested.HelloService> getProviders();"
                    );

            assertThat(compilation)
                    .extracting(Compilation::generatedFiles, JAVA_FILE_OBJECTS)
                    .filteredOn(fileNamed("/CLASS_OUTPUT/META-INF/services/definition.TestBatchNestedBatch$HelloService"))
                    .singleElement()
                    .extracting(Compilations::contentsAsUtf8String, STRING)
                    .contains(
                            "definition.TestBatchNested$ABC"
                    );
        }

        @Test
        public void testReloading() {
            assertThat(compile(forResource("definition/TestBatchReloading.java")))
                    .has(succeeded())
                    .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                    .filteredOn(sourceFileNamed("definition", "TestBatchReloadingLoader.java"))
                    .singleElement()
                    .extracting(Compilations::contentsAsUtf8String, STRING)
                    .isEqualToIgnoringNewLines(contentsAsUtf8String(forResource("definition/expected/TestBatchReloadingLoader.java")));
        }
    }

    @Nested
    class FallbackTest {

        @Test
        public void testNonInstantiable() {
            JavaFileObject file = forResource("definition/TestFallbackNonInstantiable.java");
            Compilation compilation = compile(file);

            assertThat(compilation)
                    .has(failed());

            assertThat(compilation)
                    .extracting(Compilation::errors, DIAGNOSTICS)
                    .hasSize(1)
                    .extracting(Compilations::getDefaultMessage, Diagnostic::getSource, Diagnostic::getLineNumber)
                    .containsOnly(
                            tuple("Don't know how to instantiate 'definition.TestFallbackNonInstantiable.SomeFallback'", file, 9L)
                    );
        }

        @Test
        public void testNonAssignable() {
            JavaFileObject file = forResource("definition/TestFallbackNonAssignable.java");
            Compilation compilation = compile(file);

            assertThat(compilation)
                    .has(failed());

            assertThat(compilation)
                    .extracting(Compilation::errors, DIAGNOSTICS)
                    .hasSize(1)
                    .extracting(Compilations::getDefaultMessage, Diagnostic::getSource, Diagnostic::getLineNumber)
                    .containsOnly(
                            tuple("Fallback 'java.lang.String' doesn't extend nor implement service 'definition.TestFallbackNonAssignable.HelloService'", file, 8L)
                    );
        }

        @Test
        public void testMissing() {
            JavaFileObject file = forResource("definition/TestFallbackMissing.java");
            Compilation compilation = compile(file);

            assertThat(compilation)
                    .has(succeeded());

            assertThat(compilation)
                    .extracting(Compilation::warnings, DIAGNOSTICS)
                    .hasSize(1)
                    .extracting(Compilations::getDefaultMessage, Diagnostic::getSource, Diagnostic::getLineNumber)
                    .containsOnly(
                            tuple("Missing fallback for service 'definition.TestFallbackMissing'", file, 7L)
                    );

            assertThat(compilation)
                    .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                    .singleElement()
                    .has(sourceFileNamed("definition", "TestFallbackMissingLoader.java"));
        }

        @Test
        public void testUnexpected() {
            JavaFileObject file = forResource("definition/TestFallbackUnexpected.java");
            Compilation compilation = compile(file);

            assertThat(compilation)
                    .has(succeededWithoutWarnings());

            assertThat(compilation)
                    .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                    .singleElement()
                    .has(sourceFileNamed("definition", "TestFallbackUnexpectedLoader.java"));
        }
    }

    @Nested
    class WrapperTest {

        @Test
        public void testValid() {
            JavaFileObject file = forResource("definition/TestWrapperValid.java");
            Compilation compilation = compile(file);

            assertThat(compilation)
                    .has(succeededWithoutWarnings());

            assertThat(compilation)
                    .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                    .singleElement()
                    .has(sourceFileNamed("definition", "TestWrapperValidLoader.java"))
                    .extracting(Compilations::contentsAsUtf8String, STRING)
                    .contains(
                            "private final Optional<TestWrapperValid.ByConstructor> resource = doLoad();",
                            ".map(TestWrapperValid.WrapperByConstructor::new)",
                            "private final Optional<TestWrapperValid.ByStaticMethod> resource = doLoad();",
                            ".map(TestWrapperValid.WrapperByStaticMethod::wrap)",
                            "private final Optional<TestWrapperValid.ByStaticMethodX> resource = doLoad();",
                            ".map(TestWrapperValid.WrapperByStaticMethodX::wrap)"
                    );
        }

        @Test
        public void testNonAssignable() {
            JavaFileObject file = forResource("definition/TestWrapperNonAssignable.java");
            Compilation compilation = compile(file);

            assertThat(compilation)
                    .has(failed());

            assertThat(compilation)
                    .extracting(Compilation::errors, DIAGNOSTICS)
                    .hasSize(1)
                    .extracting(Compilations::getDefaultMessage, Diagnostic::getSource, Diagnostic::getLineNumber)
                    .containsOnly(
                            tuple("Wrapper 'java.lang.String' doesn't extend nor implement service 'definition.TestWrapperNonAssignable.HelloService'", file, 8L)
                    );
        }

        @Test
        public void testNonInstantiable() {
            JavaFileObject file = forResource("definition/TestWrapperNonInstantiable.java");
            Compilation compilation = compile(file);

            assertThat(compilation)
                    .has(failed());

            assertThat(compilation)
                    .extracting(Compilation::errors, DIAGNOSTICS)
                    .hasSize(4)
                    .extracting(Compilations::getDefaultMessage, Diagnostic::getSource, Diagnostic::getLineNumber)
                    .containsOnly(
                            tuple("Don't know how to wrap 'definition.TestWrapperNonInstantiable.WrapperByPrivateConstructor'", file, 8L),
                            tuple("Don't know how to wrap 'definition.TestWrapperNonInstantiable.WrapperByNoArgConstructor'", file, 18L),
                            tuple("Don't know how to wrap 'definition.TestWrapperNonInstantiable.WrapperByInvalidArgConstructor'", file, 28L),
                            tuple("Don't know how to wrap 'definition.TestWrapperNonInstantiable.WrapperByToManyStaticMethods'", file, 38L)
                    );
        }
    }

    @Nested
    class PreprocessingTest {

        @Test
        public void testNonInstantiable() {
            JavaFileObject file = forResource("definition/TestPreprocessorNonInstantiable.java");
            Compilation compilation = compile(file);

            assertThat(compilation)
                    .has(failed());

            assertThat(compilation)
                    .extracting(Compilation::errors, DIAGNOSTICS)
                    .hasSize(1)
                    .extracting(Compilations::getDefaultMessage, Diagnostic::getSource, Diagnostic::getLineNumber)
                    .containsOnly(
                            tuple("Don't know how to instantiate 'definition.TestPreprocessorNonInstantiable.SomePreprocessor'", file, 11L)
                    );
        }

        @Test
        public void testNonAssignable() {
            JavaFileObject file = forResource("definition/TestPreprocessorNonAssignable.java");
            Compilation compilation = compile(file);

            assertThat(compilation)
                    .has(failed());

            assertThat(compilation)
                    .extracting(Compilation::errors, DIAGNOSTICS)
                    .hasSize(1)
                    .extracting(Compilations::getDefaultMessage, Diagnostic::getSource, Diagnostic::getLineNumber)
                    .containsOnly(
                            tuple("Preprocessor 'definition.TestPreprocessorNonAssignable.HelloProc' doesn't extend nor implement 'java.util.function.UnaryOperator<java.util.stream.Stream<definition.TestPreprocessorNonAssignable.HelloService>>'", file, 10L)
                    );
        }
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

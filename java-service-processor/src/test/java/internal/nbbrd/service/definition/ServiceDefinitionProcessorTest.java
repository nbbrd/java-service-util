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
                    .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                    .singleElement()
                    .has(sourceFileNamed("definition", "TestQuantifierSingleLoader.java"))
                    .extracting(Compilations::contentsAsUtf8String, STRING)
                    .contains(
                            "public static TestQuantifierSingle.Mutable load()"
                    );
        }

        @Test
        public void testOptional() {
            JavaFileObject file = forResource("definition/TestQuantifierOptional.java");
            Compilation compilation = compile(file);

            assertThat(compilation)
                    .has(succeeded());

            assertThat(compilation)
                    .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                    .singleElement()
                    .has(sourceFileNamed("definition", "TestQuantifierOptionalLoader.java"))
                    .extracting(Compilations::contentsAsUtf8String, STRING)
                    .contains(
                            "public static Optional<TestQuantifierOptional.Mutable> load()"
                    );
        }

        @Test
        public void testMultiple() {
            JavaFileObject file = forResource("definition/TestQuantifierMultiple.java");
            Compilation compilation = compile(file);

            assertThat(compilation)
                    .has(succeeded());

            assertThat(compilation)
                    .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                    .singleElement()
                    .has(sourceFileNamed("definition", "TestQuantifierMultipleLoader.java"))
                    .extracting(Compilations::contentsAsUtf8String, STRING)
                    .contains(
                            "public static List<TestQuantifierMultiple.Mutable> load()"
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
                    .hasSize(2)
                    .haveAtLeastOne(sourceFileNamed("definition", "TestLoaderNameValidLoader.java"))
                    .haveAtLeastOne(sourceFileNamed("internal", "FooLoader.java"));
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
                            "private final Predicate<TestFilterValid.SingleFilter> filter = TestFilterValid.SingleFilter::isAvailable",
                            "private final Predicate<TestFilterValid.MultiFilter> filter = ((Predicate<TestFilterValid.MultiFilter>)TestFilterValid.MultiFilter::isAvailable).and(TestFilterValid.MultiFilter::isFastEnough)",
                            "private final Predicate<TestFilterValid.ReversedFilter> filter = ((Predicate<TestFilterValid.ReversedFilter>)TestFilterValid.ReversedFilter::isAvailable).negate()",
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
                            "Comparator.comparingInt(TestSorterValid.IntSorter::getCost)",
                            "Comparator.comparingLong(TestSorterValid.LongSorter::getCost)",
                            "Comparator.comparingDouble(TestSorterValid.DoubleSorter::getCost)",
                            "Comparator.comparing(TestSorterValid.ComparableSorter::getCost)",
                            "((Comparator<TestSorterValid.MultiSorter>)Comparator.comparingInt(TestSorterValid.MultiSorter::getCost)).thenComparing(Comparator.comparingDouble(TestSorterValid.MultiSorter::getAccuracy))",
                            "Collections.reverseOrder(Comparator.comparingInt(TestSorterValid.ReversedSorter::getCost))",
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
    class BatchTest {

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

        @Test
        public void testValidType() {
            JavaFileObject file = forResource("definition/TestBatchValidType.java");
            Compilation compilation = compile(file);

            assertThat(compilation)
//                    .has(succeededWithoutWarnings())
                    .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                    .singleElement()
                    .extracting(Compilations::contentsAsUtf8String, STRING)
                    .contains(
                            "private final Iterable<?> batchSource;",
                            "private final Runnable batchReloader;",
                            "Stream.concat(",
                            "StreamSupport.stream(providerSource.spliterator(), false)",
                            "StreamSupport.stream(batchSource.spliterator(), false)",
                            ".flatMap(o -> o.getProviders()))"
                    );
        }

        @Test
        public void testInvalidType() {
            JavaFileObject file = forResource("definition/TestBatchInvalidType.java");

            assertThat(compile(file))
                    .has(failed())
                    .extracting(Compilation::errors, DIAGNOSTICS)
                    .singleElement()
                    .returns("[RULE_B1] Batch type must be an interface or an abstract class", Compilations::getDefaultMessage)
                    .returns(file, Diagnostic::getSource)
                    .returns(8L, Diagnostic::getLineNumber);
        }

        @Test
        public void testMethodUnique() {
            JavaFileObject file = forResource("definition/TestBatchTypeMethodUnique.java");

            assertThat(compile(file))
                    .has(failed())
                    .extracting(Compilation::errors, DIAGNOSTICS)
                    .singleElement()
                    .returns("[RULE_B2] Batch method must be unique", Compilations::getDefaultMessage)
                    .returns(file, Diagnostic::getSource)
                    .returns(8L, Diagnostic::getLineNumber);
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
        public void testSuppressWarning() {
            JavaFileObject file = forResource("definition/TestFallbackSuppressWarning.java");
            Compilation compilation = compile(file);

            assertThat(compilation)
                    .has(succeededWithoutWarnings());

            assertThat(compilation)
                    .extracting(Compilation::generatedSourceFiles, JAVA_FILE_OBJECTS)
                    .singleElement()
                    .has(sourceFileNamed("definition", "TestFallbackSuppressWarningLoader.java"));
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

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

import com.google.testing.compile.Compilation;
import static com.google.testing.compile.CompilationSubject.assertThat;
import com.google.testing.compile.JavaFileObjects;
import com.squareup.javapoet.ClassName;
import static internal.nbbrd.service.definition.ServiceDefinitionProcessor.resolveLoaderName;
import javax.tools.JavaFileObject;
import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 *
 * @author Philippe Charles
 */
public class ServiceDefinitionProcessorTest {

    @Test
    public void testResolveLoaderName() {
        Assertions.assertThat(resolveLoaderName("", ClassName.get("hello", "World"))).isEqualTo(ClassName.get("hello", "WorldLoader"));
        Assertions.assertThat(resolveLoaderName("", ClassName.get("hello", "World", "Nested"))).isEqualTo(ClassName.get("hello", "WorldLoader", "Nested"));
    }

    @Test
    public void testNonNestedDef() {
        Compilation compilation = com.google.testing.compile.Compiler.javac()
                .withProcessors(new ServiceDefinitionProcessor())
                .compile(JavaFileObjects.forResource("definition/NonNestedDef.java"));

        assertThat(compilation)
                .succeeded();

        assertThat(compilation)
                .generatedSourceFile("definition.NonNestedDefLoader")
                .contentsAsUtf8String();
    }

    @Test
    public void testNestedDef() {
        Compilation compilation = com.google.testing.compile.Compiler.javac()
                .withProcessors(new ServiceDefinitionProcessor())
                .compile(JavaFileObjects.forResource("definition/NestedDef.java"));

        assertThat(compilation)
                .succeeded();

        assertThat(compilation)
                .generatedSourceFile("definition.NestedDefLoader")
                .contentsAsUtf8String()
                .contains("public static final class HelloService {");
    }

    @Test
    public void testSingleDef() {
        Compilation compilation = com.google.testing.compile.Compiler.javac()
                .withProcessors(new ServiceDefinitionProcessor())
                .compile(JavaFileObjects.forResource("definition/SingleDef.java"));

        assertThat(compilation)
                .succeeded();

        assertThat(compilation)
                .generatedSourceFile("definition.SingleDefLoader")
                .contentsAsUtf8String()
                .contains("private final SingleDef.Immutable resource = doLoad();");

        assertThat(compilation)
                .generatedSourceFile("definition.SingleDefLoader")
                .contentsAsUtf8String()
                .contains("public static SingleDef.Immutable load()");

        assertThat(compilation)
                .generatedSourceFile("definition.SingleDefLoader")
                .contentsAsUtf8String()
                .contains("private SingleDef.Mutable resource = doLoad();");

        assertThat(compilation)
                .generatedSourceFile("definition.SingleDefLoader")
                .contentsAsUtf8String()
                .contains("private final AtomicReference<SingleDef.ThreadSafe> resource = new AtomicReference<>(doLoad());");

        assertThat(compilation)
                .generatedSourceFile("definition.SingleDefLoader")
                .contentsAsUtf8String()
                .contains("private static final SingleDef.ImmutableSingleton RESOURCE = doLoad();");

        assertThat(compilation)
                .generatedSourceFile("definition.SingleDefLoader")
                .contentsAsUtf8String()
                .contains("private static SingleDef.MutableSingleton RESOURCE = doLoad();");

        assertThat(compilation)
                .hadWarningContaining("Thread-unsafe singleton for 'definition.SingleDef.MutableSingleton'");

        assertThat(compilation)
                .generatedSourceFile("definition.SingleDefLoader")
                .contentsAsUtf8String()
                .contains("private static final AtomicReference<SingleDef.ThreadSafeSingleton> RESOURCE = new AtomicReference<>(doLoad());");
    }

    @Test
    public void testOptionalDef() {
        Compilation compilation = com.google.testing.compile.Compiler.javac()
                .withProcessors(new ServiceDefinitionProcessor())
                .compile(JavaFileObjects.forResource("definition/OptionalDef.java"));

        assertThat(compilation)
                .succeeded();

        assertThat(compilation)
                .generatedSourceFile("definition.OptionalDefLoader")
                .contentsAsUtf8String()
                .contains("private final Optional<OptionalDef.Immutable> resource = doLoad();");

        assertThat(compilation)
                .generatedSourceFile("definition.OptionalDefLoader")
                .contentsAsUtf8String()
                .contains("public static Optional<OptionalDef.Immutable> load()");

        assertThat(compilation)
                .generatedSourceFile("definition.OptionalDefLoader")
                .contentsAsUtf8String()
                .contains("private Optional<OptionalDef.Mutable> resource = doLoad();");

        assertThat(compilation)
                .generatedSourceFile("definition.OptionalDefLoader")
                .contentsAsUtf8String()
                .contains("private final AtomicReference<Optional<OptionalDef.ThreadSafe>> resource = new AtomicReference<>(doLoad());");

        assertThat(compilation)
                .generatedSourceFile("definition.OptionalDefLoader")
                .contentsAsUtf8String()
                .contains("private static final Optional<OptionalDef.ImmutableSingleton> RESOURCE = doLoad();");

        assertThat(compilation)
                .generatedSourceFile("definition.OptionalDefLoader")
                .contentsAsUtf8String()
                .contains("private static Optional<OptionalDef.MutableSingleton> RESOURCE = doLoad();");

        assertThat(compilation)
                .hadWarningContaining("Thread-unsafe singleton for 'definition.OptionalDef.MutableSingleton'");

        assertThat(compilation)
                .generatedSourceFile("definition.OptionalDefLoader")
                .contentsAsUtf8String()
                .contains("private static final AtomicReference<Optional<OptionalDef.ThreadSafeSingleton>> RESOURCE = new AtomicReference<>(doLoad());");
    }

    @Test
    public void testMultipleDef() {
        Compilation compilation = com.google.testing.compile.Compiler.javac()
                .withProcessors(new ServiceDefinitionProcessor())
                .compile(JavaFileObjects.forResource("definition/MultipleDef.java"));

        assertThat(compilation)
                .succeeded();

        assertThat(compilation)
                .generatedSourceFile("definition.MultipleDefLoader")
                .contentsAsUtf8String()
                .contains("private final List<MultipleDef.Immutable> resource = doLoad();");

        assertThat(compilation)
                .generatedSourceFile("definition.MultipleDefLoader")
                .contentsAsUtf8String()
                .contains("public static List<MultipleDef.Immutable> load()");

        assertThat(compilation)
                .generatedSourceFile("definition.MultipleDefLoader")
                .contentsAsUtf8String()
                .contains("private List<MultipleDef.Mutable> resource = doLoad();");

        assertThat(compilation)
                .generatedSourceFile("definition.MultipleDefLoader")
                .contentsAsUtf8String()
                .contains("private final AtomicReference<List<MultipleDef.ThreadSafe>> resource = new AtomicReference<>(doLoad());");

        assertThat(compilation)
                .generatedSourceFile("definition.MultipleDefLoader")
                .contentsAsUtf8String()
                .contains("private static final List<MultipleDef.ImmutableSingleton> RESOURCE = doLoad();");

        assertThat(compilation)
                .generatedSourceFile("definition.MultipleDefLoader")
                .contentsAsUtf8String()
                .contains("private static List<MultipleDef.MutableSingleton> RESOURCE = doLoad();");

        assertThat(compilation)
                .hadWarningContaining("Thread-unsafe singleton for 'definition.MultipleDef.MutableSingleton'");

        assertThat(compilation)
                .generatedSourceFile("definition.MultipleDefLoader")
                .contentsAsUtf8String()
                .contains("private static final AtomicReference<List<MultipleDef.ThreadSafeSingleton>> RESOURCE = new AtomicReference<>(doLoad());");
    }

    @Test
    public void testAlternateFactories() {
        Compilation compilation = com.google.testing.compile.Compiler.javac()
                .withProcessors(new ServiceDefinitionProcessor())
                .compile(JavaFileObjects.forResource("definition/AlternateFactories.java"));

        assertThat(compilation)
                .succeeded();
    }

    @Test
    public void testUnknownFactory() {
        Compilation compilation = com.google.testing.compile.Compiler.javac()
                .withProcessors(new ServiceDefinitionProcessor())
                .compile(JavaFileObjects.forResource("definition/UnknownFactory.java"));

        assertThat(compilation)
                .failed();

        assertThat(compilation)
                .hadErrorContaining("Don't know how to create");
    }

    @Test
    public void testNonAssignableFallback() {
        JavaFileObject file = JavaFileObjects.forResource("definition/NonAssignableFallback.java");

        Compilation compilation = com.google.testing.compile.Compiler.javac()
                .withProcessors(new ServiceDefinitionProcessor())
                .compile(file);

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

        Compilation compilation = com.google.testing.compile.Compiler.javac()
                .withProcessors(new ServiceDefinitionProcessor())
                .compile(file);

        assertThat(compilation)
                .failed();

        assertThat(compilation)
                .hadErrorContaining("doesn't extend nor implement")
                .inFile(file)
                .onLine(10);
    }
}
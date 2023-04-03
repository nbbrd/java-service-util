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
package internal.nbbrd.service.provider;

import _test.Compilations;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;

import javax.annotation.processing.Processor;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ServiceLoader;

import static _test.Compilations.*;
import static com.google.testing.compile.JavaFileObjects.forResource;
import static com.google.testing.compile.JavaFileObjects.forSourceLines;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.assertj.core.groups.Tuple.tuple;

/**
 * @author Philippe Charles
 */
public class ServiceProviderProcessorTest {

    @Test
    public void testRegistration() {
        assertThat(ServiceLoader.load(Processor.class))
                .hasAtLeastOneElementOfType(ServiceProviderProcessor.class);
    }

    @Test
    public void testWithoutAnnotation() {
        Compilation compilation = compile(forResource("provider/WithoutAnnotation.java"));

        assertThat(compilation)
                .has(succeeded());
    }

    @Test
    public void testWithAnnotation() {
        Compilation compilation = compile(forResource("provider/WithAnnotation.java"));

        assertThat(compilation)
                .has(succeeded());

        assertThat(compilation)
                .extracting(Compilation::generatedFiles, JAVA_FILE_OBJECTS)
                .filteredOn(fileNamed("/CLASS_OUTPUT/META-INF/services/provider.WithAnnotation$HelloService"))
                .singleElement()
                .extracting(Compilations::contentsAsUtf8String, STRING)
                .contains(
                        "provider.WithAnnotation$Provider1",
                        "provider.WithAnnotation$Provider2"
                );
    }

    @Test
    public void testWithRepeatedAnnotation() {
        Compilation compilation = compile(forResource("provider/WithRepeatedAnnotation.java"));

        assertThat(compilation)
                .has(succeeded());

        assertThat(compilation)
                .extracting(Compilation::generatedFiles, JAVA_FILE_OBJECTS)
                .filteredOn(fileNamed("/CLASS_OUTPUT/META-INF/services/WithRepeatedAnnotation$HelloService"))
                .singleElement()
                .extracting(Compilations::contentsAsUtf8String, STRING)
                .contains(
                        "WithRepeatedAnnotation$SimpleProvider",
                        "WithRepeatedAnnotation$MultiProvider"
                );

        assertThat(compilation)
                .extracting(Compilation::generatedFiles, JAVA_FILE_OBJECTS)
                .filteredOn(fileNamed("/CLASS_OUTPUT/META-INF/services/WithRepeatedAnnotation$SomeService"))
                .singleElement()
                .extracting(Compilations::contentsAsUtf8String, STRING)
                .contains("WithRepeatedAnnotation$MultiProvider")
                .doesNotContain("WithRepeatedAnnotation$SimpleProvider");
    }

    @Test
    public void testMissingImplementation() {
        JavaFileObject file = forResource("provider/MissingImplementation.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .has(failed());

        assertThat(compilation)
                .extracting(Compilation::errors, DIAGNOSTICS)
                .hasSize(1)
                .extracting(Compilations::getDefaultMessage, Diagnostic::getSource, Diagnostic::getLineNumber)
                .containsOnly(
                        tuple("Provider 'MissingImplementation.Provider2' doesn't extend nor implement service 'MissingImplementation.HelloService'", file, 14L)
                );
    }

    @Test
    public void testStaticInnerClass() {
        JavaFileObject file = forResource("provider/StaticInnerClass.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .has(failed());

        assertThat(compilation)
                .extracting(Compilation::errors, DIAGNOSTICS)
                .hasSize(1)
                .extracting(Compilations::getDefaultMessage, Diagnostic::getSource, Diagnostic::getLineNumber)
                .containsOnly(
                        tuple("Provider 'StaticInnerClass.Provider2' must be static inner class", file, 14L)
                );
    }

    @Test
    public void testAbstractClass() {
        JavaFileObject file = forResource("provider/AbstractClass.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .has(failed());

        assertThat(compilation)
                .extracting(Compilation::errors, DIAGNOSTICS)
                .hasSize(1)
                .extracting(Compilations::getDefaultMessage, Diagnostic::getSource, Diagnostic::getLineNumber)
                .containsOnly(
                        tuple("Provider 'AbstractClass.Provider2' must not be abstract", file, 14L)
                );
    }

    @Test
    public void testPublicNoArgumentConstructor() {
        JavaFileObject file = forResource("provider/PublicNoArgumentConstructor.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .has(failed());

        assertThat(compilation)
                .extracting(Compilation::errors, DIAGNOSTICS)
                .hasSize(1)
                .extracting(Compilations::getDefaultMessage, Diagnostic::getSource, Diagnostic::getLineNumber)
                .containsOnly(
                        tuple("Provider 'PublicNoArgumentConstructor.Provider2' must have a public no-argument constructor", file, 14L)
                );
    }

    @Test
    public void testStaticProviderMethod() {
        JavaFileObject file = forResource("provider/StaticProviderMethod.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .has(failed());

        assertThat(compilation)
                .extracting(Compilation::errors, DIAGNOSTICS)
                .hasSize(2)
                .extracting(Compilations::getDefaultMessage, Diagnostic::getSource, Diagnostic::getLineNumber)
                .containsOnly(
                        tuple("Static method support not implemented yet", file, 10L),
                        tuple("Static method support not implemented yet", file, 21L)
                );
    }

    @Test
    public void testStaticNoProviderMethod() {
        JavaFileObject file = forResource("provider/StaticNoProviderMethod.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .has(failed());

        assertThat(compilation)
                .extracting(Compilation::errors, DIAGNOSTICS)
                .hasSize(1)
                .extracting(Compilations::getDefaultMessage, Diagnostic::getSource, Diagnostic::getLineNumber)
                .containsOnly(
                        tuple("Provider 'StaticNoProviderMethod.Provider' must have a public no-argument constructor", file, 10L)
                );
    }

    @Test
    public void testStaticMultiProviderMethod() {
        JavaFileObject file = forResource("provider/StaticMultiProviderMethod.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .has(failed());

        assertThat(compilation)
                .extracting(Compilation::errors, DIAGNOSTICS)
                .hasSize(1)
                .extracting(Compilations::getDefaultMessage, Diagnostic::getSource, Diagnostic::getLineNumber)
                .containsOnly(
                        tuple("Static method support not implemented yet", file, 10L)
                );
    }

    @Test
    public void testDuplicatedAnnotation() {
        JavaFileObject file = forResource("provider/DuplicatedAnnotation.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .has(failed());

        assertThat(compilation)
                .extracting(Compilation::errors, DIAGNOSTICS)
                .hasSize(1)
                .extracting(Compilations::getDefaultMessage, Diagnostic::getSource, Diagnostic::getLineNumber)
                .containsOnly(
                        tuple("Duplicated provider: 'DuplicatedAnnotation.Provider1'", file, 11L)
                );
    }

    @Test
    public void testWithGenerics() {
        JavaFileObject file = forResource("provider/WithGenerics.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .has(succeeded());

        assertThat(compilation)
                .extracting(Compilation::generatedFiles, JAVA_FILE_OBJECTS)
                .filteredOn(fileNamed("/CLASS_OUTPUT/META-INF/services/WithGenerics$HelloService"))
                .singleElement()
                .extracting(Compilations::contentsAsUtf8String, STRING)
                .contains(
                        "WithGenerics$Provider1",
                        "WithGenerics$Provider2"
                );
    }

    @Test
    public void testInferredService() {
        JavaFileObject file = forResource("provider/InferredService.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .has(succeeded());

        assertThat(compilation)
                .extracting(Compilation::generatedFiles, JAVA_FILE_OBJECTS)
                .filteredOn(fileNamed("/CLASS_OUTPUT/META-INF/services/InferredService$HelloService"))
                .singleElement()
                .extracting(Compilations::contentsAsUtf8String, STRING)
                .contains("InferredService$Provider1");
    }

    @Test
    public void testVoidService() {
        JavaFileObject file = forResource("provider/VoidService.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .has(failed());

        assertThat(compilation)
                .extracting(Compilation::errors, DIAGNOSTICS)
                .hasSize(1)
                .extracting(Compilations::getDefaultMessage, Diagnostic::getSource, Diagnostic::getLineNumber)
                .containsOnly(
                        tuple("Cannot infer service from provider ", file, 11L)
                );
    }

    @Test
    @DisabledOnJre(JRE.JAVA_8)
    @Disabled("https://github.com/google/compile-testing/issues/335#issuecomment-1269011171")
    public void testModuleInfoWithoutAnnotation() {
        Compilation compilation = compile(
                forResource("provider/WithoutAnnotation.java"),
                forSourceLines("module-info",
                        "module xxx {",
                        "  exports provider;",
                        "  provides provider.WithoutAnnotation.HelloService with provider.WithoutAnnotation.Provider1;",
                        "}"
                ));

        assertThat(compilation)
                .has(succeededWithoutWarnings());
    }

    @Test
    @DisabledOnJre(JRE.JAVA_8)
    @Disabled("https://github.com/google/compile-testing/issues/335#issuecomment-1269011171")
    public void testModuleInfoWithAnnotation() {
        Compilation compilation = compile(
                forResource(fixPackageNotVisible()),
                forResource("provider/WithAnnotation.java"),
                forSourceLines("module-info",
                        "module xxx {",
                        "  exports provider;",
                        "  provides provider.WithAnnotation.HelloService with provider.WithAnnotation.Provider1;",
                        "}"
                ));

        assertThat(compilation)
                .has(succeededWithoutWarnings());
    }

    @Test
    public void testClassPathOrder() {
        JavaFileObject file = forResource("provider/ClassPathOrder.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .has(succeeded());

        assertThat(compilation)
                .extracting(Compilation::generatedFiles, JAVA_FILE_OBJECTS)
                .filteredOn(fileNamed("/CLASS_OUTPUT/META-INF/services/ClassPathOrder$HelloService"))
                .singleElement()
                .extracting(Compilations::contentsAsUtf8String, STRING)
                .isEqualToIgnoringNewLines("ClassPathOrder$AClassPathOrder$BClassPathOrder$C");
    }

    private URL fixPackageNotVisible() {
        try {
            return Paths.get(System.getProperty("user.dir"))
                    .getParent()
                    .resolve("java-service-annotation")
                    .resolve("src")
                    .resolve("main")
                    .resolve("java")
                    .resolve("nbbrd")
                    .resolve("service")
                    .resolve("ServiceProvider.java")
                    .toUri().toURL();
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Compilation compile(JavaFileObject... files) {
        return Compiler.javac()
                .withProcessors(new ServiceProviderProcessor())
                .compile(files);
    }
}

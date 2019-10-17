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

import com.google.common.truth.StringSubject;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.CompilationRule;
import static com.google.testing.compile.CompilationSubject.assertThat;
import com.google.testing.compile.JavaFileObjects;
import static internal.nbbrd.service.provider.ServiceProviderProcessor.getMissingEntries;
import static internal.nbbrd.service.provider.ServiceProviderProcessor.getMissingRefs;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import java.util.List;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import org.assertj.core.api.Assertions;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;

/**
 *
 * @author Philippe Charles
 */
public class ServiceProviderProcessorTest {

    @Rule
    public CompilationRule compilationRule = new CompilationRule();

    @Test
    public void testWithoutAnnotation() {
        Compilation compilation = compile(JavaFileObjects.forResource("provider/WithoutAnnotation.java"));

        assertThat(compilation)
                .succeeded();
    }

    @Test
    public void testWithAnnotation() {
        Compilation compilation = compile(JavaFileObjects.forResource("provider/WithAnnotation.java"));

        assertThat(compilation)
                .succeeded();

        StringSubject content
                = assertThat(compilation)
                        .generatedFile(StandardLocation.CLASS_OUTPUT, "META-INF/services/provider.WithAnnotation$HelloService")
                        .contentsAsUtf8String();
        content.contains("provider.WithAnnotation$Provider1");
        content.contains("provider.WithAnnotation$Provider2");
    }

    @Test
    public void testWithRepeatedAnnotation() {
        Compilation compilation = compile(JavaFileObjects.forResource("provider/WithRepeatedAnnotation.java"));

        assertThat(compilation)
                .succeeded();

        StringSubject c1
                = assertThat(compilation)
                        .generatedFile(StandardLocation.CLASS_OUTPUT, "META-INF/services/WithRepeatedAnnotation$HelloService")
                        .contentsAsUtf8String();
        c1.contains("WithRepeatedAnnotation$SimpleProvider");
        c1.contains("WithRepeatedAnnotation$MultiProvider");

        StringSubject c2
                = assertThat(compilation)
                        .generatedFile(StandardLocation.CLASS_OUTPUT, "META-INF/services/WithRepeatedAnnotation$SomeService")
                        .contentsAsUtf8String();
        c2.contains("WithRepeatedAnnotation$MultiProvider");
        c2.doesNotContain("WithRepeatedAnnotation$SimpleProvider");
    }

    @Test
    public void testMissingImplementation() {
        JavaFileObject file = JavaFileObjects.forResource("provider/MissingImplementation.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .failed();

        assertThat(compilation)
                .hadErrorContaining("doesn't extend nor implement service")
                .inFile(file)
                .onLine(14);
    }

    @Test
    public void testStaticInnerClass() {
        JavaFileObject file = JavaFileObjects.forResource("provider/StaticInnerClass.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .failed();

        assertThat(compilation)
                .hadErrorContaining("must be static inner class")
                .inFile(file)
                .onLine(14);
    }

    @Test
    public void testAbstractClass() {
        JavaFileObject file = JavaFileObjects.forResource("provider/AbstractClass.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .failed();

        assertThat(compilation)
                .hadErrorContaining("must not be abstract")
                .inFile(file)
                .onLine(14);
    }

    @Test
    public void testPublicNoArgumentConstructor() {
        JavaFileObject file = JavaFileObjects.forResource("provider/PublicNoArgumentConstructor.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .failed();

        assertThat(compilation)
                .hadErrorContaining("must have a public no-argument constructor")
                .inFile(file)
                .onLine(14);
    }

    @Test
    public void testStaticProviderMethod() {
        JavaFileObject file = JavaFileObjects.forResource("provider/StaticProviderMethod.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .failed();

        assertThat(compilation)
                .hadErrorContaining("not implemented yet")
                .inFile(file)
                .onLine(10);
    }

    @Test
    public void testStaticNoProviderMethod() {
        JavaFileObject file = JavaFileObjects.forResource("provider/StaticNoProviderMethod.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .failed();

        assertThat(compilation)
                .hadErrorContaining("must have a public no-argument constructor")
                .inFile(file)
                .onLine(10);
    }

    @Test
    public void testStaticMultiProviderMethod() {
        JavaFileObject file = JavaFileObjects.forResource("provider/StaticMultiProviderMethod.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .failed();

        assertThat(compilation)
                .hadErrorContaining("not implemented yet")
                .inFile(file)
                .onLine(10);
    }

    @Test
    public void testDuplicatedAnnotation() {
        JavaFileObject file = JavaFileObjects.forResource("provider/DuplicatedAnnotation.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .failed();

        assertThat(compilation)
                .hadErrorContaining("Duplicated provider")
                .inFile(file)
                .onLine(11);
    }

    @Test
    public void testWithGenerics() {
        JavaFileObject file = JavaFileObjects.forResource("provider/WithGenerics.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .succeeded();

        StringSubject content
                = assertThat(compilation)
                        .generatedFile(StandardLocation.CLASS_OUTPUT, "META-INF/services/WithGenerics$HelloService")
                        .contentsAsUtf8String();
        content.contains("WithGenerics$Provider1");
        content.contains("WithGenerics$Provider2");
    }

    @Test
    public void testInferredService() {
        JavaFileObject file = JavaFileObjects.forResource("provider/InferredService.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .succeeded();

        StringSubject content
                = assertThat(compilation)
                        .generatedFile(StandardLocation.CLASS_OUTPUT, "META-INF/services/InferredService$HelloService")
                        .contentsAsUtf8String();
        content.contains("InferredService$Provider1");
    }

    @Test
    public void testVoidService() {
        JavaFileObject file = JavaFileObjects.forResource("provider/VoidService.java");
        Compilation compilation = compile(file);

        assertThat(compilation)
                .failed();

        assertThat(compilation)
                .hadErrorContaining("Cannot infer service")
                .inFile(file)
                .onLine(11);
    }

    @Test
    public void testMerge() {
        Assertions
                .assertThat(ServiceProviderProcessor.merge(asList("a", "b"), asList("c", "d")))
                .containsExactly("a", "b", "c", "d");

        Assertions
                .assertThat(ServiceProviderProcessor.merge(asList("a", "b"), asList("a", "d")))
                .containsExactly("a", "b", "d");

        Assertions
                .assertThat(ServiceProviderProcessor.merge(asList("a", "b"), asList("c", "a")))
                .containsExactly("a", "b", "c");

        Assertions
                .assertThat(ServiceProviderProcessor.merge(asList("a", "b"), asList("c", "c")))
                .containsExactly("a", "b", "c");
    }

    @Test
    public void testModuleInfoWithoutAnnotation() {
        assumeAtLeastJava9();

        Compilation compilation = compile(
                JavaFileObjects.forResource("provider/WithoutAnnotation.java"),
                JavaFileObjects.forSourceLines("module-info",
                        "module xxx {",
                        "  exports provider;",
                        "  provides provider.WithoutAnnotation.HelloService with provider.WithoutAnnotation.Provider1;",
                        "}"
                ));

        assertThat(compilation)
                .succeededWithoutWarnings();
    }

    @Test
    public void testModuleInfoWithAnnotation() {
        assumeAtLeastJava9();

        Compilation compilation = com.google.testing.compile.Compiler.javac()
                .withProcessors(new ServiceProviderProcessor())
                .compile(
                        JavaFileObjects.forResource(fixPackageNotVisible()),
                        JavaFileObjects.forResource("provider/WithAnnotation.java"),
                        JavaFileObjects.forSourceLines("module-info",
                                "module xxx {",
                                "  exports provider;",
                                "  provides provider.WithAnnotation.HelloService with provider.WithAnnotation.Provider1;",
                                "}"
                        ));

        assertThat(compilation)
                .succeededWithoutWarnings();
    }

    @Test
    public void testGetMissingRefs() {
        ProviderRef charRef = ref(CharSequence.class, String.class);
        ProviderEntry charEntry = entry(CharSequence.class, String.class);

        ProviderRef listRef = ref(List.class, ArrayList.class);
        ProviderEntry listEntry = entry(List.class, ArrayList.class);

        Assertions.assertThat(getMissingRefs(emptyList(), emptyList()))
                .isEmpty();

        Assertions.assertThat(getMissingRefs(emptyList(), asList(charEntry, listEntry)))
                .isEmpty();

        Assertions.assertThat(getMissingRefs(asList(charRef), asList(charEntry, listEntry)))
                .isEmpty();

        Assertions.assertThat(getMissingRefs(asList(listRef), asList(charEntry, listEntry)))
                .isEmpty();

        Assertions.assertThat(getMissingRefs(asList(charRef), asList(listEntry)))
                .containsExactly(charRef);

        Assertions.assertThat(getMissingRefs(asList(charRef), emptyList()))
                .containsExactly(charRef);
    }

    @Test
    public void testGetMissingEntries() {
        ProviderRef charRef = ref(CharSequence.class, String.class);
        ProviderEntry charEntry = entry(CharSequence.class, String.class);

        ProviderRef listRef = ref(List.class, ArrayList.class);
        ProviderEntry listEntry = entry(List.class, ArrayList.class);

        Assertions.assertThat(getMissingEntries(emptyList(), emptyList()))
                .isEmpty();

        Assertions.assertThat(getMissingEntries(emptyList(), asList(charEntry, listEntry)))
                .containsExactly(charEntry, listEntry);

        Assertions.assertThat(getMissingEntries(asList(charRef), asList(charEntry, listEntry)))
                .containsExactly(listEntry);

        Assertions.assertThat(getMissingEntries(asList(listRef), asList(charEntry, listEntry)))
                .containsExactly(charEntry);

        Assertions.assertThat(getMissingEntries(asList(charRef), asList(listEntry)))
                .containsExactly(listEntry);

        Assertions.assertThat(getMissingEntries(asList(charRef), emptyList()))
                .isEmpty();
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
        return com.google.testing.compile.Compiler.javac()
                .withProcessors(new ServiceProviderProcessor())
                .compile(files);
    }

    private void assumeAtLeastJava9() {
        Assume.assumeTrue(SourceVersion.latestSupported().compareTo(SourceVersion.RELEASE_8) > 0);
    }

    private TypeElement getTypeElement(Class<?> type) {
        return compilationRule.getElements().getTypeElement(type.getName());
    }

    private <T> ProviderRef ref(Class<T> service, Class<? extends T> provider) {
        return new ProviderRef(getTypeElement(service), getTypeElement(provider));
    }

    private <T> ProviderEntry entry(Class<T> service, Class<? extends T> provider) {
        return new ProviderEntry(service.getName(), provider.getName());
    }
}

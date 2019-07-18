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
package internal.nbbrd.service;

import com.google.common.truth.StringSubject;
import com.google.testing.compile.Compilation;
import static com.google.testing.compile.CompilationSubject.assertThat;
import com.google.testing.compile.JavaFileObjects;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import org.junit.Test;

/**
 *
 * @author Philippe Charles
 */
public class ServiceProviderProcessorTest {

    @Test
    public void testwithoutAnnotation() {
        JavaFileObject s1 = JavaFileObjects.forSourceString("HelloService", "interface HelloService { }");
        JavaFileObject p1 = JavaFileObjects.forSourceString("HelloProvider", "class HelloProvider implements HelloService {}");

        Compilation compilation = com.google.testing.compile.Compiler.javac()
                .withProcessors(new ServiceProviderProcessor())
                .compile(s1, p1);

        assertThat(compilation)
                .succeeded();
    }

    @Test
    public void testWithAnnotation() {
        JavaFileObject s1 = JavaFileObjects.forSourceString("HelloService", "interface HelloService { }");
        JavaFileObject p1 = JavaFileObjects.forSourceString("Provider1", "@nbbrd.service.ServiceProvider(HelloService.class) class Provider1 implements HelloService {}");
        JavaFileObject p2 = JavaFileObjects.forSourceString("Provider2", "@nbbrd.service.ServiceProvider(HelloService.class) class Provider2 implements HelloService {}");

        Compilation compilation = com.google.testing.compile.Compiler.javac()
                .withProcessors(new ServiceProviderProcessor())
                .compile(s1, p1, p2);

        assertThat(compilation)
                .succeeded();

        StringSubject content
                = assertThat(compilation)
                        .generatedFile(StandardLocation.CLASS_OUTPUT, "META-INF/services/HelloService")
                        .contentsAsUtf8String();
        content.contains("Provider1");
        content.contains("Provider2");
    }

    @Test
    public void testWithRepeatedAnnotation() {
        Compilation compilation = com.google.testing.compile.Compiler.javac()
                .withProcessors(new ServiceProviderProcessor())
                .compile(JavaFileObjects.forResource("WithRepeatedAnnotation.java"));

        assertThat(compilation)
                .succeeded();

        StringSubject c1
                = assertThat(compilation)
                        .generatedFile(StandardLocation.CLASS_OUTPUT, "META-INF/services/HelloService")
                        .contentsAsUtf8String();
        c1.contains("Provider1");
        c1.contains("Provider2");

        StringSubject c2
                = assertThat(compilation)
                        .generatedFile(StandardLocation.CLASS_OUTPUT, "META-INF/services/SomeService")
                        .contentsAsUtf8String();
        c2.contains("Provider1");
        c2.doesNotContain("Provider2");
    }
}

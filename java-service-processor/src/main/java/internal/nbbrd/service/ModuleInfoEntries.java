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

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.modules.ModuleDirective;

import javax.annotation.processing.Filer;
import javax.annotation.processing.FilerException;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * @author Philippe Charles
 */
@lombok.Value
@lombok.Builder
public class ModuleInfoEntries {

    @lombok.Singular
    List<String> usages;

    @lombok.Builder.Default
    Map<String, List<String>> provisions = Collections.emptyMap();

    public static class Builder {

        public Builder provision(String key, String value) {
            if (!provisions$set) {
                provisions$set = true;
                provisions$value = new HashMap<>();
            }
            provisions$value.computeIfAbsent(key, o -> new ArrayList<>()).add(value);
            return this;
        }
    }

    public static Optional<ModuleInfoEntries> parse(Filer filer, Elements elements) throws IOException {
        try {
            FileObject src = filer.getResource(StandardLocation.SOURCE_PATH, "", "module-info.java");
            return parse(src.getCharContent(false), PackageExpander.of(elements));
        } catch (FileNotFoundException | NoSuchFileException | FilerException | RuntimeException ex) {
            // ignore
            return Optional.empty();
        }
    }

    interface PackageExpander {

        Stream<String> getEnclosedTypeNames(String packageName);

        static PackageExpander of(Elements elements) {
            return packageName -> {
                PackageElement packageElement = elements.getPackageElement(packageName);
                return packageElement != null
                        ? packageElement
                        .getEnclosedElements()
                        .stream()
                        .filter(TypeElement.class::isInstance)
                        .map(TypeElement.class::cast)
                        .map(TypeElement::getQualifiedName)
                        .map(Name::toString)
                        : Stream.empty();
            };
        }
    }

    static Optional<ModuleInfoEntries> parse(CharSequence moduleInfoContent, PackageExpander expander) {
        try {
            return parse(StaticJavaParser.parse(moduleInfoContent.toString()), expander);
        } catch (ParseProblemException ex) {
            // ignore
            return Optional.empty();
        }
    }

    private static Optional<ModuleInfoEntries> parse(CompilationUnit compilationUnit, PackageExpander expander) {
        List<String> imports = parseImports(compilationUnit, expander);
        return compilationUnit.getModule()
                .map(moduleDeclaration -> parseDirectives(imports, moduleDeclaration.getDirectives()));
    }

    private static List<String> parseImports(CompilationUnit compilationUnit, PackageExpander expander) {
        return compilationUnit
                .getImports()
                .stream()
                .flatMap(importDeclaration -> importDeclaration.isAsterisk()
                        ? expander.getEnclosedTypeNames(importDeclaration.getNameAsString())
                        : Stream.of(importDeclaration.getNameAsString()))
                .collect(toList());
    }

    private static ModuleInfoEntries parseDirectives(List<String> imports, NodeList<ModuleDirective> directives) {
        ModuleInfoEntries.Builder result = ModuleInfoEntries.builder();
        directives.forEach(directive -> {
            directive.ifModuleProvidesDirective(provides -> provides.getWith().forEach(z -> result.provision(resolveTypeName(imports, provides.getNameAsString()), resolveTypeName(imports, z.asString()))));
            directive.ifModuleUsesDirective(uses -> result.usage(resolveTypeName(imports, uses.getNameAsString())));
        });
        return result.build();
    }

    private static String resolveTypeName(List<String> imports, String input) {
        String expected = "." + input.split("\\.", -1)[0];
        return imports
                .stream()
                .filter(importDecl -> importDecl.endsWith(expected))
                .map(o -> o.substring(0, o.lastIndexOf(expected)) + "." + input)
                .findFirst()
                .orElse(input);
    }
}

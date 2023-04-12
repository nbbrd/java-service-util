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
import com.github.javaparser.ast.modules.ModuleDeclaration;
import com.github.javaparser.ast.modules.ModuleDirective;
import com.github.javaparser.ast.nodeTypes.NodeWithName;

import javax.annotation.processing.Filer;
import javax.annotation.processing.FilerException;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.*;

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

    public static Optional<ModuleInfoEntries> parse(Filer filer) throws IOException {
        try {
            FileObject src = filer.getResource(StandardLocation.SOURCE_PATH, "", "module-info.java");
            return Optional.of(parse(src.getCharContent(false)));
        } catch (FileNotFoundException | NoSuchFileException | FilerException | RuntimeException ex) {
            // ignore
            return Optional.empty();
        }
    }

    static ModuleInfoEntries parse(CharSequence content) {
        try {
            CompilationUnit compilationUnit = StaticJavaParser.parse(content.toString());
            List<String> imports = compilationUnit.getImports().stream().map(NodeWithName::getNameAsString).collect(toList());

            Optional<ModuleDeclaration> module = compilationUnit.getModule();
            if (module.isPresent()) {
                return parse(imports, module.get().getDirectives());
            }
        } catch (ParseProblemException ex) {
            // ignore
        }
        return ModuleInfoEntries.builder().build();
    }

    private static ModuleInfoEntries parse(List<String> imports, NodeList<ModuleDirective> directives) {
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

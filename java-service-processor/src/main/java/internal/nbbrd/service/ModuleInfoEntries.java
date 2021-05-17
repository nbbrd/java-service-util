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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.processing.Filer;
import javax.annotation.processing.FilerException;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

/**
 *
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
        return parse(CharStreams.fromString(content.toString()));
    }

    static ModuleInfoEntries parse(CharStream content) {
        Java9Lexer lexer = new Java9Lexer(content);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        Java9Parser parser = new Java9Parser(tokens);
        Java9Parser.CompilationUnitContext tree = parser.compilationUnit();

        ModuleListener listener = new ModuleListener();
        new ParseTreeWalker().walk(listener, tree);

        ModuleInfoEntries.Builder result = ModuleInfoEntries.builder();
        result.provisions(listener.getProvisions());
        result.usages(listener.getUsages());
        return result.build();
    }

    private static final class ModuleListener extends Java9BaseListener {

        private final List<String> importDecls = new ArrayList<>();
        private final List<String> usesDecls = new ArrayList<>();
        private final Map<String, List<String>> providesDecls = new HashMap<>();

        private static final int SERVICE_IDX = 1;
        private static final int PROVIDER_IDX = 3;

        @Override
        public void enterImportDeclaration(Java9Parser.ImportDeclarationContext ctx) {
            IntStream
                    .range(0, ctx.getChildCount())
                    .mapToObj(index -> ctx.getChild(index).getChild(1).getText())
                    .forEach(importDecls::add);
        }

        @Override
        public void enterModuleDirective(Java9Parser.ModuleDirectiveContext ctx) {
            switch (ModuleDirectiveType.parse(ctx)) {
                case PROVIDES:
                    String service = ctx.getChild(SERVICE_IDX).getText();
                    List<String> providers = IntStream
                            .range(PROVIDER_IDX, ctx.getChildCount())
                            .mapToObj(ctx::getChild)
                            .filter(Java9Parser.TypeNameContext.class::isInstance)
                            .map(ParseTree::getText)
                            .collect(Collectors.toList());
                    providesDecls.put(service, providers);
                    break;
                case USES:
                    usesDecls.add(ctx.getChild(SERVICE_IDX).getText());
                    break;
            }
        }

        private String resolveTypeName(String input) {
            String expected = "." + input.split("\\.", -1)[0];
            return importDecls
                    .stream()
                    .filter(importDecl -> importDecl.endsWith(expected))
                    .map(o -> o.substring(0, o.lastIndexOf(expected)) + "." + input)
                    .findFirst()
                    .orElse(input);
        }

        List<String> getUsages() {
            return usesDecls.stream().map(this::resolveTypeName).collect(Collectors.toList());
        }

        Map<String, List<String>> getProvisions() {
            Map<String, List<String>> result = new HashMap<>();
            providesDecls.forEach((k, v) -> result.put(resolveTypeName(k), v.stream().map(this::resolveTypeName).collect(Collectors.toList())));
            return result;
        }
    }

    @lombok.AllArgsConstructor
    private enum ModuleDirectiveType {
        UNKNOWN(""), PROVIDES("provides"), USES("uses");

        private final String keyword;

        private static final int MODULE_DIRECTIVE_KEYWORD_IDX = 0;

        static ModuleDirectiveType parse(Java9Parser.ModuleDirectiveContext ctx) {
            if (ctx.getChildCount() > 0) {
                String keyword = ctx.getChild(MODULE_DIRECTIVE_KEYWORD_IDX).getText();
                for (ModuleDirectiveType type : values()) {
                    if (type.keyword.equals(keyword)) {
                        return type;
                    }
                }
            }
            return UNKNOWN;
        }
    }
}

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.processing.FilerException;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Name;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

/**
 *
 * @author Philippe Charles
 */
@lombok.RequiredArgsConstructor
final class ModulePathRegistry implements ProviderRegistry {

    @lombok.NonNull
    private final ProcessingEnvironment env;

    public Optional<List<ProviderRef>> readAll() throws IOException {
        try {
            FileObject src = env.getFiler().getResource(StandardLocation.SOURCE_PATH, "", "module-info.java");
            return Optional.of(parseAll(env.getElementUtils()::getName, src.getCharContent(false)));
        } catch (FileNotFoundException | NoSuchFileException | FilerException | RuntimeException ex) {
            // ignore
            return Optional.empty();
        }
    }

    static List<ProviderRef> parseAll(Function<String, Name> nameFactory, CharSequence content) {
        Java9Lexer lexer = new Java9Lexer(CharStreams.fromString(content.toString()));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        Java9Parser parser = new Java9Parser(tokens);
        Java9Parser.CompilationUnitContext tree = parser.compilationUnit();

        ModuleListener listener = new ModuleListener();
        new ParseTreeWalker().walk(listener, tree);

        return listener.build(nameFactory);
    }

    static final class ModuleListener extends Java9BaseListener {

        public final Map<String, List<String>> refs = new HashMap<>();

        static final int PROVIDES_IDX = 0;
        static final int SERVICE_IDX = 1;
        static final int PROVIDER_IDX = 3;

        @Override
        public void enterModuleDirective(Java9Parser.ModuleDirectiveContext ctx) {
            if (isProvidesDirective(ctx)) {
                String service = ctx.getChild(SERVICE_IDX).getText();
                List<String> providers = IntStream
                        .range(PROVIDER_IDX, ctx.getChildCount())
                        .mapToObj(ctx::getChild)
                        .filter(Java9Parser.TypeNameContext.class::isInstance)
                        .map(ParseTree::getText)
                        .collect(Collectors.toList());
                refs.put(service, providers);
            }
        }

        boolean isProvidesDirective(Java9Parser.ModuleDirectiveContext ctx) {
            return ctx.getChildCount() > 0 && "provides".equals(ctx.getChild(PROVIDES_IDX).getText());
        }

        List<ProviderRef> build(Function<String, Name> nameFactory) {
            List<ProviderRef> result = new ArrayList<>();
            for (Map.Entry<String, List<String>> entry : refs.entrySet()) {
                for (String provider : entry.getValue()) {
                    result.add(new ProviderRef(nameFactory.apply(entry.getKey()), nameFactory.apply(provider)));
                }
            }
            return result;
        }
    }
}

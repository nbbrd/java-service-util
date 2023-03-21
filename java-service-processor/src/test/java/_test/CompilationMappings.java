package _test;

import com.google.testing.compile.Compilation;

import javax.tools.JavaFileObject;

public final class CompilationMappings {

    private CompilationMappings() {
        // static class
    }

    public static JavaFileObject getGeneratedSourceFile(Compilation compilation, String qualifiedName) {
        return compilation.generatedSourceFile(qualifiedName).get();
    }

    public static int getWarningsCount(Compilation compilation) {
        return compilation.warnings().size();
    }
}

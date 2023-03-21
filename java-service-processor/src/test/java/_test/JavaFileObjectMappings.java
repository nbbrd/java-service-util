package _test;

import javax.tools.JavaFileObject;
import java.io.IOException;

public final class JavaFileObjectMappings {

    private JavaFileObjectMappings() {
        // static class
    }

    public static String getNormalizedJavaSource(JavaFileObject file) {
        try {
            return normalizeSource(file.getCharContent(false));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String normalizeSource(CharSequence source) {
        return source.toString().replace("\r\n", "\n").replace("\n", "");
    }
}

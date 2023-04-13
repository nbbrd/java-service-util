package _test;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.CompilationSubject;
import org.assertj.core.api.Condition;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.assertj.core.api.ListAssert;
import org.assertj.core.condition.AllOf;

import javax.tools.Diagnostic;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static javax.tools.StandardLocation.SOURCE_OUTPUT;
import static org.assertj.core.api.HamcrestCondition.matching;
import static org.assertj.core.condition.MappedCondition.mappedCondition;
import static org.hamcrest.core.IsEqual.equalTo;

public final class Compilations {

    private Compilations() {
        // static class
    }

    @SuppressWarnings("rawtypes")
    public static InstanceOfAssertFactory<List, ListAssert<JavaFileObject>> JAVA_FILE_OBJECTS = InstanceOfAssertFactories.list(JavaFileObject.class);

    @SuppressWarnings("rawtypes")
    public static InstanceOfAssertFactory<List, ListAssert<Diagnostic>> DIAGNOSTICS = InstanceOfAssertFactories.list(Diagnostic.class);

    /**
     * {@link CompilationSubject#succeededWithoutWarnings()}
     *
     * @return
     */
    public static Condition<Compilation> succeededWithoutWarnings() {
        return AllOf.allOf(status(Compilation.Status.SUCCESS), warningCount(0));
    }

    public static Condition<Compilation> succeeded() {
        return status(Compilation.Status.SUCCESS);
    }

    public static Condition<Compilation> failed() {
        return status(Compilation.Status.FAILURE);
    }

    private static Condition<Compilation> status(Compilation.Status expected) {
        return mappedCondition(Compilation::status, matching(equalTo(expected)));
    }

    private static Condition<Compilation> warningCount(int expected) {
        return mappedCondition(Compilations::getWarningsCount, matching(equalTo(expected)));
    }

    public static int getWarningsCount(Compilation compilation) {
        return compilation.warnings().size();
    }

    public static String getDefaultMessage(Diagnostic<?> diagnostic) {
        return diagnostic.getMessage(null);
    }

    public static Condition<JavaFileObject> fileNamed(String expected) {
        return mappedCondition(JavaFileObject::getName, matching(equalTo(expected)));
    }

    public static Condition<JavaFileObject> sourceFileNamed(String packageName, String relativeName) {
        String expected = getFileName(SOURCE_OUTPUT, packageName, relativeName);
        return mappedCondition(JavaFileObject::getName, matching(equalTo(expected)));
    }

    private static String getFileName(JavaFileManager.Location location, String packageName, String relativeName) {
        String path = packageName.isEmpty() ? relativeName : packageName.replace('.', '/') + '/' + relativeName;
        return String.format(Locale.ROOT, "/%s/%s", location.getName(), path);
    }

    public static String contentsAsUtf8String(JavaFileObject file) {
        try {
            return file.getCharContent(false).toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

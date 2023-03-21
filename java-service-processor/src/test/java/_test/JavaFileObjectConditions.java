package _test;

import com.google.testing.compile.JavaFileObjectSubject;
import org.assertj.core.api.Condition;
import org.assertj.core.api.HamcrestCondition;

import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

import static javax.tools.StandardLocation.SOURCE_OUTPUT;
import static org.assertj.core.condition.MappedCondition.mappedCondition;
import static org.hamcrest.core.IsEqual.equalTo;

public final class JavaFileObjectConditions {

    private JavaFileObjectConditions() {
        // static class
    }

    /**
     * {@link JavaFileObjectSubject#hasSourceEquivalentTo(JavaFileObject)}
     *
     * @param expected
     * @return
     */
    public static Condition<JavaFileObject> sourceEquivalentTo(JavaFileObject expected) {
        return mappedCondition(JavaFileObjectMappings::getNormalizedJavaSource, new HamcrestCondition<>(equalTo(JavaFileObjectMappings.getNormalizedJavaSource(expected))));
    }

    public static Condition<JavaFileObject> fileNamed(String packageName, String relativeName) {
        String expected = getFileName(SOURCE_OUTPUT, packageName, relativeName);
        return mappedCondition(JavaFileObject::getName, new HamcrestCondition<>(equalTo(expected)));
    }

    private static String getFileName(JavaFileManager.Location location, String packageName, String relativeName) {
        String path = packageName.isEmpty() ? relativeName : packageName.replace('.', '/') + '/' + relativeName;
        return String.format("/%s/%s", location.getName(), path);
    }
}

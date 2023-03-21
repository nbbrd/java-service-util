package _test;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.CompilationSubject;
import org.assertj.core.api.Condition;
import org.assertj.core.api.HamcrestCondition;
import org.assertj.core.condition.AllOf;

import javax.tools.JavaFileObject;

import static _test.CompilationMappings.getGeneratedSourceFile;
import static org.assertj.core.condition.MappedCondition.mappedCondition;
import static org.hamcrest.core.IsEqual.equalTo;

public final class CompilationConditions {

    private CompilationConditions() {
        // static class
    }

    /**
     * {@link CompilationSubject#succeededWithoutWarnings()}
     *
     * @return
     */
    public static Condition<Compilation> succeededWithoutWarnings() {
        return AllOf.allOf(status(Compilation.Status.SUCCESS), warningCount(0));
    }

    /**
     * {@link CompilationSubject#generatedSourceFile(String)}
     *
     * @param qualifiedName
     * @param condition
     * @return
     */
    public static Condition<Compilation> generatedSourceFile(String qualifiedName, Condition<JavaFileObject> condition) {
        return mappedCondition(compilation -> getGeneratedSourceFile(compilation, qualifiedName), condition);
    }

    private static Condition<Compilation> status(Compilation.Status expected) {
        return mappedCondition(Compilation::status, new HamcrestCondition<>(equalTo(expected)));
    }

    private static Condition<Compilation> warningCount(int expected) {
        return mappedCondition(CompilationMappings::getWarningsCount, new HamcrestCondition<>(equalTo(expected)));
    }
}

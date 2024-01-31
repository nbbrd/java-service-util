package nbbrd.service;

import org.junit.jupiter.api.Test;

import static nbbrd.service.ServiceId.*;
import static org.assertj.core.api.Assertions.assertThat;

// https://gist.github.com/SuppieRK/a6fb471cf600271230c8c7e532bdae4b
public class ServiceIdTest {

    public static final String FLAT_CASE_EXAMPLE = "flatcase";
    public static final String UPPER_FLAT_CASE_EXAMPLE = "UPPERFLATCASE";
    public static final String CAMEL_CASE_EXAMPLE = "camelCase";
    public static final String PASCAL_CASE_EXAMPLE = "PascalCase";
    public static final String SNAKE_CASE_EXAMPLE = "snake_case";
    public static final String SCREAMING_SNAKE_CASE_EXAMPLE = "SCREAMING_SNAKE_CASE";
    public static final String CAMEL_SNAKE_CASE_EXAMPLE = "Camel_Snake_Case";
    public static final String KEBAB_CASE_EXAMPLE = "kebab-case";
    public static final String SCREAMING_KEBAB_CASE_EXAMPLE = "SCREAMING-KEBAB-CASE";
    public static final String TRAIN_CASE_EXAMPLE = "Train-Case";

    @Test
    public void testFLAT_CASE() {
        assertThat(FLAT_CASE_EXAMPLE).matches(FLAT_CASE);
        assertThat(UPPER_FLAT_CASE_EXAMPLE).doesNotMatch(FLAT_CASE);
        assertThat(CAMEL_CASE_EXAMPLE).doesNotMatch(FLAT_CASE);
        assertThat(PASCAL_CASE_EXAMPLE).doesNotMatch(FLAT_CASE);
        assertThat(SNAKE_CASE_EXAMPLE).doesNotMatch(FLAT_CASE);
        assertThat(SCREAMING_SNAKE_CASE_EXAMPLE).doesNotMatch(FLAT_CASE);
        assertThat(CAMEL_SNAKE_CASE_EXAMPLE).doesNotMatch(FLAT_CASE);
        assertThat(KEBAB_CASE_EXAMPLE).doesNotMatch(FLAT_CASE);
        assertThat(SCREAMING_KEBAB_CASE_EXAMPLE).doesNotMatch(FLAT_CASE);
        assertThat(TRAIN_CASE_EXAMPLE).doesNotMatch(FLAT_CASE);
    }

    @Test
    public void testUPPER_FLAT_CASE() {
        assertThat(FLAT_CASE_EXAMPLE).doesNotMatch(UPPER_FLAT_CASE);
        assertThat(UPPER_FLAT_CASE_EXAMPLE).matches(UPPER_FLAT_CASE);
        assertThat(CAMEL_CASE_EXAMPLE).doesNotMatch(UPPER_FLAT_CASE);
        assertThat(PASCAL_CASE_EXAMPLE).doesNotMatch(UPPER_FLAT_CASE);
        assertThat(SNAKE_CASE_EXAMPLE).doesNotMatch(UPPER_FLAT_CASE);
        assertThat(SCREAMING_SNAKE_CASE_EXAMPLE).doesNotMatch(UPPER_FLAT_CASE);
        assertThat(CAMEL_SNAKE_CASE_EXAMPLE).doesNotMatch(UPPER_FLAT_CASE);
        assertThat(KEBAB_CASE_EXAMPLE).doesNotMatch(UPPER_FLAT_CASE);
        assertThat(SCREAMING_KEBAB_CASE_EXAMPLE).doesNotMatch(UPPER_FLAT_CASE);
        assertThat(TRAIN_CASE_EXAMPLE).doesNotMatch(UPPER_FLAT_CASE);
    }

    @Test
    public void testCAMEL_CASE() {
        assertThat(FLAT_CASE_EXAMPLE).matches(CAMEL_CASE);
        assertThat(UPPER_FLAT_CASE_EXAMPLE).doesNotMatch(CAMEL_CASE);
        assertThat(CAMEL_CASE_EXAMPLE).matches(CAMEL_CASE);
        assertThat(PASCAL_CASE_EXAMPLE).doesNotMatch(CAMEL_CASE);
        assertThat(SNAKE_CASE_EXAMPLE).doesNotMatch(CAMEL_CASE);
        assertThat(SCREAMING_SNAKE_CASE_EXAMPLE).doesNotMatch(CAMEL_CASE);
        assertThat(CAMEL_SNAKE_CASE_EXAMPLE).doesNotMatch(CAMEL_CASE);
        assertThat(KEBAB_CASE_EXAMPLE).doesNotMatch(CAMEL_CASE);
        assertThat(SCREAMING_KEBAB_CASE_EXAMPLE).doesNotMatch(CAMEL_CASE);
        assertThat(TRAIN_CASE_EXAMPLE).doesNotMatch(CAMEL_CASE);
    }

    @Test
    public void testPASCAL_CASE() {
        assertThat(FLAT_CASE_EXAMPLE).doesNotMatch(PASCAL_CASE);
        assertThat(UPPER_FLAT_CASE_EXAMPLE).doesNotMatch(PASCAL_CASE);
        assertThat(CAMEL_CASE_EXAMPLE).doesNotMatch(PASCAL_CASE);
        assertThat(PASCAL_CASE_EXAMPLE).matches(PASCAL_CASE);
        assertThat(SNAKE_CASE_EXAMPLE).doesNotMatch(PASCAL_CASE);
        assertThat(SCREAMING_SNAKE_CASE_EXAMPLE).doesNotMatch(PASCAL_CASE);
        assertThat(CAMEL_SNAKE_CASE_EXAMPLE).doesNotMatch(PASCAL_CASE);
        assertThat(KEBAB_CASE_EXAMPLE).doesNotMatch(PASCAL_CASE);
        assertThat(SCREAMING_KEBAB_CASE_EXAMPLE).doesNotMatch(PASCAL_CASE);
        assertThat(TRAIN_CASE_EXAMPLE).doesNotMatch(PASCAL_CASE);
    }

    @Test
    public void testSNAKE_CASE() {
        assertThat(FLAT_CASE_EXAMPLE).matches(SNAKE_CASE);
        assertThat(UPPER_FLAT_CASE_EXAMPLE).doesNotMatch(SNAKE_CASE);
        assertThat(CAMEL_CASE_EXAMPLE).doesNotMatch(SNAKE_CASE);
        assertThat(PASCAL_CASE_EXAMPLE).doesNotMatch(SNAKE_CASE);
        assertThat(SNAKE_CASE_EXAMPLE).matches(SNAKE_CASE);
        assertThat(SCREAMING_SNAKE_CASE_EXAMPLE).doesNotMatch(SNAKE_CASE);
        assertThat(CAMEL_SNAKE_CASE_EXAMPLE).doesNotMatch(SNAKE_CASE);
        assertThat(KEBAB_CASE_EXAMPLE).doesNotMatch(SNAKE_CASE);
        assertThat(SCREAMING_KEBAB_CASE_EXAMPLE).doesNotMatch(SNAKE_CASE);
        assertThat(TRAIN_CASE_EXAMPLE).doesNotMatch(SNAKE_CASE);
    }

    @Test
    public void testSCREAMING_SNAKE_CASE() {
        assertThat(FLAT_CASE_EXAMPLE).doesNotMatch(SCREAMING_SNAKE_CASE);
        assertThat(UPPER_FLAT_CASE_EXAMPLE).matches(SCREAMING_SNAKE_CASE);
        assertThat(CAMEL_CASE_EXAMPLE).doesNotMatch(SCREAMING_SNAKE_CASE);
        assertThat(PASCAL_CASE_EXAMPLE).doesNotMatch(SCREAMING_SNAKE_CASE);
        assertThat(SNAKE_CASE_EXAMPLE).doesNotMatch(SCREAMING_SNAKE_CASE);
        assertThat(SCREAMING_SNAKE_CASE_EXAMPLE).matches(SCREAMING_SNAKE_CASE);
        assertThat(CAMEL_SNAKE_CASE_EXAMPLE).doesNotMatch(SCREAMING_SNAKE_CASE);
        assertThat(KEBAB_CASE_EXAMPLE).doesNotMatch(SCREAMING_SNAKE_CASE);
        assertThat(SCREAMING_KEBAB_CASE_EXAMPLE).doesNotMatch(SCREAMING_SNAKE_CASE);
        assertThat(TRAIN_CASE_EXAMPLE).doesNotMatch(SCREAMING_SNAKE_CASE);
    }

    @Test
    public void testCAMEL_SNAKE_CASE() {
        assertThat(FLAT_CASE_EXAMPLE).doesNotMatch(CAMEL_SNAKE_CASE);
        assertThat(UPPER_FLAT_CASE_EXAMPLE).doesNotMatch(CAMEL_SNAKE_CASE);
        assertThat(CAMEL_CASE_EXAMPLE).doesNotMatch(CAMEL_SNAKE_CASE);
        assertThat(PASCAL_CASE_EXAMPLE).doesNotMatch(CAMEL_SNAKE_CASE);
        assertThat(SNAKE_CASE_EXAMPLE).doesNotMatch(CAMEL_SNAKE_CASE);
        assertThat(SCREAMING_SNAKE_CASE_EXAMPLE).doesNotMatch(CAMEL_SNAKE_CASE);
        assertThat(CAMEL_SNAKE_CASE_EXAMPLE).matches(CAMEL_SNAKE_CASE);
        assertThat(KEBAB_CASE_EXAMPLE).doesNotMatch(CAMEL_SNAKE_CASE);
        assertThat(SCREAMING_KEBAB_CASE_EXAMPLE).doesNotMatch(CAMEL_SNAKE_CASE);
        assertThat(TRAIN_CASE_EXAMPLE).doesNotMatch(CAMEL_SNAKE_CASE);
    }

    @Test
    public void testKEBAB_CASE() {
        assertThat(FLAT_CASE_EXAMPLE).matches(KEBAB_CASE);
        assertThat(UPPER_FLAT_CASE_EXAMPLE).doesNotMatch(KEBAB_CASE);
        assertThat(CAMEL_CASE_EXAMPLE).doesNotMatch(KEBAB_CASE);
        assertThat(PASCAL_CASE_EXAMPLE).doesNotMatch(KEBAB_CASE);
        assertThat(SNAKE_CASE_EXAMPLE).doesNotMatch(KEBAB_CASE);
        assertThat(SCREAMING_SNAKE_CASE_EXAMPLE).doesNotMatch(KEBAB_CASE);
        assertThat(CAMEL_SNAKE_CASE_EXAMPLE).doesNotMatch(KEBAB_CASE);
        assertThat(KEBAB_CASE_EXAMPLE).matches(KEBAB_CASE);
        assertThat(SCREAMING_KEBAB_CASE_EXAMPLE).doesNotMatch(KEBAB_CASE);
        assertThat(TRAIN_CASE_EXAMPLE).doesNotMatch(KEBAB_CASE);
    }

    @Test
    public void testSCREAMING_KEBAB_CASE() {
        assertThat(FLAT_CASE_EXAMPLE).doesNotMatch(SCREAMING_KEBAB_CASE);
        assertThat(UPPER_FLAT_CASE_EXAMPLE).matches(SCREAMING_KEBAB_CASE);
        assertThat(CAMEL_CASE_EXAMPLE).doesNotMatch(SCREAMING_KEBAB_CASE);
        assertThat(PASCAL_CASE_EXAMPLE).doesNotMatch(SCREAMING_KEBAB_CASE);
        assertThat(SNAKE_CASE_EXAMPLE).doesNotMatch(SCREAMING_KEBAB_CASE);
        assertThat(SCREAMING_SNAKE_CASE_EXAMPLE).doesNotMatch(SCREAMING_KEBAB_CASE);
        assertThat(CAMEL_SNAKE_CASE_EXAMPLE).doesNotMatch(SCREAMING_KEBAB_CASE);
        assertThat(KEBAB_CASE_EXAMPLE).doesNotMatch(SCREAMING_KEBAB_CASE);
        assertThat(SCREAMING_KEBAB_CASE_EXAMPLE).matches(SCREAMING_KEBAB_CASE);
        assertThat(TRAIN_CASE_EXAMPLE).doesNotMatch(SCREAMING_KEBAB_CASE);
    }

    @Test
    public void testTRAIN_CASE() {
        assertThat(FLAT_CASE_EXAMPLE).doesNotMatch(TRAIN_CASE);
        assertThat(UPPER_FLAT_CASE_EXAMPLE).doesNotMatch(TRAIN_CASE);
        assertThat(CAMEL_CASE_EXAMPLE).doesNotMatch(TRAIN_CASE);
        assertThat(PASCAL_CASE_EXAMPLE).doesNotMatch(TRAIN_CASE);
        assertThat(SNAKE_CASE_EXAMPLE).doesNotMatch(TRAIN_CASE);
        assertThat(SCREAMING_SNAKE_CASE_EXAMPLE).doesNotMatch(TRAIN_CASE);
        assertThat(CAMEL_SNAKE_CASE_EXAMPLE).doesNotMatch(TRAIN_CASE);
        assertThat(KEBAB_CASE_EXAMPLE).doesNotMatch(TRAIN_CASE);
        assertThat(SCREAMING_KEBAB_CASE_EXAMPLE).doesNotMatch(TRAIN_CASE);
        assertThat(TRAIN_CASE_EXAMPLE).matches(TRAIN_CASE);
    }
}

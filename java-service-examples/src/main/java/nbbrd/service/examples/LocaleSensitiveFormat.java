package nbbrd.service.examples;

import nbbrd.service.Quantifier;
import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceId;
import nbbrd.service.ServiceProvider;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Illustrates {@code @ServiceId} with a non-String return type.
 * <p>
 * The service is identified by a {@link Locale}.
 * Because {@link Locale} is a built-in representable type, no explicit
 * {@code formatMethodName} is needed: the processor automatically uses
 * {@link Locale#toLanguageTag()} to obtain an unambiguous String ID.
 * <p>
 * When the return type is not a built-in, the explicit form must be used:
 * {@code @ServiceId(formatMethodName = "myMethod")}.
 */
@ServiceDefinition(quantifier = Quantifier.MULTIPLE)
public interface LocaleSensitiveFormat {

    // 💡 Locale is a built-in type: toLanguageTag() is used automatically
    @ServiceId
    Locale getLocale();

    String formatNumber(double value);

    static void main(String[] args) {
        // 💡 Retrieve service by locale tag — works because Locale#toLanguageTag() is used as ID
        LocaleSensitiveFormatLoader.loadById("fr-FR")
                .map(f -> f.formatNumber(1_234_567.89))
                .ifPresent(System.out::println); // e.g. "1 234 567,89"
    }

    @ServiceProvider
    final class French implements LocaleSensitiveFormat {

        @Override
        public Locale getLocale() {
            return Locale.FRANCE;
        }

        @Override
        public String formatNumber(double value) {
            return NumberFormat.getNumberInstance(Locale.FRANCE).format(value);
        }
    }

    @ServiceProvider
    final class German implements LocaleSensitiveFormat {

        @Override
        public Locale getLocale() {
            return Locale.GERMANY;
        }

        @Override
        public String formatNumber(double value) {
            return NumberFormat.getNumberInstance(Locale.GERMANY).format(value);
        }
    }
}


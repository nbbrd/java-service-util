package nbbrd.service.examples;

import nbbrd.service.Quantifier;
import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceId;
import nbbrd.service.ServiceProvider;

import java.util.Arrays;
import java.util.List;

/**
 * Illustrates {@code @ServiceId} with a custom type annotated with
 * {@code @RepresentableAsString}.
 * <p>
 * The service is identified by a {@link MediaType}.
 * Because {@link MediaType} is annotated with
 * {@code @RepresentableAsString},
 * the processor discovers that method automatically — no explicit
 * {@code formatMethodName} is needed on {@code @ServiceId}.
 */
@ServiceDefinition(quantifier = Quantifier.MULTIPLE)
public interface MediaTypeInfo {

    // 💡 MediaType carries @RepresentableAsString: toString() is resolved automatically
    @ServiceId
    MediaType getMediaType();

    String getDescription();

    List<String> getFileExtensions();

    static void main(String[] args) {
        // 💡 Retrieve service by media type string — works because MediaType#toString() is used as ID
        MediaTypeInfoLoader.loadById("image/png")
                .map(info -> info.getDescription() + " " + info.getFileExtensions())
                .ifPresent(System.out::println); // "Portable Network Graphics [.png]"
    }

    @ServiceProvider
    final class Png implements MediaTypeInfo {

        @Override
        public MediaType getMediaType() {
            return MediaType.parse("image/png");
        }

        @Override
        public String getDescription() {
            return "Portable Network Graphics";
        }

        @Override
        public List<String> getFileExtensions() {
            return Arrays.asList(".png");
        }
    }

    @ServiceProvider
    final class Json implements MediaTypeInfo {

        @Override
        public MediaType getMediaType() {
            return MediaType.parse("application/json");
        }

        @Override
        public String getDescription() {
            return "JavaScript Object Notation";
        }

        @Override
        public List<String> getFileExtensions() {
            return Arrays.asList(".json");
        }
    }

    @ServiceProvider
    final class PlainText implements MediaTypeInfo {

        @Override
        public MediaType getMediaType() {
            return MediaType.parse("text/plain");
        }

        @Override
        public String getDescription() {
            return "Plain Text";
        }

        @Override
        public List<String> getFileExtensions() {
            return Arrays.asList(".txt", ".text");
        }
    }
}


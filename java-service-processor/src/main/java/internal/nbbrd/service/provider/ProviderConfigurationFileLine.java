package internal.nbbrd.service.provider;

import lombok.AccessLevel;
import lombok.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * <a href="https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html">...</a>
 */
@lombok.Value
@lombok.RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ProviderConfigurationFileLine {

    @Nullable String providerBinaryName;

    @Nullable String comment;

    public boolean hasProviderBinaryName() {
        return providerBinaryName != null;
    }

    public boolean hasComment() {
        return comment != null;
    }

    @Override
    public String toString() {
        if (hasProviderBinaryName()) {
            return hasComment() ? (providerBinaryName + " #" + comment) : providerBinaryName;
        } else {
            return hasComment() ? ("#" + comment) : "";
        }
    }

    public static final ProviderConfigurationFileLine EMPTY = new ProviderConfigurationFileLine(null, null);

    public static @NonNull ProviderConfigurationFileLine ofProviderBinaryName(@NonNull CharSequence providerBinaryName) {
        if (providerBinaryName.length() == 0) {
            throw new IllegalArgumentException("Provider binary name cannot be empty");
        }
        return new ProviderConfigurationFileLine(providerBinaryName.toString(), null);
    }

    public static @NonNull ProviderConfigurationFileLine ofComment(@NonNull CharSequence comment) {
        return new ProviderConfigurationFileLine(null, comment.toString());
    }

    public static @NonNull ProviderConfigurationFileLine ofProviderBinaryNameAndComment(@NonNull CharSequence providerName, @NonNull CharSequence comment) {
        return new ProviderConfigurationFileLine(providerName.toString(), comment.toString());
    }

    public static @NonNull ProviderConfigurationFileLine parse(@NonNull CharSequence text) throws IllegalArgumentException {
        String line = text.toString();
        int commentIndex = line.indexOf('#');
        if (commentIndex >= 0) {
            String providerName = line.substring(0, commentIndex).trim();
            String comment = line.substring(commentIndex + 1);
            return providerName.isEmpty()
                    ? ofComment(comment)
                    : ofProviderBinaryNameAndComment(checkProviderName(providerName), comment);
        } else {
            String providerName = line.trim();
            return providerName.isEmpty()
                    ? EMPTY
                    : ofProviderBinaryName(checkProviderName(providerName));
        }
    }

    private static String checkProviderName(String providerName) throws IllegalArgumentException {
        if ((providerName.indexOf(' ') >= 0) || (providerName.indexOf('\t') >= 0)) {
            throw new IllegalArgumentException("Illegal configuration-file syntax");
        }
        int cp = providerName.codePointAt(0);
        if (!Character.isJavaIdentifierStart(cp)) {
            throw new IllegalArgumentException("Illegal provider-class name: " + providerName);
        }
        int start = Character.charCount(cp);
        for (int i = start; i < providerName.length(); i += Character.charCount(cp)) {
            cp = providerName.codePointAt(i);
            if (!Character.isJavaIdentifierPart(cp) && (cp != '.')) {
                throw new IllegalArgumentException("Illegal provider-class name: " + providerName);
            }
        }
        return providerName;
    }

    public static String getFileRelativeName(CharSequence serviceBinaryName) {
        return "META-INF/services/" + serviceBinaryName;
    }
}

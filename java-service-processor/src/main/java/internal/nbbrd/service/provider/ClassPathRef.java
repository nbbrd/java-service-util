package internal.nbbrd.service.provider;

@lombok.Value
public class ClassPathRef {

    String providerName;

    String comment;

    @Override
    public String toString() {
        return providerName + (comment != null ? (" #" + comment) : "");
    }

    public static ClassPathRef parse(CharSequence text) throws IllegalArgumentException {
        String line = text.toString();
        int commentIndex = line.indexOf('#');
        if (commentIndex >= 0) {
            String providerName = line.substring(0, commentIndex).trim();
            String comment = line.substring(commentIndex + 1);
            return new ClassPathRef(checkProviderName(providerName), comment);
        } else {
            String providerName = line.trim();
            return new ClassPathRef(checkProviderName(providerName), null);
        }
    }

    private static String checkProviderName(String providerName) throws IllegalArgumentException {
        if (providerName.isEmpty()) {
            return providerName;
        }
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
}

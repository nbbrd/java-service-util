package nbbrd.service.examples;

import nbbrd.design.RepresentableAsString;
import nbbrd.design.StaticFactoryMethod;

/**
 * Immutable MIME media type (e.g. {@code "text/plain"}, {@code "image/png"}).
 */
// 💡 Declares toString() as the canonical String format — picked up by @ServiceId automatically
@RepresentableAsString
public final class MediaType {

    private final String type;
    private final String subtype;

    private MediaType(String type, String subtype) {
        this.type = type;
        this.subtype = subtype;
    }

    @StaticFactoryMethod
    public static MediaType parse(CharSequence s) {
        String text = s.toString();
        int slash = text.indexOf('/');
        if (slash < 0) throw new IllegalArgumentException("Missing '/' in media type: " + text);
        return new MediaType(text.substring(0, slash), text.substring(slash + 1));
    }

    @Override
    public String toString() {
        return type + "/" + subtype;
    }

    public String getType() {
        return this.type;
    }

    public String getSubtype() {
        return this.subtype;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof MediaType)) return false;
        final MediaType other = (MediaType) o;
        final Object this$type = this.getType();
        final Object other$type = other.getType();
        if (this$type == null ? other$type != null : !this$type.equals(other$type)) return false;
        final Object this$subtype = this.getSubtype();
        final Object other$subtype = other.getSubtype();
        if (this$subtype == null ? other$subtype != null : !this$subtype.equals(other$subtype)) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $type = this.getType();
        result = result * PRIME + ($type == null ? 43 : $type.hashCode());
        final Object $subtype = this.getSubtype();
        result = result * PRIME + ($subtype == null ? 43 : $subtype.hashCode());
        return result;
    }
}


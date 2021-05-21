package internal.nbbrd.service;

import org.checkerframework.checker.nullness.qual.NonNull;

import javax.lang.model.type.TypeMirror;

public interface HasTypeMirror {

    @NonNull TypeMirror getType();

    default @NonNull String getTypeName() {
        return getType().toString();
    }
}

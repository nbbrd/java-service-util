package internal.nbbrd.service;

import lombok.NonNull;

import javax.lang.model.type.TypeMirror;

public interface HasTypeMirror {

    @NonNull TypeMirror getType();

    default @NonNull String getTypeName() {
        return getType().toString();
    }
}

package internal.nbbrd.service;

import lombok.NonNull;

import javax.lang.model.element.ExecutableElement;

public interface HasMethod {

    @NonNull ExecutableElement getMethod();

    default @NonNull String getMethodName() {
        return getMethod().getSimpleName().toString();
    }
}

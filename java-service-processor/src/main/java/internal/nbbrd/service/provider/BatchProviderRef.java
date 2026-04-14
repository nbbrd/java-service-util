package internal.nbbrd.service.provider;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

/**
 * Reference to a batch provider that needs to be generated for an enum.
 */
@lombok.Value
class BatchProviderRef {

    @lombok.NonNull
    TypeElement batchType;

    @lombok.NonNull
    TypeElement enumProvider;

    @lombok.NonNull
    TypeElement service;

    @lombok.NonNull
    String batchMethodName;

    @lombok.NonNull
    TypeMirror batchMethodReturnType;
}


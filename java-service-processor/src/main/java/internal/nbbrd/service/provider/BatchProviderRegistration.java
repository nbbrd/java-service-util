package internal.nbbrd.service.provider;

import javax.lang.model.element.TypeElement;

/**
 * Registration info for a generated batch provider.
 */
@lombok.Value
class BatchProviderRegistration {

    @lombok.NonNull
    TypeElement batchService;

    @lombok.NonNull
    String providerClassName;
}


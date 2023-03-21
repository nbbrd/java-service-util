package internal.nbbrd.service;

import lombok.NonNull;

import javax.annotation.processing.ProcessingEnvironment;
import java.util.function.Supplier;

@lombok.RequiredArgsConstructor
public abstract class ProcessorTool {

    private final @NonNull Supplier<ProcessingEnvironment> envSupplier;

    @lombok.Getter(lazy = true)
    private final ExtEnvironment env = new ExtEnvironment(envSupplier.get());
}

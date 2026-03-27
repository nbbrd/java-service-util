/*
 * Copyright 2019 National Bank of Belgium
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */
package internal.nbbrd.service.definition;

import internal.nbbrd.service.Unreachable;
import lombok.NonNull;
import nbbrd.service.Mutability;

/**
 * @author Philippe Charles
 */
enum Lifecycle {

    IMMUTABLE, MUTABLE, CONCURRENT;

    static @NonNull Lifecycle of(@NonNull Mutability mutability) {
        switch (mutability) {
            case NONE:
                return IMMUTABLE;
            case BASIC:
                return MUTABLE;
            case CONCURRENT:
                return CONCURRENT;
            default:
                throw new Unreachable();
        }
    }

    boolean isAtomicReference() {
        return this == Lifecycle.CONCURRENT;
    }

    boolean isModifiable() {
        switch (this) {
            case MUTABLE:
            case CONCURRENT:
                return true;
            default:
                return false;
        }
    }

    boolean isThreadSafe() {
        switch (this) {
            case IMMUTABLE:
            case CONCURRENT:
                return true;
            default:
                return false;
        }
    }

    @NonNull
    Mutability toMutability() {
        switch (this) {
            case IMMUTABLE:
                return Mutability.NONE;
            case MUTABLE:
                return Mutability.BASIC;
            case CONCURRENT:
                return Mutability.CONCURRENT;
            default:
                throw new Unreachable();
        }
    }
}

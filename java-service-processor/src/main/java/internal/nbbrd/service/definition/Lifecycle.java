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
import nbbrd.service.Mutability;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * @author Philippe Charles
 */
enum Lifecycle {

    IMMUTABLE, MUTABLE, CONCURRENT, CONSTANT, ATOMIC, UNSAFE_MUTABLE;

    static @NonNull Lifecycle of(@NonNull Mutability mutability, boolean singleton) {
        switch (mutability) {
            case NONE:
                return singleton ? CONSTANT : IMMUTABLE;
            case BASIC:
                return singleton ? UNSAFE_MUTABLE : MUTABLE;
            case CONCURRENT:
                return singleton ? ATOMIC : CONCURRENT;
            default:
                throw new Unreachable();
        }
    }

    boolean isSingleton() {
        switch (this) {
            case CONSTANT:
            case UNSAFE_MUTABLE:
            case ATOMIC:
                return true;
            default:
                return false;
        }
    }

    boolean isAtomicReference() {
        switch (this) {
            case CONCURRENT:
            case ATOMIC:
                return true;
            default:
                return false;
        }
    }

    boolean isModifiable() {
        switch (this) {
            case MUTABLE:
            case UNSAFE_MUTABLE:
            case CONCURRENT:
            case ATOMIC:
                return true;
            default:
                return false;
        }
    }

    boolean isThreadSafe() {
        switch (this) {
            case IMMUTABLE:
            case CONCURRENT:
            case CONSTANT:
            case ATOMIC:
                return true;
            default:
                return false;
        }
    }

    @NonNull Mutability toMutability() {
        switch (this) {
            case IMMUTABLE:
            case CONSTANT:
                return Mutability.NONE;
            case MUTABLE:
            case UNSAFE_MUTABLE:
                return Mutability.BASIC;
            case CONCURRENT:
            case ATOMIC:
                return Mutability.CONCURRENT;
            default:
                throw new Unreachable();
        }
    }
}

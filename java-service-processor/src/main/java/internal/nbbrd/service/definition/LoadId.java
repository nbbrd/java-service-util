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

import internal.nbbrd.service.HasMethod;
import org.checkerframework.checker.index.qual.NonNegative;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.Optional;

/**
 * @author Philippe Charles
 */
@lombok.Value
class LoadId implements HasMethod {

    @lombok.NonNull
    ExecutableElement method;

    @lombok.NonNull
    Optional<TypeElement> serviceType;

    @lombok.NonNull
    String pattern;
}

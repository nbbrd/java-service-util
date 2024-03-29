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
package internal.nbbrd.service.provider;

import internal.nbbrd.service.HasPositionHint;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Philippe Charles
 */
@lombok.Value
class ProviderRef implements HasPositionHint {

    @lombok.NonNull
    TypeElement service;

    @lombok.NonNull
    TypeElement provider;

    @Override
    public Element getPositionHint() {
        return provider;
    }

    @Override
    public String toString() {
        return service + "/" + provider;
    }

    ProviderEntry toEntry() {
        return new ProviderEntry(service.getQualifiedName().toString(), provider.getQualifiedName().toString());
    }

    static Set<ProviderRef> getDuplicates(Collection<ProviderRef> refs) {
        return refs
                .stream()
                .filter(ref -> Collections.frequency(refs, ref) > 1)
                .collect(Collectors.toSet());
    }
}

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

import internal.nbbrd.service.Instantiator;
import java.util.List;
import java.util.Optional;
import javax.lang.model.type.TypeMirror;

/**
 *
 * @author Philippe Charles
 */
@lombok.Value
public class TypeInstantiator {

    private TypeMirror type;
    private List<Instantiator> instantiators;

    public Optional<Instantiator> select() {
        return instantiators.size() == 1
                ? Optional.of(instantiators.get(0))
                : instantiators.stream().filter(o -> o.getKind() == Instantiator.Kind.CONSTRUCTOR).findFirst();
    }
}

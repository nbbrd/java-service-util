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
package nbbrd.service.examples;

import java.util.Optional;
import javax.swing.JOptionPane;
import nbbrd.service.Mutability;
import nbbrd.service.ServiceDefinition;

/**
 *
 * @author Philippe Charles
 */
@ServiceDefinition(mutability = Mutability.BASIC)
public interface Messenger {

    void send(String message);

    static void main(String[] args) {
        MessengerLoader loader = new MessengerLoader();
        loader.get().ifPresent(o -> o.send("First"));

        loader.set(Optional.of(System.out::println));
        loader.get().ifPresent(o -> o.send("Second"));

        loader.set(Optional.of(msg -> JOptionPane.showMessageDialog(null, msg)));
        loader.get().ifPresent(o -> o.send("Third"));

        loader.reload();
        loader.get().ifPresent(o -> o.send("Fourth"));
    }
}

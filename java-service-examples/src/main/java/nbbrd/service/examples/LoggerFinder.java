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

import nbbrd.service.Quantifier;
import nbbrd.service.ServiceDefinition;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * @author Philippe Charles
 */
@ServiceDefinition(quantifier = Quantifier.SINGLE, fallback = LoggerFinder.FallbackLogger.class)
public interface LoggerFinder {

    Consumer<String> getLogger(String name);

    class FallbackLogger implements LoggerFinder {

        @Override
        public Consumer<String> getLogger(String name) {
            return msg -> System.out.printf(Locale.ROOT, "[%s] %s%n", name, msg);
        }
    }

    static void main(String[] args) {
        LoggerFinder single = LoggerFinderLoader.load();
        single.getLogger("MyClass").accept("some message");
    }
}

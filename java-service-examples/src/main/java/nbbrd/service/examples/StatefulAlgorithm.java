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

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.stream.Stream;
import nbbrd.service.ServiceDefinition;

/**
 *
 * @author Philippe Charles
 */
@ServiceDefinition(singleton = false)
public interface StatefulAlgorithm {

    void init(SecureRandom random);

    double compute(double... values);

    public static void main(String[] args) throws NoSuchAlgorithmException {
        StatefulAlgorithm algo1 = StatefulAlgorithmLoader.load().orElseThrow(RuntimeException::new);
        algo1.init(SecureRandom.getInstance("NativePRNG"));
        
        StatefulAlgorithm algo2 = StatefulAlgorithmLoader.load().orElseThrow(RuntimeException::new);
        algo2.init(SecureRandom.getInstance("PKCS11"));

        Stream.of(algo1, algo2)
                .parallel()
                .forEach(algo -> {
                    System.out.println(algo.compute(1, 2, 3));
                });
    }
}

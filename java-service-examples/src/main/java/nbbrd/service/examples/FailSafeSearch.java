/*
 * Copyright 2020 National Bank of Belgium
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

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Philippe Charles
 */
class FailSafeSearch implements FileSearch {

    private final FileSearch delegate;

    public FailSafeSearch(FileSearch delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<File> searchByName(String name) {
        try {
            return delegate.searchByName(name);
        } catch (RuntimeException unexpected) {
            Logger.getLogger(FailSafeSearch.class.getName())
                    .log(Level.WARNING, "Unexpected exception while searching for '" + name + "'", unexpected);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean isAvailable() {
        return delegate.isAvailable();
    }

    @Override
    public int getCost() {
        return delegate.getCost();
    }
}

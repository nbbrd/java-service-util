/*
 * Copyright 2017 National Bank of Belgium
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
package internal.nbbrd.service;

import com.squareup.javapoet.JavaFile;
import java.io.IOException;
import java.io.Writer;
import java.util.function.Supplier;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

/**
 *
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class ProcessorUtil {

    public void write(ProcessingEnvironment env, JavaFile jFile) {
        try (Writer w = env.getFiler().createSourceFile(jFile.packageName + "." + jFile.typeSpec.name).openWriter()) {
            jFile.writeTo(w);
        } catch (IOException ex) {
            env.getMessager().printMessage(Diagnostic.Kind.ERROR, "Could not generate file: " + ex.getMessage());
        }
    }

    // see http://hauchee.blogspot.be/2015/12/compile-time-annotation-processing-getting-class-value.html
    public static TypeMirror extractResultType(Supplier<Class<?>> type) {
        try {
            type.get();
            throw new RuntimeException("Expecting exeption to be raised");
        } catch (MirroredTypeException ex) {
            return ex.getTypeMirror();
        }
    }
}

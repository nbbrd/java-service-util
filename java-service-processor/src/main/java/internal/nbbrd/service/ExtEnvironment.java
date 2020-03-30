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
package internal.nbbrd.service;

import com.squareup.javapoet.ClassName;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

/**
 *
 * @author Philippe Charles
 */
@lombok.AllArgsConstructor
public final class ExtEnvironment implements ProcessingEnvironment {

    @lombok.experimental.Delegate
    private final ProcessingEnvironment delegate;

    public TypeElement asTypeElement(String o) {
        return delegate.getElementUtils().getTypeElement(o);
    }

    public TypeElement asTypeElement(Class o) {
        return delegate.getElementUtils().getTypeElement(o.getName());
    }

    public TypeElement asTypeElement(ClassName o) {
        return delegate.getElementUtils().getTypeElement(o.toString());
    }

    public TypeElement asTypeElement(TypeMirror o) {
        return (TypeElement) delegate.getTypeUtils().asElement(o);
    }

    public void error(Element annotatedElement, String message) {
        delegate.getMessager().printMessage(Diagnostic.Kind.ERROR, message, annotatedElement);
    }

    public void warn(Element annotatedElement, String message) {
        delegate.getMessager().printMessage(Diagnostic.Kind.WARNING, message, annotatedElement);
    }
}

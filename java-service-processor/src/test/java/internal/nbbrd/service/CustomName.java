/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package internal.nbbrd.service;

import javax.lang.model.element.Name;

/**
 *
 * @author charphi
 */
@lombok.Value
final class CustomName implements Name {

    @lombok.experimental.Delegate
    private final String content;

    @Override
    public String toString() {
        return content;
    }

    public static ProviderRef newRef(String service, String provider) {
        return new ProviderRef(new CustomName(service), new CustomName(provider));
    }
}

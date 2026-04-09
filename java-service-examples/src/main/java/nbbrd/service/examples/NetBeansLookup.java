package nbbrd.service.examples;

import org.openide.util.Lookup;

import java.util.Optional;
import java.util.function.Consumer;

import static nbbrd.service.examples.WinRegistry.HKEY_LOCAL_MACHINE;

public interface NetBeansLookup {

    static void main(String[] args) {
        Optional<WinRegistry> optional = WinRegistryLoader
                .builder()
                // 💡 NetBeans Lookup backend
                .backend(Lookup.getDefault()::lookupResult, Lookup.Result::allInstances)
                .build()
                .get();

        optional.map(reg -> reg.readString(
                        HKEY_LOCAL_MACHINE,
                        "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion",
                        "ProductName"))
                .ifPresent(System.out::println);
    }
}

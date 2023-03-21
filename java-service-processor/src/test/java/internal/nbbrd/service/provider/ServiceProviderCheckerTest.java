package internal.nbbrd.service.provider;

import com.google.testing.compile.CompilationRule;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;

import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;

import static internal.nbbrd.service.provider.ServiceProviderChecker.getMissingEntries;
import static internal.nbbrd.service.provider.ServiceProviderChecker.getMissingRefs;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

public class ServiceProviderCheckerTest {

    @Rule
    public CompilationRule compilationRule = new CompilationRule();

    @Test
    public void testGetMissingRefs() {
        ProviderRef charRef = ref(CharSequence.class, String.class);
        ProviderEntry charEntry = entry(CharSequence.class, String.class);

        ProviderRef listRef = ref(List.class, ArrayList.class);
        ProviderEntry listEntry = entry(List.class, ArrayList.class);

        Assertions.assertThat(getMissingRefs(emptyList(), emptyList()))
                .isEmpty();

        Assertions.assertThat(getMissingRefs(emptyList(), asList(charEntry, listEntry)))
                .isEmpty();

        Assertions.assertThat(getMissingRefs(asList(charRef), asList(charEntry, listEntry)))
                .isEmpty();

        Assertions.assertThat(getMissingRefs(asList(listRef), asList(charEntry, listEntry)))
                .isEmpty();

        Assertions.assertThat(getMissingRefs(asList(charRef), asList(listEntry)))
                .containsExactly(charRef);

        Assertions.assertThat(getMissingRefs(asList(charRef), emptyList()))
                .containsExactly(charRef);
    }

    @Test
    public void testGetMissingEntries() {
        ProviderRef charRef = ref(CharSequence.class, String.class);
        ProviderEntry charEntry = entry(CharSequence.class, String.class);

        ProviderRef listRef = ref(List.class, ArrayList.class);
        ProviderEntry listEntry = entry(List.class, ArrayList.class);

        Assertions.assertThat(getMissingEntries(emptyList(), emptyList()))
                .isEmpty();

        Assertions.assertThat(getMissingEntries(emptyList(), asList(charEntry, listEntry)))
                .containsExactly(charEntry, listEntry);

        Assertions.assertThat(getMissingEntries(asList(charRef), asList(charEntry, listEntry)))
                .containsExactly(listEntry);

        Assertions.assertThat(getMissingEntries(asList(listRef), asList(charEntry, listEntry)))
                .containsExactly(charEntry);

        Assertions.assertThat(getMissingEntries(asList(charRef), asList(listEntry)))
                .containsExactly(listEntry);

        Assertions.assertThat(getMissingEntries(asList(charRef), emptyList()))
                .isEmpty();
    }

    private TypeElement getTypeElement(Class<?> type) {
        return compilationRule.getElements().getTypeElement(type.getName());
    }

    private <T> ProviderRef ref(Class<T> service, Class<? extends T> provider) {
        return new ProviderRef(getTypeElement(service), getTypeElement(provider));
    }

    private <T> ProviderEntry entry(Class<T> service, Class<? extends T> provider) {
        return new ProviderEntry(service.getName(), provider.getName());
    }
}

package nbbrd.service.examples;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceFilter;

import javax.swing.*;
import javax.swing.plaf.metal.MetalIconFactory;

@ServiceDefinition(
        backend = NetBeansLookup.class,
        cleaner = NetBeansLookup.class
)
public interface IconProvider {

    enum CommonIcons {HOME_FOLDER}

    Icon getIconOrNull(CommonIcons icon);

    @ServiceFilter
    boolean isAvailable();

    @nbbrd.service.ServiceProvider
    final class MetalIconProvider implements IconProvider {

        @Override
        public Icon getIconOrNull(CommonIcons icon) {
            return icon == CommonIcons.HOME_FOLDER ? MetalIconFactory.getFileChooserHomeFolderIcon() : null;
        }

        @Override
        public boolean isAvailable() {
            return true;
        }
    }

    // FIXME: should play nice together instead we have
    //  "javax.annotation.processing.FilerException: Attempt to reopen a file for path"
//    @org.openide.util.lookup.ServiceProvider(service = IconProvider.class)
    final class NullProvider implements IconProvider {

        @Override
        public Icon getIconOrNull(CommonIcons icon) {
            return null;
        }

        @Override
        public boolean isAvailable() {
            return false;
        }
    }

    public static void main(String[] args) {
        JLabel message = new JLabel(CommonIcons.HOME_FOLDER.name());
        IconProviderLoader.load().map(provider -> provider.getIconOrNull(CommonIcons.HOME_FOLDER)).ifPresent(message::setIcon);
        JOptionPane.showMessageDialog(null, message);
    }
}

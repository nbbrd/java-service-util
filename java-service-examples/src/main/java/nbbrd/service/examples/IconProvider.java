package nbbrd.service.examples;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceProvider;

import javax.swing.*;
import javax.swing.plaf.metal.MetalIconFactory;

@ServiceDefinition(
        backend = NetBeansLookup.class,
        cleaner = NetBeansLookup.class
)
public interface IconProvider {

    enum CommonIcons {HOME_FOLDER}

    Icon getIconOrNull(CommonIcons icon);

    @ServiceProvider
    final class MetalIconProvider implements IconProvider {

        @Override
        public Icon getIconOrNull(CommonIcons icon) {
            return icon == CommonIcons.HOME_FOLDER ? MetalIconFactory.getFileChooserHomeFolderIcon() : null;
        }
    }

    public static void main(String[] args) {
        JLabel message = new JLabel(CommonIcons.HOME_FOLDER.name());
        IconProviderLoader.load().map(provider -> provider.getIconOrNull(CommonIcons.HOME_FOLDER)).ifPresent(message::setIcon);
        JOptionPane.showMessageDialog(null, message);
    }
}

package provider;

import nbbrd.service.ServiceProvider;

class EnumWithoutBatch {

    interface Color {
        String getName();
    }

    @ServiceProvider
    enum PrimaryColor implements Color {
        RED {
            @Override
            public String getName() { return "Red"; }
        },
        BLUE {
            @Override
            public String getName() { return "Blue"; }
        },
        YELLOW {
            @Override
            public String getName() { return "Yellow"; }
        }
    }
}


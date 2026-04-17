package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceId;

@ServiceDefinition
interface TestIdBuiltInEnum {

    enum Category { A, B }

    @ServiceId
    Category getCategory();
}


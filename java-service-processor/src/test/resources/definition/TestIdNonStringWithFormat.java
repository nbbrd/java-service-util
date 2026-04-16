package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceId;

@ServiceDefinition
interface TestIdNonStringWithFormat {

    enum Category { A, B }

    @ServiceId(formatMethodName = "name")
    Category getCategory();
}


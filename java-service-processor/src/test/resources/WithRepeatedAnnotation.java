
import nbbrd.service.ServiceProvider;

interface HelloService {
}

interface SomeService {
}

@ServiceProvider(HelloService.class)
@ServiceProvider(SomeService.class)
class Provider1 implements HelloService, SomeService {
}

@ServiceProvider(HelloService.class)
class Provider2 implements HelloService {
}

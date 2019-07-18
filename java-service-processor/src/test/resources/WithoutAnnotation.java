
class WithoutAnnotation {

    interface HelloService {
    }

    public static class Provider1 implements HelloService {
    }

    public static class Provider2 extends Provider1 {
    }
}

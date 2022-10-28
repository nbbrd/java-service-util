package experimental;

public @interface ClassName {

    /**
     * Specifies the simple name of the class. An empty value
     * generates an automatic name.
     *
     * @return a simple name
     */
    String simpleName() default "";

    /**
     * Specifies the package name of the class. An empty value
     * uses the package name of the service.
     *
     * @return a package name
     */
    String packageName() default "";
}

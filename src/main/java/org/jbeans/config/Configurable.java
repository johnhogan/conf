package org.jbeans.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 *
 * @author jhogan
 *
 * This custom annotation is used by CDI at runtime to support properties
 * dependency injection. Applications using the jbeans-config component will
 * use this annotation, which identify the appropriate provider to support
 * injection of Configurable property resources.
 *
 * EXAMPLE USE IN A CLASS:
 * @Inject @Configurable("jbeans.demo.string.prop") String demoProperty;
 *
 * @see com.my.package.Configurator#obtainConfigurableName(InjectionPoint ip)
 *
 * @author adam bien, blog.adam-bien.com
 */
@Target({ElementType.FIELD, ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Configurable {

    boolean optional() default false;

    String value();
}

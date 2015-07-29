//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.pointcuts;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @deprecated
 */
@LoadOnBootstrap
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface InterfaceMapper {
  String originalInterfaceName();

  String[] className() default {};

  String[] skip() default {};

  Class<?> classVisitor() default Object.class;
}

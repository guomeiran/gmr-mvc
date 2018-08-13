package com.gmr.mvc.framework.web.servlet.annotation;

import java.lang.annotation.*;

/**
 * User: hzguomeiran
 * Date: ${Date}
 * Iime: ${Time}
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GMRRequestMapping {

	String value() default "";
}

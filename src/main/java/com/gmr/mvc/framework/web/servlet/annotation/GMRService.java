package com.gmr.mvc.framework.web.servlet.annotation;

import java.lang.annotation.*;

/**
 * User: hzguomeiran
 * Date: ${Date}
 * Iime: ${Time}
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GMRService {

	String value() default "";
}

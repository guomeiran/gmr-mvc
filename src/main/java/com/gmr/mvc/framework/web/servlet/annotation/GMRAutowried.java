package com.gmr.mvc.framework.web.servlet.annotation;

import java.lang.annotation.*;

/**
 * User: hzguomeiran
 * Date: ${Date}
 * Iime: ${Time}
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GMRAutowried {

	String value() default "";
}

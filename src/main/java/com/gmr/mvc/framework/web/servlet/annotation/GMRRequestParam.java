package com.gmr.mvc.framework.web.servlet.annotation;

import java.lang.annotation.*;

/**
 * User: hzguomeiran
 * Date: ${Date}
 * Iime: ${Time}
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GMRRequestParam {

	String value() default "";
}

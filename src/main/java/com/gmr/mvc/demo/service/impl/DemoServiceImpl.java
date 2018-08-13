package com.gmr.mvc.demo.service.impl;

import com.gmr.mvc.demo.service.DemoService;
import com.gmr.mvc.framework.web.servlet.annotation.GMRService;

/**
 * User: hzguomeiran
 * Date: ${Date}
 * Iime: ${Time}
 */
@GMRService
public class DemoServiceImpl implements DemoService {

	@Override
	public String getName(String name) {
		return "My name is " + name;
	}
}

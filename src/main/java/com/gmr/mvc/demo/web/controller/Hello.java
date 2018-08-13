package com.gmr.mvc.demo.web.controller;

import com.gmr.mvc.demo.service.DemoService;
import com.gmr.mvc.framework.web.servlet.annotation.GMRController;
import com.gmr.mvc.framework.web.servlet.annotation.GMRAutowried;
import com.gmr.mvc.framework.web.servlet.annotation.GMRRequestMapping;
import com.gmr.mvc.framework.web.servlet.annotation.GMRRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * User: hzguomeiran
 * Date: ${Date}
 * Iime: ${Time}
 */
@GMRController
@GMRRequestMapping("/hello")
public class Hello {

	@GMRAutowried
	private DemoService demoService;

	@GMRRequestMapping("/query")
	public void query(HttpServletRequest request, HttpServletResponse response, @GMRRequestParam("name") String name) {

		String result = demoService.getName(name);
		try {
			response.getWriter().write(result);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

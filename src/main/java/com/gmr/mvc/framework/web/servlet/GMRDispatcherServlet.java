package com.gmr.mvc.framework.web.servlet;

import com.gmr.mvc.framework.web.servlet.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static jdk.nashorn.api.scripting.ScriptUtils.convert;

/**
 * User: hzguomeiran
 * Date: ${Date}
 * Iime: ${Time}
 */
public class GMRDispatcherServlet extends HttpServlet {

	private List<String> classNames = new ArrayList<String>();
	private Properties p = new Properties();
	private Map<String, Object> ioc = new HashMap<String, Object>();
	//private Map<String, Method> handleMapping = new HashMap<String, Method>();

	private  List<Handler> handleMapping = new ArrayList<Handler>();

	public GMRDispatcherServlet() {
		super();
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//		String requestURI = req.getRequestURI();
//		String contextPath = req.getContextPath();
//		String url = requestURI.replace(contextPath, "").replaceAll("/+", "/");
//		if (!handleMapping.containsKey(url)) {
//			resp.getWriter().write("404");
//			return;
//		}
//		Method method = handleMapping.get(url);
//
//		System.out.println(method);
		try {
			doDispatch(req, resp); //开始匹配到对应的方法
		} catch (Exception e) {
			resp.getWriter().write("500 Expception,Details:\r\n" + 
					Arrays.toString(e.getStackTrace()).replaceAll(
							"\\[|\\]", "").replaceAll(",\\s", "\r\n"));
		}
	}

	private void doDispatch(HttpServletRequest req, HttpServletResponse resp) {
		try{
			Handler handler = getHandler(req);

			if (handler == null) {
				// 如果没有匹配上，返回404 错误
				resp.getWriter().write("404 Not Found");
				return;
			}

			// 获取方法的参数列表
			Class<?>[] parameterTypes = handler.method.getParameterTypes();
			// 保存所有需要自动赋值的参数值
			Object[] paramValues = new Object[parameterTypes.length];
			Map<String, String[]> params = req.getParameterMap();
			for (Map.Entry<String, String[]> entry : params.entrySet()) {
				String value = Arrays.toString(entry.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", "\r\n");
				// 如果找到匹配的对象，则开始填充参数值
				if (!handler.paramIndexMapping.containsKey(entry.getKey())) { continue;}
				Integer index = handler.paramIndexMapping.get(entry.getKey());
				paramValues[index] = convert(value, parameterTypes[index]);
			}
			// 设置方法中的request 和 response 对象
			Integer reqIndex = handler.paramIndexMapping.get(HttpServletRequest.class.getName());
			paramValues[reqIndex] = req;
			Integer respIndex = handler.paramIndexMapping.get(HttpServletResponse.class.getName());
			paramValues[respIndex] = resp;

			handler.method.invoke(handler.controller, paramValues);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Handler getHandler(HttpServletRequest req) throws Exception {
		if (handleMapping.isEmpty()) { return null; }

		String requestURI = req.getRequestURI();
		String contextPath = req.getContextPath();
		String url = requestURI.replace(contextPath, "").replaceAll("/+", "/");

		for (Handler handler: handleMapping) {
			Matcher matcher = handler.pattern.matcher(url);
			if (!matcher.matches()) { continue; }
			return handler;
		}
		return null;
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		//加载配置文件
		doLoadConfig(config.getInitParameter("contextConfigLoaction"));

		//根据配置文件扫描所有的相关类
		doScanner(p.getProperty("scanPackage"));

		//初始化所有的相关类的实例，并将其放入TOC容器中
		doInstance();

		//实现自动依赖注入
		doAutowried();

		//初始化HandlerMapping
		initHandlerMapping();

	}

	private void doScanner(String packageName) {

		URL baseUrl = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));
		File classDir = new File(baseUrl.getFile());
		for (File file: classDir.listFiles()) {
			if (file.isDirectory()) {
				doScanner(packageName + "." + file.getName());
			} else {
				classNames.add(packageName + "." + file.getName().replace(".class", ""));
			}
		}
	}

	private void doAutowried() {
		if (ioc.isEmpty()) {return;}
		for (Map.Entry<String, Object> entry: ioc.entrySet()) {
			Field[] fields = entry.getValue().getClass().getDeclaredFields();
			for (Field field: fields) {
				if (!field.isAnnotationPresent(GMRAutowried.class)) {continue;}
				GMRAutowried gmrAutowried = field.getAnnotation(GMRAutowried.class);
				String beanName = gmrAutowried.value().trim();
				if ("".equals(beanName)) {
					beanName = field.getType().getName();
				}
				// 强制授权
				field.setAccessible(true);
				try {
					field.set(entry.getValue(), ioc.get(beanName));
				} catch (IllegalAccessException e) {
					e.printStackTrace();
					continue;
				}
			}
		}
	}

	private void initHandlerMapping() {
		if (ioc.isEmpty()) {return;}
		for (Map.Entry<String, Object> entry : ioc.entrySet()) {

			Class<?> clazz = entry.getValue().getClass();
			if (!clazz.isAnnotationPresent(GMRController.class)) {continue;}
			String baseUrl = "";
			if (clazz.isAnnotationPresent(GMRRequestMapping.class)) {
				GMRRequestMapping requestMapping = clazz.getAnnotation(GMRRequestMapping.class);
				baseUrl = requestMapping.value();
			}

//			Method[] methods = clazz.getMethods();
//			for (Method method : methods) {
//				if (!method.isAnnotationPresent(GMRRequestMapping.class)) {continue;}
//				String url = (baseUrl + method.getAnnotation(GMRRequestMapping.class).value()).replaceAll("/+", "/");
//				handleMapping.put(url, method);
//				System.out.println("url:" + url + "\tmethod:" + method.getName());
//			}
			Method[] methods = clazz.getMethods();
			for (Method method : methods) {
				//没有加requestMapping注解的直接忽略
				if (!method.isAnnotationPresent(GMRRequestMapping.class)) {continue;}
				String regex = ("/" + baseUrl + method.getAnnotation(GMRRequestMapping.class).value()).replaceAll("/+", "/");
				Pattern pattern = Pattern.compile(regex);
				handleMapping.add(new Handler(entry.getValue(), method, pattern));
				System.out.println("mapping " + regex + "," + method);
			}
		}
	}

	private void doInstance() {
		if(classNames.isEmpty()) {return;}
		// 反射机制，实例化
		try {
			for (String className : classNames) {
				Class<?> clazz = Class.forName(className);

				// bean 实例化阶段，初始化IOC容器
				// IOC容器规则
				// 1. key 默认类名，首字母小写
				if (clazz.isAnnotationPresent(GMRController.class)) {

					String beanName = lowerFirstCase(clazz.getSimpleName());
					ioc.put(beanName, clazz.newInstance());

				} else if (clazz.isAnnotationPresent(GMRService.class)) {
					// 2. 如果有自定义名称时，用自定义名称
					GMRService gmrService = clazz.getAnnotation(GMRService.class);
					String beanName = gmrService.value();
					if ("".equals(beanName)) {
						beanName = lowerFirstCase(clazz.getSimpleName());
					}
					ioc.put(beanName, clazz.newInstance());
					// 3. 如果是接口的话，无法实例化接口，这时候用接口类型作为key
					Class<?>[] interfaces = clazz.getInterfaces();
					for (Class<?> i: interfaces ) {
						ioc.put(i.getName(), clazz.newInstance());
					}
				} else {
					continue;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String lowerFirstCase(String beanName) {
		char[] chars = beanName.toCharArray();
		chars[0] += 32;
		return String.valueOf(chars);
	}

	private void doLoadConfig(String contextConfigLoaction) {
		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLoaction);
		try {
			p.load(inputStream);
		} catch (IOException e) {
			try {
				if (null != inputStream) {
					inputStream.close();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	private class Handler {
		protected Object controller; 	//保存对应的实例
		protected Method method; 		//保存映射的方法
		protected Pattern pattern;		// 规则
		protected Map<String, Integer> paramIndexMapping;	//参数顺序

		public Handler(Object controller, Method method, Pattern pattern) {
			this.controller = controller;
			this.method = method;
			this.pattern = pattern;
			
			this.paramIndexMapping = new HashMap<String, Integer>();
			putParamIndexMapping(method);
		}

		private void putParamIndexMapping(Method method) {
			//提取方法中加了注解的参数
			Annotation[][] pa = method.getParameterAnnotations();
			for (int i = 0; i < pa.length; i++) {
				for (Annotation a : pa[i]) {
					if (a instanceof GMRRequestParam) {
						String paramName = ((GMRRequestParam) a).value();
						if (!"".equals(paramName.trim())) {
							paramIndexMapping.put(paramName, i);
						}
					}
				}
			}

			//提取方法中的request和response参数
			Class<?>[] parameterTypes = method.getParameterTypes();
			for (int i = 0; i < parameterTypes.length; i++) {
				Class<?> parameterType = parameterTypes[i];
				if (parameterType == HttpServletRequest.class ||
						parameterType == HttpServletResponse.class) {
					paramIndexMapping.put(parameterType.getName(), i);
				}
			}
		}
	}
}

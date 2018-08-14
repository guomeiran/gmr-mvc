# gmr-mvc
自己手动编写spring mvc，源码研究
版本 0.0.1，最简单版本：
主要思路线：
          配置阶段：
                    1. web配置 servlet、servlet-mapping
                    2. DispatcherServlet 重载 HttpServlet，是 Spring Web 开发的入口
                    3. application.xml  配置Spring启动前所有要加载的Bean
                    4. url-pattern   方便匹配用户在浏览器输入的地址
          初始化阶段：
                    1. 重载 HttpServlet 中的 init()，web容器启动调用的初始化方法
                    2. 加载 application.xml
                    3. 初始化IOC容器，存储格式是Map<String, Object>
                    4. 依赖注入 @AutoWired
                    5. 初始化 HandlerMapping，存储格式是 Map<String, Method> 存储@RequestMapping配置URL
          运行时阶段：
                    1. service(Resquest, Response),只要用户请求，就会调用 doService方法
                    2. request.getURL,获取用户请求的url
                    3. 匹配URL和对应的Method
                    4. 利用Response将调用结果输出到浏览器


package howen.mvcframework.servlet;


import howen.mvcframework.annotation.HWAutowired;
import howen.mvcframework.annotation.HWController;
import howen.mvcframework.annotation.HWRequestMapping;
import howen.mvcframework.annotation.HWService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.io.*;

public class HWDispatcherServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String LOCATION = "contextConfigLocation";

    /**
     * 保存所有的配置信息
     */
    private Properties p = new Properties();

    /**
     * 保存所有被扫描的相关的类名
     */
    private List<String> classNames = new ArrayList<>();

    /**
     * 核心IOC容器，保存所有初始化的bean
     */
    private Map<String,Object> ioc = new HashMap<>();

    /**
     * 保存所有的Url和方法的映射关系
     */
    private Map<String, Method> handlerMapping = new HashMap<String,Method>();

    public HWDispatcherServlet() {
        super();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        //1. 加载配置文件
        doLoadConfig(config.getInitParameter(LOCATION));

        //2. 扫描所有的相关类
        doScanner(p.getProperty("scanPackage"));

        //3. 初始化所有相关类的实例，并保存到IOC容器中
        doInstance();

        //4. 依赖注入
        doAutowired();

        //5. 构造HandlerMapping
        initHandlerMapping();

        //6. 等待请求，匹配URL,定位方法，反射调用执行
        //调用doGet或者doPost方法

        //提示信息
        System.out.println("howen's mvcframework is inited");
    }

    private void initHandlerMapping() {
        if (ioc.isEmpty()){
            return;
        }

        for (Map.Entry<String,Object> entry : ioc.entrySet() ){
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(HWController.class)){
                continue;
            }
            String baseUrl = "";

            //获取Controller的url配置
            if (clazz.isAnnotationPresent(HWRequestMapping.class)){
                HWRequestMapping requestMapping = clazz.getAnnotation(HWRequestMapping.class);
                baseUrl = requestMapping.value();
            }

            //获取Method的url配置
            Method [] methods = clazz.getMethods();
            for (Method method :methods){

                //没有加RequestMapping注解的直接忽略
                if (!method.isAnnotationPresent(HWRequestMapping.class)){
                    continue;
                }

                //映射URL
                HWRequestMapping requestMapping = method.getAnnotation(HWRequestMapping.class);
                String url = ("/"+ baseUrl + "/" + requestMapping.value()).replaceAll("/+","/");
                handlerMapping.put(url,method);
                System.out.println("mapped"+ url +","+method);
            }
        }
    }

    private void doAutowired() {

        if (ioc.isEmpty()){
            return;
        }

        for (Map.Entry<String,Object> entry : ioc.entrySet()){
            //拿到实例对象中的所有属性
            Field[] fields = entry.getValue().getClass().getDeclaredFields();

            for (Field field : fields){
                 if (!field.isAnnotationPresent(HWAutowired.class)){
                     continue;
                 }
                 HWAutowired hwAutowired = field.getAnnotation(HWAutowired.class);
                 String beanName = hwAutowired.value().trim();
                 if ("".equals(beanName)){
                     //获取该实现类的接口名
                     beanName = field.getType().getInterfaces()[0].getName();
                 }
                 //设置私有属性的访问权限
                 field.setAccessible(true);

                 try {
                     field.set(entry.getValue(),ioc.get(beanName));
                 } catch (IllegalAccessException e) {
                     e.printStackTrace();
                     continue;
                 }

            }
        }
    }

    private void doInstance() {
        if (classNames.isEmpty()){
            return ;
        }
        try {
            for (String className : classNames) {
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(HWController.class)){
                    //默认将首字母小写作为beanName
                    String beanName = lowerFirstCase(clazz.getSimpleName());
                    ioc.put(beanName,clazz.newInstance());
                }else if (clazz.isAnnotationPresent(HWService.class)){
                    HWService hwService = clazz.getAnnotation(HWService.class);
                    String beanName = hwService.value();
                    //如果用户设置了名字，就用用户自己设置
                    if (!"".equals(beanName.trim())){
                        ioc.put(beanName, clazz.newInstance());
                        continue;
                    }

                    //如果自己没设，就按照接口类型创建一个实例
                    Class<?>[] interfaces = clazz.getInterfaces();
                    for (Class<?> i : interfaces){
                        ioc.put(i.getName(),clazz.newInstance());
                    }

                }else{
                    continue;
                }
            }
        } catch (ClassNotFoundException e) {
                e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }

    }

    private void doScanner(String scanPackage) {

        //将所有的包路径转换为文件路径
        URL url = this.getClass().getClassLoader().getResource("/"+scanPackage.replaceAll("\\.","/"));

        File dir = new File(url.getFile());

        for (File file : dir.listFiles()){
            //如果是文件夹，继续递归
            if (file.isDirectory()){
                doScanner(scanPackage+"."+file.getName());
            }else{
                classNames.add(scanPackage+"."+file.getName().replace(".class","").trim());
            }
        }
    }

    private void doLoadConfig(String location) {

        InputStream fis = null;

        try {
            fis = this.getClass().getClassLoader().getResourceAsStream("system.properties");
            p.load(fis);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try{
                if (null != fis){
                    fis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String lowerFirstCase(String string){
        char [] chars = string.toCharArray();
        chars[0] += 32;
        return  String.valueOf(chars);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            //开始匹配到对应的方法
            this.doDispatch(req,resp);
        }catch (Exception e){
            resp.getWriter().write("500 Exception,Details:\r\n"+Arrays.toString(e.getStackTrace())
            .replaceAll("\\[|\\]","").replaceAll(",\\s","\r\n"));
        }

    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (this.handlerMapping.isEmpty()){
            return;
        }

        String url = req.getRequestURI();
        String contextPath = req.getContextPath();

        url = url.replace(contextPath,"").replaceAll("/+","/");
        if (!this.handlerMapping.containsKey(url)){
            resp.getWriter().write("404 Not Found!!");
            return;
        }

        Map<String, String[]> params = req.getParameterMap();
        Method method = this.handlerMapping.get(url);

        //获取方法的参数列表
        Class<?> [] parameterTypes = method.getParameterTypes();
        //获取请求的参数
        Map<String,String[]> parameterMap = req.getParameterMap();
        //保存参数值
        Object [] paramValues = new Object[parameterTypes.length];
        //方法的参数列表
        for (int i = 0; i< parameterTypes.length;i++){
            //根据参数名称，做一些处理
            Class parameterType = parameterTypes[i];
            if (parameterType == HttpServletRequest.class){
                //参数类型已经明确，这边强转类型
                paramValues[i] = req;
                continue;
            }else if (parameterType == HttpServletResponse.class){
                paramValues[i] = resp;
                continue;
            }else if(parameterType == String.class){
                for (Map.Entry<String, String[]> param : parameterMap.entrySet()) {
                    String value = Arrays.toString(param.getValue())
                            .replaceAll("\\[|\\]]","")
                            .replaceAll(",\\s",",");
                    paramValues[i] = value;
                }
            }
        }
        try{
            String beanName = lowerFirstCase(method.getDeclaringClass().getSimpleName());
            //利用反射机制来调用
            method.invoke(this.ioc.get(beanName),paramValues);
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}

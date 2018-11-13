package howen.demo.controller;

import howen.demo.service.impl.DemoServiceImpl;
import howen.mvcframework.annotation.HWAutowired;
import howen.mvcframework.annotation.HWController;
import howen.mvcframework.annotation.HWRequestMapping;
import howen.mvcframework.annotation.HWRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@HWController
@HWRequestMapping("/demo")
public class DemoController {

    @HWAutowired
    DemoServiceImpl demoService;


    @HWRequestMapping("/query")
    public void query(HttpServletRequest request, HttpServletResponse response,
                      @HWRequestParam("name") String name){
        System.out.println(name);
        demoService.printHello();
    }
}

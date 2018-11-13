package howen.demo.service.impl;


import howen.demo.service.IDemoService;
import howen.mvcframework.annotation.HWService;

@HWService
public class DemoServiceImpl implements IDemoService {

    @Override
    public void printHello() {
        System.out.println("hello world");
    }
}

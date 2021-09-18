package com.lagou.rpc.consumer.proxy;

import com.lagou.rpc.consumer.annotation.RpcReference;
import com.lagou.rpc.consumer.controller.UserController;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Component
public class RpcBeanPostProcessor implements BeanPostProcessor {

    @Value("${zookeeper.host}")
    private String zkHost;

    @Value("${zookeeper.port}")
    private int zkPort;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

        Field[] declaredFields = bean.getClass().getDeclaredFields();


        for (Field declaredField : declaredFields) {

            if(declaredField.isAnnotationPresent(RpcReference.class)){

                Class<?> declaringClass = declaredField.getType();

                System.out.println(declaringClass);

                Object proxy = RpcClientProxy.createProxy(declaringClass,zkHost,zkPort);

                declaredField.setAccessible(true);

                try {
                    declaredField.set(bean,proxy);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

                return bean;

            }
        }

        return bean;
    }
}

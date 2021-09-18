package com.lagou.rpc.provider.zk;

import com.lagou.rpc.provider.anno.RpcService;
import com.lagou.rpc.provider.server.RpcServer;
import org.I0Itec.zkclient.ZkClient;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class ServiceRegister implements BeanPostProcessor {


    @Value("${zookeeper.host}")
    private String zkHost;

    @Value("${zookeeper.port}")
    private int zkPort;

    @Autowired
    private Environment environment;


    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

        boolean annotationPresent = bean.getClass().isAnnotationPresent(RpcService.class);

        if(annotationPresent){

            try {
                register(bean);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

        return bean;
    }

    public void register(Object bean) throws UnknownHostException {

        Class<?>[] interfaces = bean.getClass().getInterfaces();

        Class<?> anInterface = interfaces[0];

        String applicationName = anInterface.getName();

        String path = "/"+applicationName;

        //连接zookeeper
        ZkClient zkClient = new ZkClient(zkHost,zkPort);
        boolean exists = zkClient.exists(path);
        if(!exists){
            zkClient.createPersistent(path);
        }

        InetAddress addr = InetAddress.getLocalHost();

        String host = "127.0.0.1";
        int port = Integer.parseInt(environment.getProperty("server.port"));

        zkClient.createEphemeral(path+"/"+host+"-"+port);

    }

}

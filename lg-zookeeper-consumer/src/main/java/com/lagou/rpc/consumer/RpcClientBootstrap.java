package com.lagou.rpc.consumer;

import com.lagou.rpc.api.IUserService;
import com.lagou.rpc.consumer.proxy.RpcClientProxy;
import com.lagou.rpc.pojo.User;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class RpcClientBootstrap {

    public static void main(String[] args) {

        SpringApplication.run(RpcClientBootstrap.class,args);

    }
}

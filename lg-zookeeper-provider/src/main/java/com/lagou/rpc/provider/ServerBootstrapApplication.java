package com.lagou.rpc.provider;

import com.lagou.rpc.provider.server.RpcServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class ServerBootstrapApplication implements CommandLineRunner {

    @Autowired
    private RpcServer rpcServer;

    @Autowired
    private Environment environment;

    public static void main(String[] args) {

        SpringApplication.run(ServerBootstrapApplication.class,args);
    }

    @Override
    public void run(String... args) throws Exception {

        new Thread(() -> {
            System.out.println("开始启动服务器");
            rpcServer.start("127.0.0.1", Integer.parseInt(environment.getProperty("server.port")));
        }).start();
    }
}

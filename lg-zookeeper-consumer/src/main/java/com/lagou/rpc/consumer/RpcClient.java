package com.lagou.rpc.consumer;


import com.lagou.rpc.consumer.handler.RpcClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class RpcClient {

    private String ip;

    private int port;

    NioEventLoopGroup group;

    RpcClientHandler rpcClientHandler = new RpcClientHandler();

    Channel channel;

    private ExecutorService executorService = Executors.newCachedThreadPool();

    public RpcClient(String ip, int port) {
        this.ip = ip;
        this.port = port;
        initClient();
    }

    public void initClient(){

        try {
            group = new NioEventLoopGroup();

            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE,Boolean.TRUE)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,3000)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            pipeline.addLast(new StringDecoder());
                            pipeline.addLast(new StringEncoder());

                            pipeline.addLast(rpcClientHandler);
                        }
                    });


            channel = bootstrap.connect(ip, port).sync().channel();

        }catch (Exception e){

            e.printStackTrace();

            if(channel!=null)
                channel.close();

            if(group!=null)
                group.shutdownGracefully();

        }

    }

    //关闭连接
    public void close(){
        if(channel!=null)
            channel.close();

        if(group!=null)
            group.shutdownGracefully();

    }


    //发送消息
    public Object send(String msg) throws ExecutionException, InterruptedException {

        rpcClientHandler.setRequestMsg(msg);
        Future submit = executorService.submit(rpcClientHandler);

        return submit.get();
    }



}

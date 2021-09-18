package com.lagou.rpc.provider.server;

import com.lagou.rpc.provider.handler.RpcServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.I0Itec.zkclient.ZkClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
public class RpcServer implements DisposableBean {

    private NioEventLoopGroup bossGroup;

    private NioEventLoopGroup workerGroup;

    @Autowired
    private RpcServerHandler rpcServerHandler;



    public void start(String ip,int port)  {

        try {
            bossGroup = new NioEventLoopGroup(1);

            workerGroup = new NioEventLoopGroup();

            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup,workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {

                            ChannelPipeline pipeline = socketChannel.pipeline();

                            pipeline.addLast(new StringDecoder());
                            pipeline.addLast(new StringEncoder());


                            //业务处理类
                            pipeline.addLast(rpcServerHandler);

                        }
                    });


            ChannelFuture channelFuture = serverBootstrap.bind(ip, port).sync();

            System.out.println("服务端启动成功");

            channelFuture.channel().closeFuture().sync();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            if(bossGroup!=null)
                bossGroup.shutdownGracefully();

            if(workerGroup!=null)
                workerGroup.shutdownGracefully();

        }

    }

    @Override
    public void destroy() throws Exception {

        if(bossGroup!=null)
            bossGroup.shutdownGracefully();

        if(workerGroup!=null)
            workerGroup.shutdownGracefully();

    }
}

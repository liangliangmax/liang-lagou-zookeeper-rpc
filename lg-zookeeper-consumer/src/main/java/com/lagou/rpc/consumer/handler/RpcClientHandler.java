package com.lagou.rpc.consumer.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.concurrent.Callable;

/**
 * 发送消息
 * 接受消息
 */
public class RpcClientHandler extends SimpleChannelInboundHandler<String> implements Callable {

    private ChannelHandlerContext channelHandlerContext;

    //要发送的消息
    private String requestMsg;

    //读取服务端返回的数据
    private String responseMsg;

    public void setRequestMsg(String requestMsg) {
        this.requestMsg = requestMsg;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        channelHandlerContext = ctx;

    }

    /**
     * 通道读取事件
     * @param channelHandlerContext
     * @param msg
     * @throws Exception
     */
    @Override
    protected synchronized void channelRead0(ChannelHandlerContext channelHandlerContext, String msg) throws Exception {

        responseMsg = msg;

        //唤醒等待的线程
        notify();
    }


    /**
     * 发送消息到服务端
     * @return
     * @throws Exception
     */
    @Override
    public synchronized Object call() throws Exception {

        channelHandlerContext.writeAndFlush(requestMsg);

        //线程等待
        wait();

        return responseMsg;
    }
}

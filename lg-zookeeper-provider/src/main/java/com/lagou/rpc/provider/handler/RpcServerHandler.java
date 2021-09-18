package com.lagou.rpc.provider.handler;

import com.alibaba.fastjson.JSON;
import com.lagou.rpc.common.RpcRequest;
import com.lagou.rpc.common.RpcResponse;
import com.lagou.rpc.provider.anno.RpcService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.beans.BeansException;
import org.springframework.cglib.reflect.FastClass;
import org.springframework.cglib.reflect.FastMethod;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端的处理类
 * 1. 将表有@RpcService注解的bean进行缓存
 * 2. 接受客户端的请求
 * 3. 根据传过来的beanName从缓存中获取对应的bean
 * 4. 解析请求中的方法名 参数类型 参数信息
 * 5. 反射调用bean的方法
 * 6. 给客户端进行响应
 *
 *
 */
@Component
@ChannelHandler.Sharable
public class RpcServerHandler extends SimpleChannelInboundHandler<String> implements ApplicationContextAware {

    private Map<String,Object> serviceMap = new ConcurrentHashMap<>();

    /**
     * 将表有@RpcService注解的bean进行缓存
     *
     * @param applicationContext
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

        Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(RpcService.class);

        beansWithAnnotation.forEach((key,value)->{

            Class<?>[] interfaces = value.getClass().getInterfaces();

            if(interfaces.length == 0){
                throw new RuntimeException("服务必须实现接口");
            }

            String name = interfaces[0].getName();

            serviceMap.put(name,value);

        });

    }

    /**
     * 通道就绪事件
     * @param channelHandlerContext
     * @param s
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String msg) throws Exception {

        //接受客户端的请求
        RpcRequest rpcRequest = JSON.parseObject(msg, RpcRequest.class);
        RpcResponse rpcResponse = new RpcResponse();

        rpcResponse.setRequestId(rpcRequest.getRequestId());

        try {
            //业务处理
            rpcResponse.setResult(handle(rpcRequest));
        }catch (Exception e){

            e.printStackTrace();
            rpcResponse.setError(e.getMessage());
        }


        // 6. 给客户端进行响应
        channelHandlerContext.writeAndFlush(JSON.toJSONString(rpcResponse));


    }

    /**
     * 3. 根据传过来的beanName从缓存中获取对应的bean
     * 4. 解析请求中的方法名 参数类型 参数信息
     * 5. 反射调用bean的方法
     * @param rpcRequest
     * @return
     */
    private Object handle(RpcRequest rpcRequest) throws InvocationTargetException {

        Object serviceBean = serviceMap.get(rpcRequest.getClassName());

        if(serviceBean == null){

            throw new RuntimeException("找不到对应的服务，beanName:"+rpcRequest.getClassName());
        }

        Class<?> serviceBeanClass = serviceBean.getClass();

        String methodName = rpcRequest.getMethodName();
        Class<?>[] parameterTypes = rpcRequest.getParameterTypes();

        Object[] parameters = rpcRequest.getParameters();

        //使用cglib创建代理对象
        FastClass fastClass = FastClass.create(serviceBeanClass);
        FastMethod method = fastClass.getMethod(methodName, parameterTypes);

        return method.invoke(serviceBean, parameters);

    }


}

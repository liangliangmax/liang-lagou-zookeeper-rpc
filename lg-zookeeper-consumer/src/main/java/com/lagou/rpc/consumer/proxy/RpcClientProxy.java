package com.lagou.rpc.consumer.proxy;

import com.alibaba.fastjson.JSON;
import com.lagou.rpc.common.RpcRequest;
import com.lagou.rpc.common.RpcResponse;
import com.lagou.rpc.consumer.RpcClient;
import lombok.Data;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.ZkClient;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RpcClientProxy {

    private static int count = 0;

    static List<NetAddress> addressList = new ArrayList<>();

    public static Object createProxy(Class serviceClass,String zkHost,int zkPort){

        String serviceName = serviceClass.getName();
        ZkClient zkClient = new ZkClient(zkHost,zkPort);

        List<String> children = zkClient.getChildren("/" + serviceName);

        zkClient.subscribeChildChanges("/"+serviceName,new IZkChildListener(){

            @Override
            public void handleChildChange(String parentPath, List<String> list) throws Exception {

                System.out.println(parentPath+"节点发生变化，最新的节点信息是"+JSON.toJSONString(list));
                children.clear();
                children.addAll(list);
            }
        });

        return Proxy.newProxyInstance(
            Thread.currentThread().getContextClassLoader(),
            new Class[]{serviceClass},

            (proxy, method, args) -> {
                
                //1.封装request请求对象
                RpcRequest rpcRequest = new RpcRequest();
                rpcRequest.setRequestId(UUID.randomUUID().toString());
                rpcRequest.setClassName(method.getDeclaringClass().getName());
                rpcRequest.setMethodName(method.getName());
                rpcRequest.setParameterTypes(method.getParameterTypes());
                rpcRequest.setParameters(args);

                //动态获取服务地址
                if(CollectionUtils.isEmpty(children)){

                    throw new RuntimeException(serviceName+"没有可用节点");
                }

                children.stream().forEach(item->{

                    String[] split = item.split("-");

                    addressList.add(new NetAddress(split[0],Integer.parseInt(split[1])));

                });


                //正常的话这里应该把发送的逻辑抽出去，判断addressList是否为空，为空就不发送了，
                int index = count%addressList.size();

                System.out.println("从"+addressList.get(index)+"处请求数据");

                //2.创建rpcClient对象
                RpcClient rpcClient = new RpcClient(addressList.get(index).getIp(),addressList.get(index).getPort());

                try {
                    //3.发送消息
                    Object responseMsg = rpcClient.send(JSON.toJSONString(rpcRequest));

                    RpcResponse rpcResponse = JSON.parseObject(responseMsg.toString(), RpcResponse.class);

                    if(rpcResponse.getError()!=null){
                        throw new RuntimeException(rpcResponse.getError());
                    }

                    //4.返回结果
                    Object result = rpcResponse.getResult();

                    return JSON.parseObject(result.toString(),method.getReturnType());
                }catch (Exception e){
                    throw e;

                }finally {
                    count++;
                    rpcClient.close();
                }

            });

    }


    @Data
    static class NetAddress{

        private String ip;

        private int port;

        public NetAddress(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }
    }

}

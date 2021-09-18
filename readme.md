### 项目说明
本项目是在上一次netty做rpc远程调用的基础上，加上了zookeeper服务自动发现以及负载均衡。




lg-zookeeper-rpc-api为接口层，提供了公共的api接口

lg-zookeeper-rpc-provider为服务端，使用netty提供接口服务；

lg-zookeeper-rpc-consumer为消费端，注入api接口，在需要的地方注入api即可



1. provider中添加了启动时候注册zookeeper的方法

```java
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
```

添加beanPostProcessor，判断每个标注了RpcService注解的bean，如果标记了，获取bean的接口的类名（com.lagou.rpc.api.IUserService），在zookeeper中创建永久节点。然后在永久节点下面创建临时节点，节点的名称就是ip+port。这样，zookeeper中就有了服务器的节点信息

```
/com.lagou.rpc.api.IUserService/127.0.0.1-9901
/com.lagou.rpc.api.IUserService/127.0.0.1-9902
```



2. 客户端启动时候要创建动态代理对象，在创建代理对象时候，就需要为其连接zookeeper，然后注册临时节点的动态监听事件。

```
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
```

当有节点上线时候，会动态添加到服务器列表，然后通过轮询算法实现负载均衡的功能。





在浏览器中输入http://localhost:8080/user/getUserById?id=2 ， id可以是1或者2。

在com.lagou.rpc.consumer.controller.UserController中会接受到请求，然后调用IUserService生成的代理对象去调用服务端，在创建代理对象时候里面维护了一个简单的服务器列表，正常这个列表应该是从zookeeper或者别的注册中心获取，这里简单就直接写死了。



发送请求的时候采用轮询机制，轮询服务器列表中的地址，向指定的地址发送请求。控制台会打印出

```
从RpcClientProxy.NetAddress(ip=127.0.0.1, port=9900)处请求数据
从RpcClientProxy.NetAddress(ip=127.0.0.1, port=9901)处请求数据
从RpcClientProxy.NetAddress(ip=127.0.0.1, port=9900)处请求数据
从RpcClientProxy.NetAddress(ip=127.0.0.1, port=9901)处请求数据
从RpcClientProxy.NetAddress(ip=127.0.0.1, port=9900)处请求数据
```

页面会显示出结果

```
{"id":2,"name":"李四"}
```

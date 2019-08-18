package com.sxl.study.shop.service.impl;

import com.sxl.study.shop.reqeust.ProductIndex;
import com.sxl.study.shop.reqeust.Request;
import com.sxl.study.shop.reqeust.RequestQueue;
import com.sxl.study.shop.service.RequestUpdateAsyncProcessService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class RequestUpdateAsyncProcessServiceImpl implements RequestUpdateAsyncProcessService {

    /**
     * 如果系统是分布式部署，则根据一定规则将商品更新请求发到本机器
     * 无论是单机器还是多机器，需要保证一点：相同id商品的请求一定分发到相同的机器中。
     * 写请求一定进队列
     * @param request
     */
    @Override
    public void process(Request request) {
        RequestQueue requestQueue = RequestQueue.getInstance();
        Map<String, ConcurrentLinkedQueue<ProductIndex>> flagMap = requestQueue.getFlagMap();
        String key=RequestQueue.getPrefix()+request.getProductId();
        String readKey=RequestQueue.getWrite()+request.getProductId();
        try {
            // 做请求的路由，根据每个请求的商品id，路由到对应的内存队列中去
            ArrayBlockingQueue<Request> queue = getRoutingQueue(request.getProductId());
            // 将请求放入对应的队列中，完成路由操作
            queue.put(request);

            ProductIndex index = new ProductIndex.Builder()
                    .setPid(readKey)
                    .setFlag(true)
                    .build();
            if(flagMap.containsKey(key)){
                flagMap.get(key).add(index);
            }else{
                ConcurrentLinkedQueue<ProductIndex> val = new ConcurrentLinkedQueue<>();
                val.add(index);
                flagMap.put(key,val);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取路由到的内存队列
     * @param key
     * @return 内存队列
     */
    private ArrayBlockingQueue<Request> getRoutingQueue(String key) {
        RequestQueue requestQueue = RequestQueue.getInstance();
        // 先获取productId的hash值
        int h;
        int hash = (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);

        // 对hash值取模，将hash值路由到指定的内存队列中，比如内存队列大小8
        // 用内存队列的数量对hash值取模之后，结果一定是在0~7之间
        // 所以任何一个商品id都会被固定路由到同样的一个内存队列中去的
        int index = (requestQueue.queueSize() - 1) & hash;
        return requestQueue.getQueue(index);
    }
}

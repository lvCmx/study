package com.sxl.study.shop.service.impl;

import com.sxl.study.shop.reqeust.ProductIndex;
import com.sxl.study.shop.reqeust.Request;
import com.sxl.study.shop.reqeust.RequestQueue;
import com.sxl.study.shop.service.RequestReadAsyncProcessService;
import com.sxl.study.shop.service.RequestUpdateAsyncProcessService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class RequestReadAsycProcessServiceImpl implements RequestReadAsyncProcessService {
    /**
     * 它与更新不同的是，它需要判断它之前是否有写请求写入到队列
     *  有：则将读请求加入队列
     *  无：则读redis，再无就请求数据库
     * @param request
     * @return
     */
    @Override
    public Object process(Request request) {
        RequestQueue requestQueue = RequestQueue.getInstance();
        ArrayBlockingQueue<Request> queue = getRoutingQueue(request.getProductId());
        // 将请求放入对应的队列中，完成路由操作
        Map<String, ConcurrentLinkedQueue<ProductIndex>> flagMap = requestQueue.getFlagMap();
        String key=RequestQueue.getPrefix()+request.getProductId();
        String readKey=RequestQueue.getRead()+request.getProductId();
        String writeKey=RequestQueue.getWrite()+request.getProductId();
        if(!flagMap.containsKey(key)){
            return "None";
        }
        ConcurrentLinkedQueue<ProductIndex> productIndices = flagMap.get(key);
        Iterator<ProductIndex> iterator = productIndices.iterator();
        int read=0,write=0,flag=0;
        while(iterator.hasNext()){
            flag++;
            ProductIndex producelement = iterator.next();
            String pkey = producelement.getPid();
            if(StringUtils.isEmpty(pkey)){
                continue;
            }
            // 如果存在读操作，则直接返回，等待
            if(readKey.equals(pkey)){
                read=flag;
            }else if(writeKey.equals(pkey)){
                write=flag;
            }
        }
        // 读写，，读读（x），写写(x)
        // 写读
        if(read<write){
            try {
                queue.put(request);
                ProductIndex index = new ProductIndex.Builder().setPid(readKey).setFlag(false).build();
                productIndices.add(index);
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        // 需要使用原子变量，生成唯一
        return "rtn";
    }

    /**
     * 获取路由到的内存队列
     * @param key 商品id
     * @return 内存队列
     */
    private ArrayBlockingQueue<Request> getRoutingQueue(String key) {
        RequestQueue requestQueue = RequestQueue.getInstance();
        int h;
        int hash = (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
        // 对hash值取模，将hash值路由到指定的内存队列中，比如内存队列大小8
        // 用内存队列的数量对hash值取模之后，结果一定是在0~7之间
        // 所以任何一个商品id都会被固定路由到同样的一个内存队列中去的
        int index = (requestQueue.queueSize() - 1) & hash;
        return requestQueue.getQueue(index);
    }
}

package com.sxl.study.shop.controller;

import com.sxl.study.shop.common.ResponseVo;
import com.sxl.study.shop.common.ReturnStatus;
import com.sxl.study.shop.model.Product;
import com.sxl.study.shop.reqeust.ProductCacheRefreshRequest;
import com.sxl.study.shop.reqeust.ProductDBUpdateRequest;
import com.sxl.study.shop.reqeust.Request;
import com.sxl.study.shop.service.ProductService;
import com.sxl.study.shop.service.RequestReadAsyncProcessService;
import com.sxl.study.shop.service.RequestUpdateAsyncProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/product")
public class ProductController {
    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);
    @Autowired
    private ProductService productService;
    @Autowired
    private RequestUpdateAsyncProcessService requestUpdateAsyncProcessService;
    @Autowired
    private RequestReadAsyncProcessService requestReadAsyncProcessService;
    /**
     * 问题：
     * 一个更新商品库存的请求过来，然后此时会删除redis中的缓存，另一个请求过来要查询商品，此时redis可能为空，则会再向数据库发送请求
     * 如果此时，第一个更新请求还没有执行数据库，则第二个请求则是查询的旧数据。
     * 通过队列的方式，将请求串行化
     * 模拟场景：
    *  （1）一个更新商品库存的请求过来，然后此时会先删除redis中的缓存，然后模拟卡顿5秒钟
    *  （2）在这个卡顿的5秒钟内，我们发送一个商品缓存的读请求，因为此时redis中没有缓存，就会来请求将数据库中最新的数据刷新到缓存中
    *  （3）此时读请求会路由到同一个内存队列中，阻塞住，不会执行
    *  （4）等5秒钟过后，写请求完成了数据库的更新之后，读请求才会执行
    *  （5）读请求执行的时候，会将最新的库存从数据库中查询出来，然后更新到缓存中
    *
    *  如果是不一致的情况，可能会出现说redis中还是库存为100，但是数据库中也许已经更新成了库存为99了
    *  现在做了一致性保障的方案之后，就可以保证说，数据是一致的
     */
    @RequestMapping(value = "/update",method = {RequestMethod.POST})
    public ResponseVo updateProductInventory(Product product){
        ResponseVo result=null;
        try {
            Request request = new ProductDBUpdateRequest(product, productService);
            requestUpdateAsyncProcessService.process(request);
            result=new ResponseVo(ReturnStatus.SC_OK.getValue(),ReturnStatus.SC_OK.getDesc());
        } catch (Exception e) {
            e.printStackTrace();
            result=new ResponseVo(ReturnStatus.SC_INTERNAL_SERVER_ERROR.getValue(),
                    ReturnStatus.SC_INTERNAL_SERVER_ERROR.getDesc());
        }
        return result;
    }

    /**
     * 读库存
     * 1.先判断map中此前是否有写操作，如果没有，则读缓存。
     */
    @RequestMapping(value = "/findById/{id}")
    public ResponseVo<Product> findByPid(@PathVariable String id){
        ResponseVo<Product> result=null;
        try {
            Request request = new ProductCacheRefreshRequest(id,productService,false);
            String process = (String) requestReadAsyncProcessService.process(request);
            // 将请求扔给service异步去处理以后，就需要while(true)一会儿，在这里hang住
            // 去尝试等待前面有商品库存更新的操作，同时缓存刷新的操作，将最新的数据刷新到缓存中
            long startTime = System.currentTimeMillis();
            long endTime = 0L;
            long waitTime = 0L;
            // 等待超过200ms没有从缓存中获取到结果
            Product product=null;
            while(true) {
                // 一般公司里面，面向用户的读请求控制在200ms就可以了
                if(waitTime > 200) {
                    break;
                }
                // 尝试去redis中读取一次商品库存的缓存数据
                product = productService.getProductInventoryCache(id);

                // 如果读取到了结果，那么就返回
                if(product != null) {
                    result=new ResponseVo(ReturnStatus.SC_OK.getValue(),ReturnStatus.SC_OK.getDesc(),product);
                    return result;
                } else {  // 如果没有读取到结果，那么等待一段时间
                    Thread.sleep(20);
                    endTime = System.currentTimeMillis();
                    waitTime = endTime - startTime;
                }
            }
            // 直接尝试从数据库中读取数据
            product = productService.findProductInventory(id);
            if(product != null) {
                // 将缓存刷新一下
                // 这个过程，实际上是一个读操作的过程，但是没有放在队列中串行去处理，还是有数据不一致的问题
                request = new ProductCacheRefreshRequest(
                        id, productService, true);
                requestReadAsyncProcessService.process(request);
                // 代码会运行到这里，只有三种情况：
                // 1、就是说，上一次也是读请求，数据刷入了redis，但是redis LRU算法给清理掉了，标志位还是false
                // 所以此时下一个读请求是从缓存中拿不到数据的，再放一个读Request进队列，让数据去刷新一下
                // 2、可能在200ms内，就是读请求在队列中一直积压着，没有等待到它执行（在实际生产环境中，基本是比较坑了）
                // 所以就直接查一次库，然后给队列里塞进去一个刷新缓存的请求
                // 3、数据库里本身就没有，缓存穿透，穿透redis，请求到达mysql库
                result=new ResponseVo(ReturnStatus.SC_OK.getValue(),ReturnStatus.SC_OK.getDesc(),product);
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        result=new ResponseVo(ReturnStatus.SC_INTERNAL_SERVER_ERROR.getValue(),
                ReturnStatus.SC_INTERNAL_SERVER_ERROR.getDesc());
        return result;
    }
}

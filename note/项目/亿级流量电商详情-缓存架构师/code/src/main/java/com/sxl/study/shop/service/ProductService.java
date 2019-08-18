package com.sxl.study.shop.service;

import com.sxl.study.shop.model.Product;

public interface ProductService {
    /**
     * 更新商品库存
     * @param product 商品库存
     */
    void updateProductInventory(Product product);

    /**
     * 删除Redis中的商品库存的缓存
     * @param product 商品库存
     */
    void removeProductInventoryCache(Product product);

    /**
     * 根据商品id查询商品库存
     * @param pId 商品id
     * @return 商品库存
     */
    Product findProductInventory(String pId);

    /**
     * 设置商品库存的缓存
     * @param product 商品库存
     */
    void setProductInventoryCache(Product product);

    /**
     * 获取商品库存的缓存
     * @param productId
     * @return
     */
    Product getProductInventoryCache(String productId);

}

package com.sxl.study.shop.service.impl;

import com.sxl.study.shop.model.Product;
import com.sxl.study.shop.service.ProductService;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl implements ProductService {

    @Override
    public void updateProductInventory(Product product) {

    }

    @Override
    public void removeProductInventoryCache(Product product) {

    }

    @Override
    public Product findProductInventory(String pId) {
        return null;
    }

    @Override
    public void setProductInventoryCache(Product product) {

    }

    @Override
    public Product getProductInventoryCache(String productId) {
        return null;
    }
}

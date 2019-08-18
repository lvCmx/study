package com.sxl.study.shop.reqeust;

import com.sxl.study.shop.model.Product;
import com.sxl.study.shop.service.ProductService;

/**
 * 重新加载商品库存的缓存
 * @author Administrator
 *
 */
public class ProductCacheRefreshRequest implements Request {

	/**
	 * 商品id
	 */
	private String productId;
	/**
	 * 商品库存Service
	 */
	private ProductService productService;
	/**
	 * 是否强制刷新缓存
	 */
	private boolean forceRefresh;
	
	public ProductCacheRefreshRequest(String productId, ProductService productService,
									  boolean forceRefresh) {
		this.productId = productId;
		this.productService = productService;
		this.forceRefresh = forceRefresh;
	}
	
	@Override
	public void process() {
		// 从数据库中查询最新的商品库存数量
		Product product = productService.findProductInventory(productId);
		// 将最新的商品库存数量，刷新到redis缓存中去
		productService.setProductInventoryCache(product);
	}
	
	public String getProductId() {
		return productId;
	}

	public boolean isForceRefresh() {
		return forceRefresh;
	}
	
}

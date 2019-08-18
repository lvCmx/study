package com.sxl.study.shop.reqeust;

import com.sxl.study.shop.model.Product;
import com.sxl.study.shop.service.ProductService;
import org.springframework.transaction.annotation.Transactional;

/**
 * 比如说一个商品发生了交易，那么就要修改这个商品对应的库存
 * 此时就会发送请求过来，要求修改库存，那么这个可能就是所谓的data update request，数据更新请求
 * cache aside pattern
 * （1）删除缓存
 * （2）更新数据库
 */
public class ProductDBUpdateRequest implements Request {

	/**
	 * 商品库存
	 */
	private Product product;
	/**
	 * 商品库存Service
	 */
	private ProductService productService;
	
	public ProductDBUpdateRequest(Product product, ProductService productService) {
		this.product = product;
		this.productService = productService;
	}
	
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void process() {
		// 删除redis中的缓存
		productService.removeProductInventoryCache(product);
		// 修改数据库中的库存
		productService.updateProductInventory(product);
	}
	
	/**
	 * 获取商品id
	 */
	public String getProductId() {
		return product.getPid();
	}

	@Override
	public boolean isForceRefresh() {
		return false;
	}
}

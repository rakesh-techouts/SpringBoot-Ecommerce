package com.techouts.service;

import com.techouts.entity.Products;

import java.util.List;

public interface ProductService {
    List<Products> getProducts(String category);

    Products getById(Long productId);
}

package com.techouts.service.impl;

import com.techouts.entity.Products;
import com.techouts.repository.ProductRepository;
import com.techouts.service.ProductService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public List<Products> getProducts(String category) {
        if (category == null || category.isBlank() || "All".equalsIgnoreCase(category)) {
            return productRepository.findAll();
        }
        return productRepository.findByCategory(category);
    }

    @Override
    public Products getById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
    }
}

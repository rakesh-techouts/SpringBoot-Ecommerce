package com.techouts.controller;

import com.techouts.entity.Products;
import com.techouts.service.CartItemsService;
import com.techouts.service.ProductService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class ProductController {

    private final ProductService productService;
    private final CartItemsService cartItemsService;

    public ProductController(ProductService productService, CartItemsService cartItemsService) {
        this.productService = productService;
        this.cartItemsService = cartItemsService;
    }

    @GetMapping({"/", "/index"})
    public String index(@RequestParam(defaultValue = "All") String category, 
                        @RequestParam(required = false) String search, Model model) {
        List<Products> products;
        if (search != null && !search.trim().isEmpty()) {
            products = productService.searchProductsByName(search.trim());
        } else {
            products = productService.getProducts(category);
        }
        model.addAttribute("products", products);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("search", search);
        return "user/index";
    }

    @GetMapping("/home")
    public String home(@RequestParam(defaultValue = "All") String category, 
                       @RequestParam(required = false) String search, HttpSession session, Model model) {
        if (session.getAttribute("USER_ID") == null) {
            return "redirect:/login";
        }
        List<Products> products;
        if (search != null && !search.trim().isEmpty()) {
            products = productService.searchProductsByName(search.trim());
        } else {
            products = productService.getProducts(category);
        }
        model.addAttribute("products", products);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("search", search);
        model.addAttribute("userName", session.getAttribute("USER_NAME"));
        
        // Add cart count
        Long userId = (Long) session.getAttribute("USER_ID");
        if (userId != null) {
            int cartCount = cartItemsService.getCartItemCount(userId);
            model.addAttribute("cartCount", cartCount);
        }
        
        return "user/home";
    }

    @GetMapping("/product/{id}")
    public String productDetails(@PathVariable Long id, HttpSession session, Model model) {
        Products product = productService.getById(id);
        model.addAttribute("product", product);
        model.addAttribute("isLoggedIn", session.getAttribute("USER_ID") instanceof Long);
        model.addAttribute("userName", session.getAttribute("USER_NAME"));
        
        // Add cart count
        Long userId = (Long) session.getAttribute("USER_ID");
        if (userId != null) {
            int cartCount = cartItemsService.getCartItemCount(userId);
            model.addAttribute("cartCount", cartCount);
        }
        
        return "user/product-details";
    }
}

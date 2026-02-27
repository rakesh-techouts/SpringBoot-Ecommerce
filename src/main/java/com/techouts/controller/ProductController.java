package com.techouts.controller;

import com.techouts.entity.Products;
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

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping({"/", "/index"})
    public String index(@RequestParam(defaultValue = "All") String category, Model model) {
        List<Products> products = productService.getProducts(category);
        model.addAttribute("products", products);
        model.addAttribute("selectedCategory", category);
        return "index";
    }

    @GetMapping("/home")
    public String home(@RequestParam(defaultValue = "All") String category, HttpSession session, Model model) {
        if (session.getAttribute("USER_ID") == null) {
            return "redirect:/login";
        }
        List<Products> products = productService.getProducts(category);
        model.addAttribute("products", products);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("userName", session.getAttribute("USER_NAME"));
        return "home";
    }

    @GetMapping("/product/{id}")
    public String productDetails(@PathVariable Long id, HttpSession session, Model model) {
        Products product = productService.getById(id);
        model.addAttribute("product", product);
        model.addAttribute("isLoggedIn", session.getAttribute("USER_ID") instanceof Long);
        model.addAttribute("userName", session.getAttribute("USER_NAME"));
        return "product-details";
    }
}

package com.techouts.controller;

import com.techouts.entity.User;
import com.techouts.entity.UserRole;
import com.techouts.service.CartItemsService;
import com.techouts.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
public class UserController {

    private final UserService userService;
    private final CartItemsService cartItemsService;

    public UserController(UserService userService, CartItemsService cartItemsService) {
        this.userService = userService;
        this.cartItemsService = cartItemsService;
    }

    @GetMapping("/register")
    public String registerPage() {
        return "user/register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String name,
                           @RequestParam String username,
                           @RequestParam String email,
                           @RequestParam String phone,
                           @RequestParam String password,
                           @RequestParam String confirmPassword,
                           Model model) {
        try {
            userService.register(name, username, email, phone, password, confirmPassword);
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "user/register";
        }
    }

    @GetMapping("/login")
    public String loginPage() {
        return "user/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String identifier,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {
        return userService.login(identifier, password)
                .map(user -> {
                    session.setAttribute("USER_ID", user.getId());
                    session.setAttribute("USER_NAME", user.getName());
                    session.setAttribute("IS_ADMIN", isAdmin(user));
                    if (isAdmin(user)) {
                        return "redirect:/admin/products";
                    }
                    return "redirect:/home";
                })
                .orElseGet(() -> {
                    model.addAttribute("error", "Invalid username/email/phone or password");
                    return "user/login";
                });
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/index";
    }

    @GetMapping("/profile")
    public String profilePage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("USER_ID");
        if (userId == null) {
            return "redirect:/login";
        }
        
        User user = userService.findById(userId);
        model.addAttribute("user", user);
        
        // Add cart count
        int cartCount = cartItemsService.getCartItemCount(userId);
        model.addAttribute("cartCount", cartCount);
        
        return "user/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam String name,
                               @RequestParam String username,
                               @RequestParam String email,
                               @RequestParam(required = false) String phone,
                               @RequestParam(required = false) String address,
                               @RequestParam(required = false) String gender,
                               @RequestParam(required = false) String dateOfBirth,
                               @RequestParam(required = false) MultipartFile profilePictureFile,
                               HttpSession session,
                               Model model) {
        Long userId = (Long) session.getAttribute("USER_ID");
        if (userId == null) {
            return "redirect:/login";
        }
        
        try {
            userService.updateProfile(userId, name, username, email, phone, address, gender, dateOfBirth, profilePictureFile);
            User updated = userService.findById(userId);
            session.setAttribute("USER_NAME", updated.getName());
            session.setAttribute("IS_ADMIN", isAdmin(updated));
            model.addAttribute("success", "Profile updated successfully!");
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
        } catch (Exception ex) {
            model.addAttribute("error", "Error updating profile: " + ex.getMessage());
        }
        
        User user = userService.findById(userId);
        model.addAttribute("user", user);
        model.addAttribute("cartCount", cartItemsService.getCartItemCount(userId));
        return "user/profile";
    }

    private boolean isAdmin(User user) {
        if (user.getRole() != null) {
            return user.getRole() == UserRole.ROLE_ADMIN;
        }
        return "admin".equalsIgnoreCase(user.getUsername());
    }
}

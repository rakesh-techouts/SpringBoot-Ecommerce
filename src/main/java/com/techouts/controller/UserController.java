package com.techouts.controller;

import com.techouts.entity.User;
import com.techouts.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String name,
                           @RequestParam String email,
                           @RequestParam String phone,
                           @RequestParam String password,
                           Model model) {
        try {
            userService.register(name, email, phone, password);
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "register";
        }
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
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
                    return "redirect:/home";
                })
                .orElseGet(() -> {
                    model.addAttribute("error", "Invalid email/phone or password");
                    return "login";
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
        return "profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam String name,
                               @RequestParam String email,
                               @RequestParam String phone,
                               @RequestParam(required = false) String address,
                               @RequestParam(required = false) String profilePicture,
                               HttpSession session,
                               Model model) {
        Long userId = (Long) session.getAttribute("USER_ID");
        if (userId == null) {
            return "redirect:/login";
        }
        
        try {
            userService.updateProfile(userId, name, email, phone, address, profilePicture);
            session.setAttribute("USER_NAME", name);
            model.addAttribute("success", "Profile updated successfully!");
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        
        User user = userService.findById(userId);
        model.addAttribute("user", user);
        return "profile";
    }
}

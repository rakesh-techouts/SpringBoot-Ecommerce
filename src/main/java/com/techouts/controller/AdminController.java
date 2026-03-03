package com.techouts.controller;

import com.techouts.entity.Order;
import com.techouts.entity.OrderStatus;
import com.techouts.entity.Products;
import com.techouts.entity.Gender;
import com.techouts.entity.User;
import com.techouts.entity.UserRole;
import com.techouts.repository.OrderRepository;
import com.techouts.repository.ProductRepository;
import com.techouts.repository.UserRepository;
import com.techouts.service.OrderService;
import com.techouts.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class AdminController {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final OrderService orderService;

    public AdminController(ProductRepository productRepository,
                           OrderRepository orderRepository,
                           UserRepository userRepository,
                           UserService userService,
                           OrderService orderService) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.orderService = orderService;
    }

    @GetMapping("/admin")
    public String adminRoot(HttpSession session) {
        String redirect = guardAdmin(session);
        if (redirect != null) {
            return redirect;
        }
        return "redirect:/admin/products";
    }

    @GetMapping("/admin/products")
    public String products(@RequestParam(defaultValue = "created") String sort,
                           @RequestParam(required = false) String search,
                           @RequestParam(defaultValue = "ALL") String category,
                           HttpSession session,
                           Model model) {
        String redirect = guardAdmin(session);
        if (redirect != null) {
            return redirect;
        }
        List<Products> products;
        if (search != null && !search.trim().isEmpty()) {
            products = productRepository.findByNameContainingIgnoreCase(search.trim());
        } else {
            products = productRepository.findAll();
        }

        if (category != null && !"ALL".equalsIgnoreCase(category)) {
            products = products.stream()
                    .filter(p -> p.getCategory() != null && p.getCategory().equalsIgnoreCase(category))
                    .collect(Collectors.toList());
        }

        switch (sort) {
            case "categoryAsc" -> products.sort((a, b) -> a.getCategory().compareToIgnoreCase(b.getCategory()));
            case "categoryDesc" -> products.sort((a, b) -> b.getCategory().compareToIgnoreCase(a.getCategory()));
            default -> {
            }
        }
        Set<String> categories = productRepository.findAll().stream()
                .map(Products::getCategory)
                .filter(c -> c != null && !c.isBlank())
                .collect(Collectors.toCollection(java.util.TreeSet::new));
        model.addAttribute("products", products);
        model.addAttribute("selectedSort", sort);
        model.addAttribute("search", search == null ? "" : search.trim());
        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategory", category == null ? "ALL" : category);
        return "admin/admin-products";
    }

    @GetMapping("/admin/products/new")
    public String newProduct(HttpSession session, Model model) {
        String redirect = guardAdmin(session);
        if (redirect != null) {
            return redirect;
        }
        model.addAttribute("product", new Products());
        model.addAttribute("isEdit", false);
        return "admin/admin-product-form";
    }

    @GetMapping("/admin/products/{id}/edit")
    public String editProduct(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes ra) {
        String redirect = guardAdmin(session);
        if (redirect != null) {
            return redirect;
        }
        Products product = productRepository.findById(id).orElse(null);
        if (product == null) {
            ra.addFlashAttribute("error", "Product not found");
            return "redirect:/admin/products";
        }
        model.addAttribute("product", product);
        model.addAttribute("isEdit", true);
        return "admin/admin-product-form";
    }

    @GetMapping("/admin/products/{id}")
    public String productDetails(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes ra) {
        String redirect = guardAdmin(session);
        if (redirect != null) {
            return redirect;
        }
        Products product = productRepository.findById(id).orElse(null);
        if (product == null) {
            ra.addFlashAttribute("error", "Product not found");
            return "redirect:/admin/products";
        }
        model.addAttribute("product", product);
        return "admin/admin-product-details";
    }

    @PostMapping("/admin/products/save")
    public String saveProduct(@RequestParam(required = false) Long id,
                              @RequestParam String name,
                              @RequestParam String category,
                              @RequestParam BigDecimal price,
                              @RequestParam Integer stock,
                              @RequestParam String description,
                              @RequestParam String imageUrl,
                              HttpSession session,
                              RedirectAttributes ra) {
        String redirect = guardAdmin(session);
        if (redirect != null) {
            return redirect;
        }
        try {
            validateProduct(name, category, price, stock, description, imageUrl);
            Products product = (id == null)
                    ? new Products()
                    : productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Product not found"));
            product.setName(name.trim());
            product.setCategory(category.trim());
            product.setPrice(price);
            product.setStock(stock);
            product.setDescription(description.trim());
            product.setImageUrl(imageUrl.trim());
            productRepository.save(product);
            ra.addFlashAttribute("success", id == null ? "Product created" : "Product updated");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/admin/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        String redirect = guardAdmin(session);
        if (redirect != null) {
            return redirect;
        }
        if (!productRepository.existsById(id)) {
            ra.addFlashAttribute("error", "Product not found");
            return "redirect:/admin/products";
        }
        productRepository.deleteById(id);
        ra.addFlashAttribute("success", "Product deleted");
        return "redirect:/admin/products";
    }

    @PostMapping("/admin/products/{id}/stock")
    public String updateStock(@PathVariable Long id,
                              @RequestParam Integer stock,
                              HttpSession session,
                              RedirectAttributes ra) {
        String redirect = guardAdmin(session);
        if (redirect != null) {
            return redirect;
        }
        if (stock == null || stock < 0) {
            ra.addFlashAttribute("error", "Stock must be 0 or greater");
            return "redirect:/admin/products";
        }
        Products product = productRepository.findById(id).orElse(null);
        if (product == null) {
            ra.addFlashAttribute("error", "Product not found");
            return "redirect:/admin/products";
        }
        product.setStock(stock);
        productRepository.save(product);
        ra.addFlashAttribute("success", "Stock updated");
        return "redirect:/admin/products";
    }

    @GetMapping("/admin/orders")
    public String orders(@RequestParam(required = false) String status,
                         @RequestParam(required = false) String userQuery,
                         @RequestParam(defaultValue = "createdAtDesc") String sort,
                         HttpSession session,
                         Model model) {
        String redirect = guardAdmin(session);
        if (redirect != null) {
            return redirect;
        }
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();

        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            orders = orders.stream()
                    .filter(o -> o.getStatus() != null && o.getStatus().name().equalsIgnoreCase(status))
                    .collect(Collectors.toList());
        }

        if (userQuery != null && !userQuery.isBlank()) {
            String q = userQuery.trim().toLowerCase(Locale.ROOT);
            orders = orders.stream()
                    .filter(o -> {
                        User u = o.getUser();
                        if (u == null) {
                            return false;
                        }
                        boolean idMatch = String.valueOf(u.getId()).equals(q);
                        boolean usernameMatch = u.getUsername() != null && u.getUsername().toLowerCase(Locale.ROOT).contains(q);
                        boolean emailMatch = u.getEmail() != null && u.getEmail().toLowerCase(Locale.ROOT).contains(q);
                        boolean phoneMatch = u.getPhone() != null && u.getPhone().toLowerCase(Locale.ROOT).contains(q);
                        return idMatch || usernameMatch || emailMatch || phoneMatch;
                    })
                    .collect(Collectors.toList());
        }

        switch (sort) {
            case "createdAtAsc" -> orders.sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));
            case "statusAsc" -> orders.sort((a, b) -> safeStatus(a).compareTo(safeStatus(b)));
            case "statusDesc" -> orders.sort((a, b) -> safeStatus(b).compareTo(safeStatus(a)));
            default -> {
            }
        }

        model.addAttribute("orders", orders);
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("selectedStatus", status == null || status.isBlank() ? "ALL" : status);
        model.addAttribute("userQuery", userQuery == null ? "" : userQuery.trim());
        model.addAttribute("selectedSort", sort);
        return "admin/admin-orders";
    }

    @GetMapping("/admin/users")
    public String users(@RequestParam(required = false) String search, HttpSession session, Model model) {
        String redirect = guardAdmin(session);
        if (redirect != null) {
            return redirect;
        }
        List<User> users = userRepository.findAll();
        if (search != null && !search.trim().isEmpty()) {
            String q = search.trim().toLowerCase(Locale.ROOT);
            users = users.stream()
                    .filter(u -> String.valueOf(u.getId()).equals(q)
                            || (u.getUsername() != null && u.getUsername().toLowerCase(Locale.ROOT).contains(q))
                            || (u.getEmail() != null && u.getEmail().toLowerCase(Locale.ROOT).contains(q))
                            || (u.getPhone() != null && u.getPhone().toLowerCase(Locale.ROOT).contains(q))
                            || (u.getName() != null && u.getName().toLowerCase(Locale.ROOT).contains(q)))
                    .collect(Collectors.toList());
        }
        model.addAttribute("users", users);
        model.addAttribute("search", search == null ? "" : search.trim());
        model.addAttribute("roles", UserRole.values());
        return "admin/admin-users";
    }

    @GetMapping("/admin/users/{id}/edit")
    public String editUser(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes ra) {
        String redirect = guardAdmin(session);
        if (redirect != null) {
            return redirect;
        }
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            ra.addFlashAttribute("error", "User not found");
            return "redirect:/admin/users";
        }
        model.addAttribute("user", user);
        model.addAttribute("genders", Gender.values());
        return "admin/admin-user-form";
    }

    @PostMapping("/admin/users/{id}/save")
    public String saveUser(@PathVariable Long id,
                           @RequestParam String name,
                           @RequestParam String username,
                           @RequestParam String email,
                           @RequestParam(required = false) String phone,
                           @RequestParam(required = false) String address,
                           @RequestParam(required = false) String gender,
                           @RequestParam(required = false) String dateOfBirth,
                           @RequestParam(required = false) String profilePicture,
                           HttpSession session,
                           RedirectAttributes ra) {
        String redirect = guardAdmin(session);
        if (redirect != null) {
            return redirect;
        }
        try {
            User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
            String normalizedName = normalizeName(name);
            String normalizedUsername = normalizeUsername(username);
            String normalizedEmail = normalizeEmail(email);
            String normalizedPhone = normalizePhone(phone);

            userRepository.findByUsername(normalizedUsername).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new IllegalArgumentException("Username already exists");
                }
            });
            userRepository.findByEmail(normalizedEmail).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new IllegalArgumentException("Email already exists");
                }
            });
            if (normalizedPhone != null) {
                userRepository.findByPhone(normalizedPhone).ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new IllegalArgumentException("Phone number already exists");
                    }
                });
            }

            user.setName(normalizedName);
            user.setUsername(normalizedUsername);
            user.setEmail(normalizedEmail);
            user.setPhone(normalizedPhone);
            user.setAddress(address == null || address.trim().isEmpty() ? null : address.trim());
            user.setProfilePicture(profilePicture == null || profilePicture.trim().isEmpty() ? null : profilePicture.trim());

            if (gender == null || gender.isBlank()) {
                user.setGender(null);
            } else {
                user.setGender(Gender.valueOf(gender.toUpperCase(Locale.ROOT)));
            }

            if (dateOfBirth == null || dateOfBirth.isBlank()) {
                user.setDateOfBirth(null);
            } else {
                user.setDateOfBirth(LocalDate.parse(dateOfBirth));
            }

            userRepository.save(user);
            ra.addFlashAttribute("success", "User updated successfully");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Failed to update user");
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        String redirect = guardAdmin(session);
        if (redirect != null) {
            return redirect;
        }
        Object currentUserId = session.getAttribute("USER_ID");
        if (currentUserId instanceof Long && ((Long) currentUserId).equals(id)) {
            ra.addFlashAttribute("error", "Admin cannot delete own account");
            return "redirect:/admin/users";
        }
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            ra.addFlashAttribute("error", "User not found");
            return "redirect:/admin/users";
        }
        long ordersCount = orderRepository.countByUserId(id);
        if (ordersCount > 0) {
            ra.addFlashAttribute("error", "Cannot delete user with order history");
            return "redirect:/admin/users";
        }
        userRepository.delete(user);
        ra.addFlashAttribute("success", "User deleted successfully");
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/{id}/role")
    public String updateUserRole(@PathVariable Long id,
                                 @RequestParam UserRole role,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        String redirect = guardAdmin(session);
        if (redirect != null) {
            return redirect;
        }

        try {
            User target = userRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            Long currentUserId = (Long) session.getAttribute("USER_ID");

            if (target.getRole() == UserRole.ROLE_ADMIN
                    && role == UserRole.ROLE_USER
                    && currentUserId != null
                    && currentUserId.equals(id)) {
                throw new IllegalArgumentException("You cannot remove your own admin role");
            }

            if (target.getRole() == UserRole.ROLE_ADMIN
                    && role == UserRole.ROLE_USER
                    && userRepository.countByRole(UserRole.ROLE_ADMIN) <= 1) {
                throw new IllegalArgumentException("At least one admin is required");
            }

            target.setRole(role);
            userRepository.save(target);

            if (currentUserId != null && currentUserId.equals(id)) {
                session.setAttribute("IS_ADMIN", role == UserRole.ROLE_ADMIN);
            }

            ra.addFlashAttribute("success", "Role updated to " + role.name());
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/admin/orders/{id}/status")
    public String updateOrderStatus(@PathVariable Long id,
                                    @RequestParam OrderStatus status,
                                    HttpSession session,
                                    RedirectAttributes ra) {
        String redirect = guardAdmin(session);
        if (redirect != null) {
            return redirect;
        }
        try {
            Order order = orderRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Order not found"));
            OrderStatus current = order.getStatus() == null ? OrderStatus.PLACED : order.getStatus();

            if (current == OrderStatus.CANCELLED || current == OrderStatus.DELIVERED) {
                throw new IllegalArgumentException("Cannot modify delivered/cancelled order");
            }

            if (status == OrderStatus.CANCELLED) {
                orderService.cancelOrder(order.getUser().getId(), id);
                ra.addFlashAttribute("success", "Order cancelled and stock restored");
                return "redirect:/admin/orders";
            }

            if (!isForwardTransition(current, status)) {
                throw new IllegalArgumentException("Invalid status transition");
            }

            order.setStatus(status);
            orderRepository.save(order);
            ra.addFlashAttribute("success", "Order status updated to " + status);
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/orders";
    }

    private boolean isForwardTransition(OrderStatus current, OrderStatus next) {
        if (next == OrderStatus.PLACED) {
            return current == OrderStatus.PLACED;
        }
        if (next == OrderStatus.PACKED) {
            return current == OrderStatus.PLACED || current == OrderStatus.PACKED;
        }
        if (next == OrderStatus.SHIPPED) {
            return current == OrderStatus.PACKED || current == OrderStatus.SHIPPED;
        }
        if (next == OrderStatus.DELIVERED) {
            return current == OrderStatus.SHIPPED || current == OrderStatus.DELIVERED;
        }
        return false;
    }

    private String safeStatus(Order order) {
        return order.getStatus() == null ? OrderStatus.PLACED.name() : order.getStatus().name();
    }

    private String normalizeName(String name) {
        if (name == null || name.trim().length() < 2 || name.trim().length() > 60) {
            throw new IllegalArgumentException("Name must be between 2 and 60 characters");
        }
        return name.trim();
    }

    private String normalizeUsername(String username) {
        if (username == null) {
            throw new IllegalArgumentException("Username is required");
        }
        String normalized = username.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() < 3 || normalized.length() > 20) {
            throw new IllegalArgumentException("Username must be between 3 and 20 characters");
        }
        if (!normalized.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("Username can only contain letters, numbers, and underscores");
        }
        return normalized;
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("Email is required");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank() || !normalized.contains("@") || normalized.startsWith("@") || normalized.endsWith("@")) {
            throw new IllegalArgumentException("Valid email is required");
        }
        return normalized;
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }
        String normalized = phone.replaceAll("[^0-9]", "");
        if (!normalized.matches("\\d{10}")) {
            throw new IllegalArgumentException("Phone number must be exactly 10 digits");
        }
        return normalized;
    }

    private void validateProduct(String name, String category, BigDecimal price, Integer stock, String description, String imageUrl) {
        if (name == null || name.trim().length() < 2) {
            throw new IllegalArgumentException("Product name must be at least 2 characters");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Category is required");
        }
        if (price == null || price.signum() < 0) {
            throw new IllegalArgumentException("Price must be 0 or greater");
        }
        if (stock == null || stock < 0) {
            throw new IllegalArgumentException("Stock must be 0 or greater");
        }
        if (description == null || description.trim().length() < 5) {
            throw new IllegalArgumentException("Description must be at least 5 characters");
        }
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Image URL is required");
        }
    }

    private String guardAdmin(HttpSession session) {
        Object userId = session.getAttribute("USER_ID");
        if (!(userId instanceof Long id)) {
            return "redirect:/login";
        }
        User user = userService.findById(id);
        if (!isAdmin(user)) {
            return "redirect:/home";
        }
        return null;
    }

    private boolean isAdmin(User user) {
        if (user.getRole() != null) {
            return user.getRole() == UserRole.ROLE_ADMIN;
        }
        return "admin".equalsIgnoreCase(user.getUsername());
    }
}

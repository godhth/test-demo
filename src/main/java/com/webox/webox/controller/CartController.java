package com.webox.webox.controller;

import com.webox.webox.service.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/cart")
    public String view(HttpSession session, Model model) {
        model.addAttribute("cart", cartService.view(session));
        return "cart";
    }

    @PostMapping("/cart/add")
    public String add(@RequestParam Long menuItemId,
                      @RequestParam(defaultValue = "1") int quantity,
                      @RequestParam(defaultValue = "/menu") String redirect,
                      HttpSession session) {
        cartService.add(session, menuItemId, quantity);
        return "redirect:" + redirect;
    }

    @PostMapping("/cart/update")
    public String update(@RequestParam Long menuItemId,
                         @RequestParam int quantity,
                         HttpSession session) {
        cartService.update(session, menuItemId, quantity);
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String remove(@RequestParam Long menuItemId, HttpSession session) {
        cartService.remove(session, menuItemId);
        return "redirect:/cart";
    }
}

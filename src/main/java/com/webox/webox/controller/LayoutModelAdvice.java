package com.webox.webox.controller;

import com.webox.webox.model.LoginUser;
import com.webox.webox.service.CartService;
import com.webox.webox.support.SessionKeys;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class LayoutModelAdvice {

    private final CartService cartService;

    public LayoutModelAdvice(CartService cartService) {
        this.cartService = cartService;
    }

    @ModelAttribute("cartCount")
    public int cartCount(HttpSession session) {
        return cartService.count(session);
    }

    @ModelAttribute("currentUser")
    public LoginUser currentUser(HttpSession session) {
        return (LoginUser) session.getAttribute(SessionKeys.LOGIN_USER);
    }
}

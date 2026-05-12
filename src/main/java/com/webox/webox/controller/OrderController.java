package com.webox.webox.controller;

import com.webox.webox.model.LoginUser;
import com.webox.webox.service.OrderService;
import com.webox.webox.support.BusinessException;
import com.webox.webox.support.SessionKeys;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/orders")
    public String list(HttpSession session, Model model) {
        LoginUser u = currentUser(session);
        model.addAttribute("orders", orderService.listByUser(u.id()));
        return "order/list";
    }

    @GetMapping("/orders/{id}")
    public String detail(@PathVariable Long id, HttpSession session, Model model) {
        LoginUser u = currentUser(session);
        model.addAttribute("order", orderService.getMine(u.id(), id));
        return "order/detail";
    }

    @GetMapping("/orders/success")
    public String success(@RequestParam Long id, HttpSession session, Model model) {
        LoginUser u = currentUser(session);
        model.addAttribute("order", orderService.getMine(u.id(), id));
        return "order/success";
    }

    private LoginUser currentUser(HttpSession s) {
        Object u = s.getAttribute(SessionKeys.LOGIN_USER);
        if (u == null) throw new BusinessException("请先登录");
        return (LoginUser) u;
    }
}

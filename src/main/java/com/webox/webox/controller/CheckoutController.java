package com.webox.webox.controller;

import com.webox.webox.dto.CartView;
import com.webox.webox.dto.CheckoutForm;
import com.webox.webox.entity.Order;
import com.webox.webox.model.LoginUser;
import com.webox.webox.service.CartService;
import com.webox.webox.service.OrderService;
import com.webox.webox.support.BusinessException;
import com.webox.webox.support.SessionKeys;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDate;

@Controller
public class CheckoutController {

    private final CartService cartService;
    private final OrderService orderService;

    public CheckoutController(CartService cartService, OrderService orderService) {
        this.cartService = cartService;
        this.orderService = orderService;
    }

    @GetMapping("/checkout")
    public String checkoutForm(HttpSession session, Model model) {
        CartView cart = cartService.view(session);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            return "redirect:/cart";
        }
        CheckoutForm form = new CheckoutForm();
        form.setDeliveryDate(LocalDate.now().plusDays(1));
        form.setMealPeriod("lunch");

        model.addAttribute("cart", cart);
        model.addAttribute("form", form);
        return "checkout";
    }

    @PostMapping("/checkout")
    public String checkout(@Valid @ModelAttribute("form") CheckoutForm form,
                           BindingResult br,
                           HttpSession session,
                           Model model) {
        LoginUser u = currentUser(session);
        if (br.hasErrors()) {
            model.addAttribute("cart", cartService.view(session));
            return "checkout";
        }
        Order o = orderService.placeOrder(u.id(), form, session);
        return "redirect:/orders/success?id=" + o.getId();
    }

    private LoginUser currentUser(HttpSession s) {
        Object u = s.getAttribute(SessionKeys.LOGIN_USER);
        if (u == null) throw new BusinessException("请先登录");
        return (LoginUser) u;
    }
}

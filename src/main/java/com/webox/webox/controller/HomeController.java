package com.webox.webox.controller;

import com.webox.webox.support.SessionKeys;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index(HttpSession session) {
        if (session.getAttribute(SessionKeys.LOGIN_USER) != null) {
            return "redirect:/menu";
        }
        return "redirect:/login";
    }
}

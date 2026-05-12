package com.webox.webox.controller;

import com.webox.webox.dto.LoginForm;
import com.webox.webox.dto.RegisterForm;
import com.webox.webox.model.LoginUser;
import com.webox.webox.service.UserService;
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

import java.util.Optional;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registerForm", new RegisterForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterForm form,
                           BindingResult br,
                           HttpSession session,
                           Model model) {
        if (br.hasErrors()) {
            return "register";
        }
        try {
            LoginUser u = userService.register(form);
            session.setAttribute(SessionKeys.LOGIN_USER, u);
            return "redirect:/menu";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    @GetMapping("/login")
    public String loginForm(Model model) {
        model.addAttribute("loginForm", new LoginForm());
        return "login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("loginForm") LoginForm form,
                        BindingResult br,
                        HttpSession session,
                        Model model) {
        if (br.hasErrors()) {
            return "login";
        }
        Optional<LoginUser> user = userService.login(form.getEmail(), form.getPassword());
        if (user.isPresent()) {
            session.setAttribute(SessionKeys.LOGIN_USER, user.get());
            return "redirect:/menu";
        }
        model.addAttribute("error", "邮箱或密码错误");
        return "login";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}

package com.webox.webox.controller;

import com.webox.webox.model.LoginUser;
import com.webox.webox.service.MenuService;
import com.webox.webox.support.SessionKeys;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/menu")
    public String list(@RequestParam(required = false) String category,
                       @RequestParam(defaultValue = "false") boolean onlyRecommend,
                       HttpSession session,
                       Model model) {
        LoginUser u = (LoginUser) session.getAttribute(SessionKeys.LOGIN_USER);
        Long userId = u != null ? u.id() : null;

        model.addAttribute("items", menuService.list(category, onlyRecommend, userId));
        model.addAttribute("categories", menuService.categories());
        model.addAttribute("selectedCategory", category);
        model.addAttribute("onlyRecommend", onlyRecommend);

        Object flash = session.getAttribute(SessionKeys.FLASH);
        if (flash != null) {
            session.removeAttribute(SessionKeys.FLASH);
        }
        model.addAttribute("flash", flash);

        return "menu/list";
    }

    @GetMapping("/menu/{code}")
    public String detail(@PathVariable String code, Model model) {
        model.addAttribute("item", menuService.getByCode(code));
        return "menu/detail";
    }
}

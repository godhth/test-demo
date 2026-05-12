package com.webox.webox.controller;

import com.webox.webox.dto.PreferenceForm;
import com.webox.webox.entity.UserPreference;
import com.webox.webox.model.LoginUser;
import com.webox.webox.service.PreferenceService;
import com.webox.webox.support.BusinessException;
import com.webox.webox.support.SessionKeys;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class PreferenceController {

    private final PreferenceService preferenceService;

    public PreferenceController(PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping("/preferences")
    public String form(HttpSession session, Model model) {
        LoginUser u = currentUser(session);
        Optional<UserPreference> existing = preferenceService.find(u.id());
        PreferenceForm form = new PreferenceForm();
        if (existing.isPresent()) {
            UserPreference pref = existing.get();
            List<String> allergens = new ArrayList<>();
            if (pref.getAllergens() != null && !pref.getAllergens().isBlank()) {
                allergens = Arrays.stream(pref.getAllergens().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.toList());
            }
            form.setAllergens(allergens);
            form.setRecommendOnly(pref.getRecommendOnly() != null && pref.getRecommendOnly());
        }

        model.addAttribute("form", form);
        model.addAttribute("allAllergens", preferenceService.allergenOptions());

        Object flash = session.getAttribute(SessionKeys.FLASH);
        if (flash != null) {
            session.removeAttribute(SessionKeys.FLASH);
        }
        model.addAttribute("flash", flash);

        return "preferences";
    }

    @PostMapping("/preferences")
    public String save(@ModelAttribute("form") PreferenceForm form, HttpSession session) {
        LoginUser u = currentUser(session);
        preferenceService.save(u.id(), form);
        session.setAttribute(SessionKeys.FLASH, "已保存");
        return "redirect:/preferences";
    }

    private LoginUser currentUser(HttpSession s) {
        Object u = s.getAttribute(SessionKeys.LOGIN_USER);
        if (u == null) throw new BusinessException("请先登录");
        return (LoginUser) u;
    }
}

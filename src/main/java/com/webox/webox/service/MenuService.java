package com.webox.webox.service;

import com.webox.webox.entity.MenuItem;
import com.webox.webox.entity.UserPreference;
import com.webox.webox.repository.MenuItemRepository;
import com.webox.webox.repository.UserPreferenceRepository;
import com.webox.webox.support.BusinessException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MenuService {
    private final MenuItemRepository menuItemRepository;
    private final UserPreferenceRepository userPreferenceRepository;

    public MenuService(MenuItemRepository menuItemRepository,
                       UserPreferenceRepository userPreferenceRepository) {
        this.menuItemRepository = menuItemRepository;
        this.userPreferenceRepository = userPreferenceRepository;
    }

    public List<MenuItem> list(String category, boolean onlyRecommend, Long userId) {
        List<MenuItem> items = (category == null || category.isBlank())
                ? menuItemRepository.findAll()
                : menuItemRepository.findByCategory(category);

        if (onlyRecommend && userId != null) {
            Optional<UserPreference> prefOpt = userPreferenceRepository.findByUserId(userId);
            if (prefOpt.isEmpty()) {
                return items;
            }
            UserPreference pref = prefOpt.get();
            String prefAllergens = pref.getAllergens();
            if (prefAllergens == null || prefAllergens.isBlank()) {
                return items;
            }
            Set<String> prefSet = splitToSet(prefAllergens);
            return items.stream().filter(item -> {
                Set<String> itemSet = splitToSet(item.getAllergens());
                for (String a : itemSet) {
                    if (prefSet.contains(a)) {
                        return false;
                    }
                }
                return true;
            }).toList();
        }
        return items;
    }

    private Set<String> splitToSet(String csv) {
        if (csv == null || csv.isBlank()) {
            return new HashSet<>();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(HashSet::new));
    }

    public MenuItem getByCode(String code) {
        return menuItemRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException("菜品不存在"));
    }

    public List<String> categories() {
        return menuItemRepository.findAll().stream()
                .map(MenuItem::getCategory)
                .distinct()
                .sorted()
                .toList();
    }
}

package com.webox.webox.service;

import com.webox.webox.dto.PreferenceForm;
import com.webox.webox.entity.UserPreference;
import com.webox.webox.repository.UserPreferenceRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class PreferenceService {
    private final UserPreferenceRepository userPreferenceRepository;

    public PreferenceService(UserPreferenceRepository userPreferenceRepository) {
        this.userPreferenceRepository = userPreferenceRepository;
    }

    public Optional<UserPreference> find(Long userId) {
        return userPreferenceRepository.findByUserId(userId);
    }

    public UserPreference save(Long userId, PreferenceForm form) {
        UserPreference pref = userPreferenceRepository.findByUserId(userId).orElseGet(UserPreference::new);
        pref.setUserId(userId);
        List<String> list = form.getAllergens() == null ? Collections.emptyList() : form.getAllergens();
        pref.setAllergens(String.join(",", list));
        pref.setRecommendOnly(form.isRecommendOnly());
        return userPreferenceRepository.save(pref);
    }

    public List<String> allergenOptions() {
        return List.of("peanut", "shellfish", "dairy", "egg", "gluten", "fish", "soy");
    }
}

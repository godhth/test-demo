package com.webox.webox.dto;

import java.util.ArrayList;
import java.util.List;

public class PreferenceForm {
    private List<String> allergens = new ArrayList<>();
    private boolean recommendOnly = false;

    public List<String> getAllergens() {
        return allergens;
    }

    public void setAllergens(List<String> allergens) {
        this.allergens = allergens;
    }

    public boolean isRecommendOnly() {
        return recommendOnly;
    }

    public void setRecommendOnly(boolean recommendOnly) {
        this.recommendOnly = recommendOnly;
    }
}

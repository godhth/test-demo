package com.webox.webox.service.ai;

import com.webox.webox.dto.AiRecommendResponse;
import com.webox.webox.entity.MenuItem;
import com.webox.webox.entity.UserPreference;
import com.webox.webox.repository.MenuItemRepository;
import com.webox.webox.repository.UserPreferenceRepository;
import com.webox.webox.support.AiUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class AiRecommendService {

    private static final Logger log = LoggerFactory.getLogger(AiRecommendService.class);
    private static final int MAX_ITEMS = 5;
    private static final int MIN_ITEMS = 3;

    private final QwenClient qwenClient;
    private final MenuItemRepository menuItemRepository;
    private final UserPreferenceRepository userPreferenceRepository;

    public AiRecommendService(QwenClient qwenClient,
                              MenuItemRepository menuItemRepository,
                              UserPreferenceRepository userPreferenceRepository) {
        this.qwenClient = qwenClient;
        this.menuItemRepository = menuItemRepository;
        this.userPreferenceRepository = userPreferenceRepository;
    }

    public AiRecommendResponse recommend(Long userId, String query) {
        AiRecommendResponse resp = new AiRecommendResponse();
        List<MenuItem> menu = menuItemRepository.findAll();

        List<String> userAllergens = loadAllergens(userId);
        Set<String> allergenSet = new HashSet<>(userAllergens);

        try {
            List<QwenClient.Recommendation> recs = qwenClient.recommend(query, userAllergens, menu);
            Map<String, MenuItem> byCode = new LinkedHashMap<>();
            for (MenuItem m : menu) {
                byCode.put(m.getCode(), m);
            }
            List<AiRecommendResponse.Item> items = new ArrayList<>();
            for (QwenClient.Recommendation r : recs) {
                if (r == null || r.code == null) {
                    continue;
                }
                MenuItem m = byCode.get(r.code);
                if (m == null) {
                    continue;
                }
                if (hitsAllergen(m, allergenSet)) {
                    continue;
                }
                items.add(new AiRecommendResponse.Item(m.getCode(), m.getName(), m.getPrice(), r.reason, m.getImage()));
                if (items.size() >= MAX_ITEMS) {
                    break;
                }
            }
            if (items.size() < MIN_ITEMS) {
                fallbackTopUp(items, menu, allergenSet);
            }
            resp.setItems(items);
            return resp;
        } catch (AiUnavailableException e) {
            log.warn("AI unavailable, using fallback: {}", e.getMessage());
            resp.setError("AI 暂不可用，请稍后再试");
            resp.setItems(localFallback(menu, allergenSet));
            return resp;
        } catch (Exception e) {
            log.warn("AI recommend unexpected error: {}", e.getMessage());
            resp.setError("AI 暂不可用，请稍后再试");
            resp.setItems(localFallback(menu, allergenSet));
            return resp;
        }
    }

    private List<String> loadAllergens(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        Optional<UserPreference> pref = userPreferenceRepository.findByUserId(userId);
        if (pref.isEmpty()) {
            return Collections.emptyList();
        }
        String csv = pref.get().getAllergens();
        if (csv == null || csv.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static boolean hitsAllergen(MenuItem m, Set<String> allergens) {
        if (allergens.isEmpty()) {
            return false;
        }
        String a = m.getAllergens();
        if (a == null || a.isBlank()) {
            return false;
        }
        for (String x : a.split(",")) {
            if (allergens.contains(x.trim())) {
                return true;
            }
        }
        return false;
    }

    private static void fallbackTopUp(List<AiRecommendResponse.Item> items,
                                      List<MenuItem> menu,
                                      Set<String> allergenSet) {
        Set<String> have = new HashSet<>();
        for (AiRecommendResponse.Item i : items) {
            have.add(i.getCode());
        }
        for (MenuItem m : menu) {
            if (items.size() >= MIN_ITEMS) {
                break;
            }
            if (have.contains(m.getCode())) {
                continue;
            }
            if (hitsAllergen(m, allergenSet)) {
                continue;
            }
            items.add(new AiRecommendResponse.Item(m.getCode(), m.getName(), m.getPrice(), "今日热门推荐", m.getImage()));
        }
    }

    private static List<AiRecommendResponse.Item> localFallback(List<MenuItem> menu, Set<String> allergenSet) {
        List<AiRecommendResponse.Item> out = new ArrayList<>();
        for (MenuItem m : menu) {
            if (hitsAllergen(m, allergenSet)) {
                continue;
            }
            out.add(new AiRecommendResponse.Item(m.getCode(), m.getName(), m.getPrice(), "今日热门推荐", m.getImage()));
            if (out.size() >= MIN_ITEMS) {
                break;
            }
        }
        return out;
    }
}

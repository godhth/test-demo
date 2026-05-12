package com.webox.webox.service;

import com.webox.webox.dto.CartView;
import com.webox.webox.entity.MenuItem;
import com.webox.webox.repository.MenuItemRepository;
import com.webox.webox.support.BusinessException;
import com.webox.webox.support.SessionKeys;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class CartService {
    private final MenuItemRepository menuItemRepository;

    public CartService(MenuItemRepository menuItemRepository) {
        this.menuItemRepository = menuItemRepository;
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Integer> raw(HttpSession session) {
        synchronized (session) {
            Object existing = session.getAttribute(SessionKeys.CART);
            if (existing instanceof Map<?, ?>) {
                return (Map<Long, Integer>) existing;
            }
            Map<Long, Integer> fresh = new LinkedHashMap<>();
            session.setAttribute(SessionKeys.CART, fresh);
            return fresh;
        }
    }

    public void add(HttpSession s, Long menuItemId, int qty) {
        if (qty <= 0) {
            throw new BusinessException("数量必须大于0");
        }
        if (menuItemRepository.findById(menuItemId).isEmpty()) {
            throw new BusinessException("菜品不存在");
        }
        Map<Long, Integer> cart = raw(s);
        synchronized (s) {
            cart.merge(menuItemId, qty, Integer::sum);
        }
    }

    public void update(HttpSession s, Long menuItemId, int qty) {
        Map<Long, Integer> cart = raw(s);
        synchronized (s) {
            if (qty <= 0) {
                cart.remove(menuItemId);
            } else {
                cart.put(menuItemId, qty);
            }
        }
    }

    public void remove(HttpSession s, Long menuItemId) {
        Map<Long, Integer> cart = raw(s);
        synchronized (s) {
            cart.remove(menuItemId);
        }
    }

    public void clear(HttpSession s) {
        Map<Long, Integer> cart = raw(s);
        synchronized (s) {
            cart.clear();
        }
    }

    public CartView view(HttpSession s) {
        Map<Long, Integer> cart = raw(s);
        CartView view = new CartView();
        BigDecimal total = BigDecimal.ZERO;
        int count = 0;
        synchronized (s) {
            for (Map.Entry<Long, Integer> e : cart.entrySet()) {
                Optional<MenuItem> miOpt = menuItemRepository.findById(e.getKey());
                if (miOpt.isEmpty()) {
                    continue;
                }
                MenuItem mi = miOpt.get();
                int qty = e.getValue();
                BigDecimal subtotal = mi.getPrice().multiply(BigDecimal.valueOf(qty));
                view.getItems().add(new CartView.Line(mi, qty, subtotal));
                total = total.add(subtotal);
                count += qty;
            }
        }
        view.setTotal(total);
        view.setCount(count);
        return view;
    }

    public int count(HttpSession s) {
        Map<Long, Integer> cart = raw(s);
        int count = 0;
        synchronized (s) {
            for (Integer v : cart.values()) {
                if (v != null) {
                    count += v;
                }
            }
        }
        return count;
    }
}

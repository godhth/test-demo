package com.webox.webox.service;

import com.webox.webox.dto.CartView;
import com.webox.webox.dto.CheckoutForm;
import com.webox.webox.entity.MenuItem;
import com.webox.webox.entity.Order;
import com.webox.webox.entity.OrderItem;
import com.webox.webox.repository.MenuItemRepository;
import com.webox.webox.repository.OrderRepository;
import com.webox.webox.support.BusinessException;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final CartService cartService;

    public OrderService(OrderRepository orderRepository,
                        MenuItemRepository menuItemRepository,
                        CartService cartService) {
        this.orderRepository = orderRepository;
        this.menuItemRepository = menuItemRepository;
        this.cartService = cartService;
    }

    @Transactional
    public Order placeOrder(Long userId, CheckoutForm form, HttpSession session) {
        CartView cartView = cartService.view(session);
        if (cartView.getItems().isEmpty()) {
            throw new BusinessException("购物车为空");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (CartView.Line line : cartView.getItems()) {
            MenuItem source = line.getMenuItem();
            Optional<MenuItem> verified = menuItemRepository.findById(source.getId());
            if (verified.isEmpty()) {
                throw new BusinessException("菜品不存在");
            }
            MenuItem mi = verified.get();
            int qty = line.getQuantity();
            BigDecimal price = mi.getPrice();

            OrderItem oi = new OrderItem();
            oi.setMenuItemId(mi.getId());
            oi.setName(mi.getName());
            oi.setPrice(price);
            oi.setQuantity(qty);
            orderItems.add(oi);

            total = total.add(price.multiply(BigDecimal.valueOf(qty)));
        }

        Order o = new Order();
        o.setUserId(userId);
        o.setTotalAmount(total);
        o.setDeliveryDate(form.getDeliveryDate());
        o.setMealPeriod(form.getMealPeriod());
        o.setDeliveryAddress(form.getDeliveryAddress());
        o.setStatus("CREATED");
        o.setCreatedAt(Instant.now());
        o.getItems().addAll(orderItems);

        Order saved = orderRepository.save(o);
        cartService.clear(session);
        return saved;
    }

    public List<Order> listByUser(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Order getMine(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("订单不存在"));
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }
}

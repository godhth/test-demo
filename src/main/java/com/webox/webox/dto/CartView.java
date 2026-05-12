package com.webox.webox.dto;

import com.webox.webox.entity.MenuItem;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CartView {
    public static class Line {
        private MenuItem menuItem;
        private int quantity;
        private BigDecimal subtotal;

        public Line(MenuItem m, int q, BigDecimal s) {
            this.menuItem = m;
            this.quantity = q;
            this.subtotal = s;
        }

        public MenuItem getMenuItem() {
            return menuItem;
        }

        public int getQuantity() {
            return quantity;
        }

        public BigDecimal getSubtotal() {
            return subtotal;
        }
    }

    private List<Line> items = new ArrayList<>();
    private BigDecimal total = BigDecimal.ZERO;
    private int count = 0;

    public List<Line> getItems() {
        return items;
    }

    public void setItems(List<Line> items) {
        this.items = items;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}

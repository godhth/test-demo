package com.webox.webox.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AiRecommendResponse {
    private List<Item> items = new ArrayList<>();
    private String error;

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public static class Item {
        private String code;
        private String name;
        private BigDecimal price;
        private String reason;
        private String image;

        public Item() {
        }

        public Item(String code, String name, BigDecimal price, String reason, String image) {
            this.code = code;
            this.name = name;
            this.price = price;
            this.reason = reason;
            this.image = image;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public String getImage() {
            return image;
        }

        public void setImage(String image) {
            this.image = image;
        }
    }
}

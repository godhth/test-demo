package com.webox.webox.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class CheckoutForm {
    @NotNull
    private LocalDate deliveryDate;

    @NotBlank
    private String mealPeriod;

    @NotBlank
    private String deliveryAddress;

    public LocalDate getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(LocalDate deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public String getMealPeriod() {
        return mealPeriod;
    }

    public void setMealPeriod(String mealPeriod) {
        this.mealPeriod = mealPeriod;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }
}

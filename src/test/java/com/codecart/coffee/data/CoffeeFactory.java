package com.codecart.coffee.data;

import com.codecart.coffee.models.coffees.Coffee;

/**
 * Builds coffee request bodies.
 * The id is never set, because the server creates it.
 */
public final class CoffeeFactory {

    private CoffeeFactory() {
    }

    public static Coffee validCoffee() {
        Coffee coffee = new Coffee();
        coffee.setName("Test Coffee " + System.nanoTime());
        coffee.setType("WHITE");
        coffee.setPrice(4.25);
        coffee.setStock(10);
        return coffee;
    }

    public static Coffee withName(String name) {
        Coffee coffee = validCoffee();
        coffee.setName(name);
        return coffee;
    }

    public static Coffee withType(String type) {
        Coffee coffee = validCoffee();
        coffee.setType(type);
        return coffee;
    }

    public static Coffee withPrice(Double price) {
        Coffee coffee = validCoffee();
        coffee.setPrice(price);
        return coffee;
    }

    public static Coffee withStock(Integer stock) {
        Coffee coffee = validCoffee();
        coffee.setStock(stock);
        return coffee;
    }

    public static Coffee withoutName() {
        return withName(null);
    }

    public static Coffee withoutType() {
        return withType(null);
    }

    public static Coffee withoutPrice() {
        return withPrice(null);
    }

    public static Coffee withoutStock() {
        return withStock(null);
    }
}
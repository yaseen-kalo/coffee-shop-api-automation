package com.codecart.coffee.data;

import com.codecart.coffee.models.orders.Order;
import com.codecart.coffee.models.orders.OrderItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds order request bodies.
 * Only items are set. The server creates id, status and total.
 */
public final class OrderFactory {

    private static final int DEFAULT_COFFEE_ID = 3;
    private static final int DEFAULT_QUANTITY = 2;

    private OrderFactory() {
    }

    /** One line: coffee 3, quantity 2. */
    public static Order validOrder() {
        return orderWith(line(DEFAULT_COFFEE_ID, DEFAULT_QUANTITY));
    }

    /** The example from the contract: 4.50 x 2 + 3.20 x 1 = 12.20. */
    public static Order multiLineOrder() {
        return orderWith(line(3, 2), line(6, 1));
    }

    public static Order withQuantity(Integer quantity) {
        return orderWith(line(DEFAULT_COFFEE_ID, quantity));
    }

    public static Order withCoffeeId(Integer coffeeId) {
        return orderWith(line(coffeeId, DEFAULT_QUANTITY));
    }

    /** Builds this many lines: coffee 1, 2, 3 ... with quantity 1 each. */
    public static Order withLines(int count) {
        List<OrderItem> items = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            items.add(line(i, 1));
        }
        Order order = new Order();
        order.setItems(items);
        return order;
    }

    public static Order withEmptyItems() {
        return withLines(0);
    }

    /** The items field is null, so Gson does not send it. */
    public static Order withoutItems() {
        Order order = new Order();
        order.setItems(null);
        return order;
    }

    private static Order orderWith(OrderItem... lines) {
        List<OrderItem> items = new ArrayList<>(List.of(lines));
        Order order = new Order();
        order.setItems(items);
        return order;
    }

    private static OrderItem line(Integer coffeeId, Integer quantity) {
        OrderItem item = new OrderItem();
        item.setCoffeeId(coffeeId);
        item.setQuantity(quantity);
        return item;
    }

    public static Order withCoffeeAndQuantity(Integer coffeeId, Integer quantity) {
        return orderWith(line(coffeeId, quantity));
    }
}
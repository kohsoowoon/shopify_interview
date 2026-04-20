package shopify.shopping.cart;

import java.util.HashMap;
import java.util.Map;

public class Cart {

    private Map<String, ItemCount> cart;
    private double totalPrice;

    public Cart(){
        this.cart = new HashMap<>();
    }

    public void upsertItems(ItemCount items) {
        /**
         * validate the input items (items cannot be null, items.getItem() cannot be null, items.getItem().id cannot be null, items.getCount() cannot be negative)
         * if the item by id not found from cart
         *  > add the items to cart
         *  > sum the totalPrice = items.getItem().price * items.count() + totalPrice
         * if the item by id found from cart
         *  > new ItemCount with summed quantity = prev quantity + items.getCount() and update cart to the new ItemCount
         *  > sum the totalPrice = items.getItem().price * input items.getCount() + totalPrice
         */
        validateItemCount(items);

        if (items.getCount() == 0) {
            return;
        }

        String itemId = items.getItem().id();
        ItemCount existingItems = cart.get(itemId);

        if (existingItems == null) {
            cart.put(itemId, items);
        } else {
            int updatedCount = existingItems.getCount() + items.getCount();
            cart.put(itemId, new ItemCount(items.getItem(), updatedCount));
        }

        totalPrice += items.getItem().price() * items.getCount();
    }

    public void removeItems(ItemCount items) {
        /**
         * validate the input items
         * if the item by id not found from the cart, just return
         * if otherwise
         *  > if the input items quantity is greater than or equal to the existing item quantity, remove the item from cart
         *      - update totalPrice by subtracting the price of all the removed items (existing item quantity * item price)
         *  > if the input items quantity is less than the existing item quantity, create a new ItemCount with the quantity difference of previous and new quantity and update cart to the new ItemCount
         *      - update totalPrice by subtracting the price of all the removed items (removed quantity * item price)
         */
        validateItemCount(items);

        if (items.getCount() == 0) {
            return;
        }

        String itemId = items.getItem().id();
        ItemCount existingItems = cart.get(itemId);
        if (existingItems == null) {
            return;
        }

        if (items.getCount() >= existingItems.getCount()) {
            cart.remove(itemId);
            totalPrice -= existingItems.getCount() * existingItems.getItem().price();
            return;
        }

        int remainingCount = existingItems.getCount() - items.getCount();
        cart.put(itemId, new ItemCount(existingItems.getItem(), remainingCount));
        totalPrice -= items.getCount() * existingItems.getItem().price();
    }

    public void setItemQuantity(String itemId, int quantity) {
        /**
         * validate the input itemId and quantity (itemId cannot be null or blank, quantity cannot be negative)
         * if the item not found or it's found but the quantity is the same as before, return
         * if the item is found and the prev and new quantity is different:
         *  > create a new ItemCount with the new quantity
         *  > calculate the price difference and update the totalPrice (if reduced quantity, totalPrice should decreased, if increased quantity, totpalPrice increased by the price diff)
         */
        validateItemIdAndQuantity(itemId, quantity);

        ItemCount existingItems = cart.get(itemId);
        if (existingItems == null || existingItems.getCount() == quantity) {
            return;
        }

        if (quantity == 0) {
            cart.remove(itemId);
        } else {
            cart.put(itemId, new ItemCount(existingItems.getItem(), quantity));
        }

        int quantityDifference = quantity - existingItems.getCount();
        totalPrice += existingItems.getItem().price() * quantityDifference;
    }

    public double getTotalPrice(){
        return totalPrice;
    }

    private void validateItemCount(ItemCount items) {
        if (items == null || items.getItem() == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }

        if (items.getItem().id() == null) {
            throw new IllegalArgumentException("Item id cannot be null");
        }

        if (items.getCount() < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
    }

    private void validateItemIdAndQuantity(String itemId, int quantity) {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("Item id cannot be null or blank");
        }

        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
    }
}

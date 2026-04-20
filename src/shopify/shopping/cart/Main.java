package shopify.shopping.cart;

public class Main {
    private static int totalTests = 0;
    private static int passedTests = 0;
    private static int failedTests = 0;

    public static void main(String[] args) {
        printSuiteHeader("Cart plain Java test runner");

        testUpsertItems();
        testRemoveItems();
        setItemQuantity();

        printSuiteSummary();
    }

    private static void testUpsertItems() {
        printSection("upsertItems");
        /**
         * Test case
         * - if the item is not found, add the items to the cart(items.id, items) and sum the current totalPrice + new additional price from the items
         * - if the item is found by items.getId(), create a new ItemCount with the quantity sum of previous and new quantity + update the totalPrice to include the new quantity price
         * - if the input items quantity is 0, just return
         * - if the input items quantity is negative, throw IllegalArgumentException with message "Quantity cannot be negative"
         * - if the input items have null item, throw IllegalArgumentException with message "Item cannot be null"
         * - if the input items have null item id, throw IllegalArgumentException with message "Item id cannot be null"
         */

        runTest(
                "Behavior: add new item when item id is not found",
                "Covers: new item should be added and total price should increase by item price times quantity.",
                () -> {
                    Cart cart = new Cart();
                    Item item = new Item("sku-1", "Keyboard", 100.00, 2);

                    cart.upsertItems(new ItemCount(item, item.quantity()));

                    assertEquals("new item should increase total price", 200.00, cart.getTotalPrice());
                }
        );

        runTest(
                "Behavior: update existing item when item id already exists",
                "Covers: existing item quantity should be increased and total price should include only the newly added quantity price.",
                () -> {
                    Cart cart = new Cart();
                    Item item = new Item("sku-2", "Mouse", 50.00, 1);

                    cart.upsertItems(new ItemCount(item, 1));
                    assertEquals("first upsert should set total price to the first quantity price", 50.00, cart.getTotalPrice());

                    cart.upsertItems(new ItemCount(item, 2));

                    assertEquals("existing item should accumulate total price by added quantity", 150.00, cart.getTotalPrice());
                }
        );

        runTest(
                "Behavior: zero quantity input should do nothing",
                "Covers: quantity 0 should return without changing the cart.",
                () -> {
                    Cart cart = new Cart();
                    Item item = new Item("sku-3", "Headset", 25.00, 0);

                    cart.upsertItems(new ItemCount(item, 0));

                    assertEquals("zero quantity should not change total price", 0.00, cart.getTotalPrice());
                }
        );

        runTest(
                "Validation: negative quantity should be rejected",
                "Covers: negative quantity should throw IllegalArgumentException with the expected message.",
                () -> {
                    Cart cart = new Cart();
                    Item item = new Item("sku-4", "Monitor", 300.00, -1);

                    IllegalArgumentException exception = assertThrows(
                            "negative quantity should be rejected",
                            IllegalArgumentException.class,
                            () -> cart.upsertItems(new ItemCount(item, -1))
                    );
                    assertEquals("negative quantity message should match", "Quantity cannot be negative", exception.getMessage());
                }
        );

        runTest(
                "Validation: null item should be rejected",
                "Covers: null item should throw IllegalArgumentException with the expected message.",
                () -> {
                    Cart cart = new Cart();

                    IllegalArgumentException exception = assertThrows(
                            "null item should be rejected",
                            IllegalArgumentException.class,
                            () -> cart.upsertItems(new ItemCount(null, 1))
                    );
                    assertEquals("null item message should match", "Item cannot be null", exception.getMessage());
                }
        );

        runTest(
                "Validation: null item id should be rejected",
                "Covers: null item id should throw IllegalArgumentException with the expected message.",
                () -> {
                    Cart cart = new Cart();
                    Item item = new Item(null, "Laptop Stand", 80.00, 1);

                    IllegalArgumentException exception = assertThrows(
                            "null item id should be rejected",
                            IllegalArgumentException.class,
                            () -> cart.upsertItems(new ItemCount(item, 1))
                    );
                    assertEquals("null item id message should match", "Item id cannot be null", exception.getMessage());
                }
        );
    }

    private static void testRemoveItems() {
        printSection("removeItems");
        /**
         * Test case
         * - if the item is not found, do nothing and return
         * - if the item is found by items.getId() and the input items quantity is greater than or equal to the existing item quantity, remove the item from the cart and decrease the total price by the existing item quantity times item price
         * - if the item is found by items.getId() and the input items quantity is less than the existing item quantity, create a new ItemCount with the quantity difference of previous and new quantity + update the totalPrice to decrease by the removed quantity price
         * - if the input items quantity is negative, throw IllegalArgumentException with message "Quantity cannot be negative"
         * - if the input items have null item, throw IllegalArgumentException with message "Item cannot be null"
         * - if the input items have null item id, throw IllegalArgumentException with message "Item id cannot be null"
         * - if the input items have quantity 0, just return
         */

        runTest(
                "Behavior: removing a missing item should do nothing",
                "Covers: if item id is not found in the cart, removeItems should return without changing total price.",
                () -> {
                    Cart cart = new Cart();
                    Item existing = new Item("sku-rm-1", "Keyboard", 100.00, 2);
                    Item missing = new Item("sku-rm-2", "Mouse", 50.00, 1);

                    cart.upsertItems(new ItemCount(existing, 2));
                    assertEquals("setup upsert should establish the original total price", 200.00, cart.getTotalPrice());

                    cart.removeItems(new ItemCount(missing, 1));

                    assertEquals("missing item removal should not change total price", 200.00, cart.getTotalPrice());
                }
        );

        runTest(
                "Behavior: removing quantity greater than existing should remove the whole item",
                "Covers: if input quantity is greater than existing quantity, remove the item and subtract existing quantity price.",
                () -> {
                    Cart cart = new Cart();
                    Item item = new Item("sku-rm-3", "Headset", 25.00, 2);

                    cart.upsertItems(new ItemCount(item, 2));
                    assertEquals("setup upsert should establish the original total price", 50.00, cart.getTotalPrice());

                    cart.removeItems(new ItemCount(item, 5));

                    assertEquals("removing more than existing quantity should clear total price for that item", 0.00, cart.getTotalPrice());
                }
        );

        runTest(
                "Behavior: removing quantity equal to existing should remove the whole item",
                "Covers: if input quantity equals existing quantity, remove the item and subtract existing quantity price.",
                () -> {
                    Cart cart = new Cart();
                    Item item = new Item("sku-rm-4", "Stand", 40.00, 3);

                    cart.upsertItems(new ItemCount(item, 3));
                    assertEquals("setup upsert should establish the original total price", 120.00, cart.getTotalPrice());

                    cart.removeItems(new ItemCount(item, 3));

                    assertEquals("removing exact quantity should clear total price for that item", 0.00, cart.getTotalPrice());
                }
        );

        runTest(
                "Behavior: removing quantity less than existing should reduce total price proportionally",
                "Covers: if input quantity is less than existing quantity, update remaining quantity and subtract only removed quantity price.",
                () -> {
                    Cart cart = new Cart();
                    Item item = new Item("sku-rm-5", "Chair", 30.00, 4);

                    cart.upsertItems(new ItemCount(item, 4));
                    assertEquals("setup upsert should establish the original total price", 120.00, cart.getTotalPrice());

                    cart.removeItems(new ItemCount(item, 1));

                    assertEquals("partial removal should subtract only removed quantity price", 90.00, cart.getTotalPrice());
                }
        );

        runTest(
                "Behavior: zero quantity removal should do nothing",
                "Covers: quantity 0 should return without changing the cart.",
                () -> {
                    Cart cart = new Cart();
                    Item item = new Item("sku-rm-6", "Lamp", 15.00, 2);

                    cart.upsertItems(new ItemCount(item, 2));
                    assertEquals("setup upsert should establish the original total price", 30.00, cart.getTotalPrice());

                    cart.removeItems(new ItemCount(item, 0));

                    assertEquals("zero quantity removal should not change total price", 30.00, cart.getTotalPrice());
                }
        );

        runTest(
                "Validation: negative removal quantity should be rejected",
                "Covers: negative quantity should throw IllegalArgumentException with the expected message.",
                () -> {
                    Cart cart = new Cart();
                    Item item = new Item("sku-rm-7", "Monitor", 300.00, -1);

                    IllegalArgumentException exception = assertThrows(
                            "negative removal quantity should be rejected",
                            IllegalArgumentException.class,
                            () -> cart.removeItems(new ItemCount(item, -1))
                    );
                    assertEquals("negative quantity removal message should match", "Quantity cannot be negative", exception.getMessage());
                }
        );

        runTest(
                "Validation: null removal item should be rejected",
                "Covers: null item should throw IllegalArgumentException with the expected message.",
                () -> {
                    Cart cart = new Cart();

                    IllegalArgumentException exception = assertThrows(
                            "null removal item should be rejected",
                            IllegalArgumentException.class,
                            () -> cart.removeItems(new ItemCount(null, 1))
                    );
                    assertEquals("null removal item message should match", "Item cannot be null", exception.getMessage());
                }
        );

        runTest(
                "Validation: null removal item id should be rejected",
                "Covers: null item id should throw IllegalArgumentException with the expected message.",
                () -> {
                    Cart cart = new Cart();
                    Item item = new Item(null, "Laptop Stand", 80.00, 1);

                    IllegalArgumentException exception = assertThrows(
                            "null removal item id should be rejected",
                            IllegalArgumentException.class,
                            () -> cart.removeItems(new ItemCount(item, 1))
                    );
                    assertEquals("null removal item id message should match", "Item id cannot be null", exception.getMessage());
                }
        );
    }

    private static void setItemQuantity() {
        printSection("setItemQuantity");
        /**
         * Test case
         * - if the itemId null or blank, throw IllegalArgumentException with message "Item id cannot be null or blank"
         * - if the quantity is negative, throw IllegalArgumentException with message "Quantity cannot be
         * - if itemId not found from cart, no change
         * - if itemId found from cart, update the item quantity to input quantity and the totalPrice should update accordingly
         * - if itemId found from cart, and the input quantity is 0, the item should be removed and totalPrice should be updated accordingly
         * - if itemId found from cart, and the input quantity is the same as existing quantity, the price should also remain same as well as the quantity.
         */

        runTest(
                "Validation: null item id should be rejected",
                "Covers: null itemId should throw IllegalArgumentException with the expected message.",
                () -> {
                    Cart cart = new Cart();

                    IllegalArgumentException exception = assertThrows(
                            "null item id should be rejected",
                            IllegalArgumentException.class,
                            () -> cart.setItemQuantity(null, 1)
                    );
                    assertEquals("null item id message should match", "Item id cannot be null or blank", exception.getMessage());
                }
        );

        runTest(
                "Validation: blank item id should be rejected",
                "Covers: blank itemId should throw IllegalArgumentException with the expected message.",
                () -> {
                    Cart cart = new Cart();

                    IllegalArgumentException exception = assertThrows(
                            "blank item id should be rejected",
                            IllegalArgumentException.class,
                            () -> cart.setItemQuantity("   ", 1)
                    );
                    assertEquals("blank item id message should match", "Item id cannot be null or blank", exception.getMessage());
                }
        );

        runTest(
                "Validation: negative quantity should be rejected",
                "Covers: negative quantity should throw IllegalArgumentException with the expected message.",
                () -> {
                    Cart cart = new Cart();

                    IllegalArgumentException exception = assertThrows(
                            "negative set quantity should be rejected",
                            IllegalArgumentException.class,
                            () -> cart.setItemQuantity("sku-set-1", -1)
                    );
                    assertEquals("negative set quantity message should match", "Quantity cannot be negative", exception.getMessage());
                }
        );

        runTest(
                "Behavior: missing item id should do nothing",
                "Covers: if item id is not found from cart, setItemQuantity should not change total price.",
                () -> {
                    Cart cart = new Cart();
                    Item existing = new Item("sku-set-2", "Keyboard", 100.00, 2);

                    cart.upsertItems(new ItemCount(existing, 2));
                    assertEquals("setup upsert should establish the original total price", 200.00, cart.getTotalPrice());

                    cart.setItemQuantity("missing-id", 5);

                    assertEquals("missing item id should not change total price", 200.00, cart.getTotalPrice());
                }
        );

        runTest(
                "Behavior: existing item quantity should update total price when set to a new quantity",
                "Covers: if item id is found, quantity should update and totalPrice should be recalculated accordingly.",
                () -> {
                    Cart cart = new Cart();
                    Item item = new Item("sku-set-3", "Mouse", 50.00, 2);

                    cart.upsertItems(new ItemCount(item, 2));
                    assertEquals("setup upsert should establish the original total price", 100.00, cart.getTotalPrice());

                    cart.setItemQuantity("sku-set-3", 5);

                    assertEquals("setItemQuantity should update total price to the new quantity", 250.00, cart.getTotalPrice());
                }
        );

        runTest(
                "Behavior: setting quantity to zero should remove the item and clear its total price contribution",
                "Covers: if item id is found and quantity is 0, the item should be removed and totalPrice should update accordingly.",
                () -> {
                    Cart cart = new Cart();
                    Item item = new Item("sku-set-4", "Lamp", 15.00, 2);

                    cart.upsertItems(new ItemCount(item, 2));
                    assertEquals("setup upsert should establish the original total price", 30.00, cart.getTotalPrice());

                    cart.setItemQuantity("sku-set-4", 0);

                    assertEquals("setting quantity to zero should remove the total price contribution", 0.00, cart.getTotalPrice());
                }
        );

        runTest(
                "Behavior: setting quantity to the same existing quantity should keep total price unchanged",
                "Covers: if item id is found and quantity is unchanged, price and quantity effect should remain the same.",
                () -> {
                    Cart cart = new Cart();
                    Item item = new Item("sku-set-5", "Stand", 40.00, 3);

                    cart.upsertItems(new ItemCount(item, 3));
                    assertEquals("setup upsert should establish the original total price", 120.00, cart.getTotalPrice());

                    cart.setItemQuantity("sku-set-5", 3);

                    assertEquals("setting the same quantity should keep total price unchanged", 120.00, cart.getTotalPrice());
                }
        );
    }

    private static void printSuiteHeader(String title) {
        System.out.println("==============================================");
        System.out.println(title);
        System.out.println("==============================================");
    }

    private static void printSection(String sectionName) {
        System.out.println();
        System.out.println("----------------------------------------------");
        System.out.println("Running tests for " + sectionName);
        System.out.println("----------------------------------------------");
    }

    private static void printSuiteSummary() {
        System.out.println();
        System.out.println("==============================================");
        System.out.println("Test summary");
        System.out.println("==============================================");
        System.out.println("Total : " + totalTests);
        System.out.println("Passed: " + passedTests);
        System.out.println("Failed: " + failedTests);
        System.out.println();
        System.out.println("All tests were executed. Failures are reported above.");
    }

    private static void runTest(String testName, String description, ThrowingRunnable testBody) {
        totalTests++;

        System.out.println();
        System.out.println("TEST " + totalTests + ": " + testName);
        System.out.println("  " + description);

        try {
            testBody.run();
            passedTests++;
            System.out.println("  RESULT: PASS");
        } catch (AssertionError assertionError) {
            failedTests++;
            System.out.println("  RESULT: FAIL");
            System.out.println("  REASON: " + assertionError.getMessage());
        } catch (Throwable throwable) {
            failedTests++;
            System.out.println("  RESULT: FAIL");
            System.out.println("  REASON: Unexpected " + throwable.getClass().getSimpleName() + " - " + throwable.getMessage());
        }
    }

    private static void assertEquals(String failureMessage, Object expected, Object actual) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(failureMessage + ". Expected <" + expected + "> but was <" + actual + ">.");
        }
    }

    private static IllegalArgumentException assertThrows(String failureMessage, Class<IllegalArgumentException> expectedType, ThrowingRunnable action) {
        try {
            action.run();
        } catch (Throwable throwable) {
            if (expectedType.isInstance(throwable)) {
                return expectedType.cast(throwable);
            }

            throw new AssertionError(
                    failureMessage + ". Expected " + expectedType.getSimpleName()
                            + " but got " + throwable.getClass().getSimpleName() + "."
            );
        }

        throw new AssertionError(failureMessage + ". Expected " + expectedType.getSimpleName() + " to be thrown.");
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}

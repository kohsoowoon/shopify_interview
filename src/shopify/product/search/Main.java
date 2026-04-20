package shopify.product.search;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Main {
    private static int totalTests = 0;
    private static int passedTests = 0;
    private static int failedTests = 0;

    public static void main(String[] args) {
        printSuiteHeader("SearchProducts plain Java test runner");

        //testAddProduct();
        testKeywordMatcher();
        //testSearchProducts();

        printSuiteSummary();
    }

    private static void testKeywordMatcher(){
        /**
         * Test cases for keywordMatcher method: product, keyword => boolean
         * 1. Keyword matching:
         *  - product.name rules:
         *      > null or blank names hould not match any keyword => return false
         *      > substring match: if keyword is part of the product name => return true
         *      > case sensitivity: regardless of the case, if keyword found in name => return true
         *      > multiple occurence: if found at first occurence, stop the iteration => return true
         *      > "   abc    def  " should be normalized to "abc def" for search
         *  - product.tags rules:
         *      > null or empty tags should not match any keyword => return false
         *      > if any existing tags contains the substring normalized keyword => return true
         *  - combination of name and tags:
         *      > if any of the above rule true => return true
         * 2. keyword rules:
         * - keyword: "  abc   def " -> normalized to "abc def" for search
         * - emtpy or blank keyword => return false
         *
         */

        printSection("keywordMatcher");

        SearchProducts searchProducts = new SearchProducts();

        // Case group 1: product name matching rules.
        runTest(
                "Validation: null product input should be rejected",
                "Covers: product cannot be null.",
                () -> assertThrows(
                        "null product should be rejected",
                        IllegalArgumentException.class,
                        () -> searchProducts.keywordMatcher(null, "keyboard")
                )
        );

        runTest(
                "Validation: null product name should not match any keyword",
                "Covers: product.name null should return false.",
                () -> assertFalse(
                        "null product name should not match",
                        searchProducts.keywordMatcher(product("sku-k1", null, 10.00, List.of("tech")), "keyboard")
                )
        );

        runTest(
                "Validation: empty product name should not match any keyword",
                "Covers: product.name empty should return false and there is no need to attempt contains on an empty normalized name.",
                () -> assertFalse(
                        "empty product name should not match",
                        searchProducts.keywordMatcher(product("sku-k1a", "", 10.00, List.of("tech")), "keyboard")
                )
        );

        runTest(
                "Validation: blank product name should not match any keyword",
                "Covers: product.name blank should return false.",
                () -> assertFalse(
                        "blank product name should not match",
                        searchProducts.keywordMatcher(product("sku-k2", "   ", 10.00, List.of("tech")), "keyboard")
                )
        );

        runTest(
                "Behavior: substring match inside product name should return true",
                "Covers: if keyword is part of product name, return true.",
                () -> assertTrue(
                        "keyword substring in name should match",
                        searchProducts.keywordMatcher(product("sku-k3", "Mechanical Keyboard", 10.00, List.of("tech")), "board")
                )
        );

        runTest(
                "Behavior: keyword matching in product name should ignore case",
                "Covers: regardless of case, if keyword is found in name, return true.",
                () -> assertTrue(
                        "case-insensitive name match should return true",
                        searchProducts.keywordMatcher(product("sku-k4", "Mechanical Keyboard", 10.00, List.of("tech")), "KEYBOARD")
                )
        );

        runTest(
                "Behavior: normalized spaces in product name should still match",
                "Covers: \"   abc    def  \" should be normalized to \"abc def\" for search.",
                () -> assertTrue(
                        "normalized product name should match normalized keyword",
                        searchProducts.keywordMatcher(product("sku-k5", "   abc    def  ", 10.00, List.of("tech")), "abc def")
                )
        );

        runTest(
                "Behavior: first matching occurrence in product name is enough",
                "Covers: if found at first occurrence, stop iteration and return true.",
                () -> assertTrue(
                        "multiple occurrences in name should still return true once matched",
                        searchProducts.keywordMatcher(product("sku-k6", "abc def abc xyz", 10.00, List.of("tech")), "abc")
                )
        );

        // Case group 2: product tags matching rules.
        runTest(
                "Validation: null product tags should not match any keyword",
                "Covers: null tags should return false when name does not match.",
                () -> assertFalse(
                        "null tags should not match",
                        searchProducts.keywordMatcher(product("sku-k7", "Desk Lamp", 10.00, null), "wireless")
                )
        );

        runTest(
                "Validation: empty product tags should not match any keyword",
                "Covers: empty tags should return false when name does not match.",
                () -> assertFalse(
                        "empty tags should not match",
                        searchProducts.keywordMatcher(product("sku-k8", "Desk Lamp", 10.00, List.of()), "wireless")
                )
        );

        runTest(
                "Validation: empty tag value should not match any keyword",
                "Covers: empty tag text should return false and there is no need to attempt contains on an empty normalized tag.",
                () -> assertFalse(
                        "empty tag should not match",
                        searchProducts.keywordMatcher(product("sku-k8a", "Desk Lamp", 10.00, List.of("")), "wireless")
                )
        );

        runTest(
                "Validation: blank tag value should not match any keyword",
                "Covers: blank tag text should return false and there is no need to attempt contains on a blank normalized tag.",
                () -> assertFalse(
                        "blank tag should not match",
                        searchProducts.keywordMatcher(product("sku-k8b", "Desk Lamp", 10.00, List.of("   ")), "wireless")
                )
        );

        runTest(
                "Behavior: substring match inside any tag should return true",
                "Covers: if any existing tag contains the substring normalized keyword, return true.",
                () -> assertTrue(
                        "substring inside tag should match",
                        searchProducts.keywordMatcher(product("sku-k9", "Desk Lamp", 10.00, List.of("home office", "lighting")), "office")
                )
        );

        runTest(
                "Behavior: normalized keyword should match normalized tag text",
                "Covers: normalized keyword should be used for tag matching too.",
                () -> assertTrue(
                        "normalized keyword should match normalized tag",
                        searchProducts.keywordMatcher(product("sku-k10", "Desk Lamp", 10.00, Arrays.asList("  abc   def  ", "lighting")), " abc def ")
                )
        );

        // Case group 3: combination of name and tags.
        runTest(
                "Behavior: matching product name alone should return true even if tags do not match",
                "Covers: if any name rule is true, return true.",
                () -> assertTrue(
                        "name match should be enough even when tags miss",
                        searchProducts.keywordMatcher(product("sku-k11", "Gaming Keyboard", 10.00, List.of("office")), "keyboard")
                )
        );

        runTest(
                "Behavior: matching tag alone should return true even if name does not match",
                "Covers: if any tag rule is true, return true.",
                () -> assertTrue(
                        "tag match should be enough even when name misses",
                        searchProducts.keywordMatcher(product("sku-k12", "Desk Lamp", 10.00, List.of("gaming gear")), "gear")
                )
        );

        // Case group 4: keyword normalization and blank handling.
        runTest(
                "Behavior: keyword should be normalized before search",
                "Covers: keyword \"  abc   def \" should be normalized to \"abc def\".",
                () -> assertTrue(
                        "normalized keyword should match normalized name",
                        searchProducts.keywordMatcher(product("sku-k13", "abc def", 10.00, List.of("tech")), "  abc   def ")
                )
        );

        runTest(
                "Validation: empty keyword should return false",
                "Covers: empty keyword should return false.",
                () -> assertFalse(
                        "empty keyword should not match",
                        searchProducts.keywordMatcher(product("sku-k14", "Mechanical Keyboard", 10.00, List.of("tech")), "")
                )
        );

        runTest(
                "Validation: blank keyword should return false",
                "Covers: blank keyword should return false.",
                () -> assertFalse(
                        "blank keyword should not match",
                        searchProducts.keywordMatcher(product("sku-k15", "Mechanical Keyboard", 10.00, List.of("tech")), "   ")
                )
        );
    }

    private static void testSearchProducts() {
        /**
         * Test cases for searchProducts method:
         * 1.
         */

        printSection("searchProducts");
        System.out.println("No executable searchProducts tests added yet.");
    }

    private static void testAddProduct() {
        /**
         * Test cases for addProduct method:
         * 1. New product validation:
         *  - id cannot be null or blank, also has to be unique across the given list of products.
         *  - name cannot be null or blank. Will normalize it for search later.
         *  - price bigger or equal to 0, 2 digit decimal places.
         *  - tags: nullable, emptiable, blankable list, each tag could be also blankable : nothing to validate here
         *
         *  2. Add a valid product to a list
         *  - if no matching id, add the new product
         *  - if the id already exists, update the existing product with the new product data
         */

        printSection("addProduct");

        SearchProducts searchProducts = new SearchProducts();

        // Case group 1: product id validation.
        runTest(
                "Validation: reject product when id is null",
                "Covers: id cannot be null.",
                () -> assertThrows(
                        "null id should be rejected",
                        IllegalArgumentException.class,
                        () -> searchProducts.addProduct(new LinkedHashSet<>(), product(null, "Keyboard", 10.00, List.of("tech")))
                )
        );

        runTest(
                "Validation: reject product when id is blank",
                "Covers: id cannot be blank.",
                () -> assertThrows(
                        "blank id should be rejected",
                        IllegalArgumentException.class,
                        () -> searchProducts.addProduct(new LinkedHashSet<>(), product("   ", "Keyboard", 10.00, List.of("tech")))
                )
        );

        runTest(
                "Validation: update existing product when id already exists",
                "Covers: existing matching id should not create a duplicate; it should update the existing product data.",
                () -> {
                    Set<Product> products = new LinkedHashSet<>();
                    Product original = product("sku-1", "Original Keyboard", 99.99, List.of("tech"));
                    Product replacement = product("sku-1", "Updated Keyboard", 79.99, List.of("sale"));

                    products.add(original);
                    searchProducts.addProduct(products, replacement);

                    assertEquals("set size should stay at one", 1, products.size());
                    assertProductEquals("existing product should be updated", replacement, findById(products, "sku-1"));
                }
        );

        // Case group 2: product name validation.
        runTest(
                "Validation: reject product when name is null",
                "Covers: name cannot be null.",
                () -> assertThrows(
                        "null name should be rejected",
                        IllegalArgumentException.class,
                        () -> searchProducts.addProduct(new LinkedHashSet<>(), product("sku-1", null, 10.00, List.of("tech")))
                )
        );

        runTest(
                "Validation: reject product when name is blank",
                "Covers: name cannot be blank.",
                () -> assertThrows(
                        "blank name should be rejected",
                        IllegalArgumentException.class,
                        () -> searchProducts.addProduct(new LinkedHashSet<>(), product("sku-1", "   ", 10.00, List.of("tech")))
                )
        );

        // Case group 3: price validation.
        runTest(
                "Validation: reject product when price is negative",
                "Covers: price must be bigger or equal to 0.",
                () -> assertThrows(
                        "negative price should be rejected",
                        IllegalArgumentException.class,
                        () -> searchProducts.addProduct(new LinkedHashSet<>(), product("sku-1", "Keyboard", -0.01, List.of("tech")))
                )
        );

        runTest(
                "Validation: reject product when price has more than two decimal places",
                "Covers: price should allow only two decimal places.",
                () -> assertThrows(
                        "price with more than two decimals should be rejected",
                        IllegalArgumentException.class,
                        () -> searchProducts.addProduct(new LinkedHashSet<>(), product("sku-1", "Keyboard", 10.999, List.of("tech")))
                )
        );

        // Case group 4: tags acceptance.
        runTest(
                "Validation: allow nullable, empty, and blank tags",
                "Covers: tags can be null, the list can be empty, and individual tag entries can be blank or null.",
                () -> {
                    Set<Product> products = new LinkedHashSet<>();
                    Product productWithLooseTags = product("sku-2", "Mouse", 25.00, Arrays.asList(null, "", "  "));

                    searchProducts.addProduct(products, productWithLooseTags);

                    assertEquals("valid product with loose tags should be added", 1, products.size());
                    assertProductEquals("tags should be stored as provided", productWithLooseTags, findById(products, "sku-2"));
                }
        );

        runTest(
                "Behavior: add valid product when id does not exist",
                "Covers: if no matching id exists, add the new product.",
                () -> {
                    Set<Product> products = new LinkedHashSet<>();
                    Product newProduct = product("sku-3", "Desk Lamp", 40.00, null);

                    searchProducts.addProduct(products, newProduct);

                    assertEquals("new valid product should be added", 1, products.size());
                    assertProductEquals("added product should match input data", newProduct, findById(products, "sku-3"));
                }
        );
    }

    private static Product product(String id, String name, double price, List<String> tags) {
        return new Product(id, name, price, tags);
    }

    private static Product findById(Set<Product> products, String id) {
        return products.stream()
                .filter(product -> Objects.equals(product.id(), id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Could not find product with id '" + id + "'."));
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
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(failureMessage + ". Expected <" + expected + "> but was <" + actual + ">.");
        }
    }

    private static void assertTrue(String failureMessage, boolean actual) {
        if (!actual) {
            throw new AssertionError(failureMessage + ". Expected <true> but was <false>.");
        }
    }

    private static void assertFalse(String failureMessage, boolean actual) {
        if (actual) {
            throw new AssertionError(failureMessage + ". Expected <false> but was <true>.");
        }
    }

    private static void assertProductEquals(String failureMessage, Product expected, Product actual) {
        if (!Objects.equals(expected.id(), actual.id())
                || !Objects.equals(expected.name(), actual.name())
                || Double.compare(expected.price(), actual.price()) != 0
                || !Objects.equals(expected.tags(), actual.tags())) {
            throw new AssertionError(failureMessage + ". Expected <" + expected + "> but was <" + actual + ">.");
        }
    }

    private static void assertThrows(String failureMessage, Class<? extends Throwable> expectedType, ThrowingRunnable action) {
        try {
            action.run();
        } catch (Throwable throwable) {
            if (expectedType.isInstance(throwable)) {
                return;
            }

            throw new AssertionError(
                    failureMessage + ". Expected " + expectedType.getSimpleName()
                            + " but got " + throwable.getClass().getSimpleName() + "."
            );
        }

        throw new AssertionError(failureMessage + ". Expected " + expectedType.getSimpleName() + " to be thrown.");
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

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}

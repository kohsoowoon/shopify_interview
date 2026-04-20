package shopify.product.search;

import static shopify.product.search.TestHelper.assertEquals;
import static shopify.product.search.TestHelper.assertFalse;
import static shopify.product.search.TestHelper.assertProductEquals;
import static shopify.product.search.TestHelper.assertThrows;
import static shopify.product.search.TestHelper.assertTrue;
import static shopify.product.search.TestHelper.findById;
import static shopify.product.search.TestHelper.printSection;
import static shopify.product.search.TestHelper.printSuiteHeader;
import static shopify.product.search.TestHelper.printSuiteSummary;
import static shopify.product.search.TestHelper.normalizedProduct;
import static shopify.product.search.TestHelper.product;
import static shopify.product.search.TestHelper.runTest;
import static shopify.product.search.TestHelper.searchCriteria;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Main {
    public static void main(String[] args) {
        printSuiteHeader("SearchProducts plain Java test runner");

        testAddProduct();
        testKeywordMatcher();
        testRelevanceCalculator();
        testSearchProducts();

        printSuiteSummary();
    }

    private static void testRelevanceCalculator(){
        /**
         * Test cases for relevanceCalculator method: product, keyword => int
         * 1. Relevance score calculation rules:
         * - matching definition: same as keywordMatcher normalized + case insenstive + substring match
         *   > only non-overlapping occurence should be counted. ex) keyword=aba, name = abababa => count as 2 not 3.
         * - if the keyword is found in the product name > score: 10
         * - if the keyword is found more than once in the name, we want to weigh the score > score: 10 * occurence count
         * - if more than 5 times found, we want to cap the score from name <= 50 (max score from name match = 50)
         * - if the keyword is found in any of the tags > score: 5
         * - if multiple tags match, we want to sum up the score: 5 * occurence count in tags
         * - if more than 6 times found in tags, we want to cap the score from tags by 30
         */

        printSection("relevanceCalculator");

        SearchProducts searchProducts = new SearchProducts();

        // Case group 1: product name scoring rules.
        runTest(
                "Behavior: single name match should score 10",
                "Covers: one matching occurrence in product name should score 10.",
                () -> assertEquals(
                        "single name match should score 10",
                        10,
                        searchProducts.relevanceCalculator(normalizedProduct(product("sku-r1", "Mechanical Keyboard", 10.00, List.of("tech"))), searchCriteria("keyboard"))
                )
        );

        runTest(
                "Behavior: repeated name matches should scale by occurrence count",
                "Covers: name score should be 10 times the non-overlapping occurrence count.",
                () -> assertEquals(
                        "three name matches should score 30",
                        30,
                        searchProducts.relevanceCalculator(normalizedProduct(product("sku-r2", "abc abc abc", 10.00, List.of("tech"))), searchCriteria("abc"))
                )
        );

        runTest(
                "Behavior: overlapping name matches should not be double counted",
                "Covers: only non-overlapping occurrences in the name should be counted.",
                () -> assertEquals(
                        "abababa with keyword aba should count as 2 name matches",
                        20,
                        searchProducts.relevanceCalculator(normalizedProduct(product("sku-r3", "abababa", 10.00, List.of("tech"))), searchCriteria("aba"))
                )
        );

        runTest(
                "Behavior: name score should be capped at 50",
                "Covers: more than 5 name matches should cap the name score at 50.",
                () -> assertEquals(
                        "name score should cap at 50",
                        50,
                        searchProducts.relevanceCalculator(normalizedProduct(product("sku-r4", "abc abc abc abc abc abc", 10.00, List.of("tech"))), searchCriteria("abc"))
                )
        );

        // Case group 2: tag scoring rules.
        runTest(
                "Behavior: single tag match should score 5",
                "Covers: one matching occurrence in tags should score 5.",
                () -> assertEquals(
                        "single tag match should score 5",
                        5,
                        searchProducts.relevanceCalculator(normalizedProduct(product("sku-r5", "Desk Lamp", 10.00, List.of("home office"))), searchCriteria("office"))
                )
        );

        runTest(
                "Behavior: tag score should sum non-overlapping matches across tags",
                "Covers: tag score should be 5 times the total non-overlapping occurrence count across all tags.",
                () -> assertEquals(
                        "three tag matches across tags should score 15",
                        15,
                        searchProducts.relevanceCalculator(normalizedProduct(product("sku-r6", "Desk Lamp", 10.00, List.of("abc abc", "abc", "lighting"))), searchCriteria("abc"))
                )
        );

        runTest(
                "Behavior: overlapping tag matches should not be double counted",
                "Covers: only non-overlapping occurrences should be counted in tags too.",
                () -> assertEquals(
                        "abababa tag with keyword aba should count as 2 tag matches",
                        10,
                        searchProducts.relevanceCalculator(normalizedProduct(product("sku-r7", "Desk Lamp", 10.00, List.of("abababa"))), searchCriteria("aba"))
                )
        );

        runTest(
                "Behavior: tag score should be capped at 30 after more than 6 matches",
                "Covers: if more than 6 total tag matches are found, the tag score should be capped at 30.",
                () -> assertEquals(
                        "tag score should cap at 30",
                        30,
                        searchProducts.relevanceCalculator(normalizedProduct(product("sku-r8", "Desk Lamp", 10.00, List.of("abc abc abc", "abc abc", "abc abc"))), searchCriteria("abc"))
                )
        );

        // Case group 3: combined name and tag scoring.
        runTest(
                "Behavior: name and tag scores should be added together",
                "Covers: total relevance should include both name score and tag score.",
                () -> assertEquals(
                        "combined name and tag score should add up",
                        20,
                        searchProducts.relevanceCalculator(normalizedProduct(product("sku-r9", "abc lamp", 10.00, List.of("abc abc"))), searchCriteria("abc"))
                )
        );

        runTest(
                "Behavior: combined capped name and capped tag scores should add together",
                "Covers: total relevance should respect both caps and sum them together.",
                () -> assertEquals(
                        "combined capped score should be 80",
                        80,
                        searchProducts.relevanceCalculator(
                                normalizedProduct(product("sku-r10", "abc abc abc abc abc abc", 10.00, List.of("abc abc abc", "abc abc", "abc abc"))),
                                searchCriteria("abc")
                        )
                )
        );

        runTest(
                "Behavior: no name or tag match should score 0",
                "Covers: if no match is found in either name or tags, the relevance score should be 0.",
                () -> assertEquals(
                        "no matches should score 0",
                        0,
                        searchProducts.relevanceCalculator(normalizedProduct(product("sku-r11", "Desk Lamp", 10.00, List.of("lighting"))), searchCriteria("keyboard"))
                )
        );
    }
    private static void testKeywordMatcher(){
        /**
         * Test cases for keywordMatcher method: product, keyword => boolean
         * 1. Keyword matching:
         *  - product.name rules:
         *      > null or blank names hould not match any keyword => return false
         *      > substring match: if keyword is part of the product name => return true
         *      > case sensitivity: assuming normalized keyword input, normalized name matching should still succeed
         *      > multiple occurence: if found at first occurence, stop the iteration => return true
         *      > "   abc    def  " should be normalized to "abc def" for search
         *  - product.tags rules:
         *      > null or empty tags should not match any keyword => return false
         *      > if any existing tags contains the substring normalized keyword => return true
         *  - combination of name and tags:
         *      > if any of the above rule true => return true
         * 2. keyword rules:
         * - keyword is expected to be already normalized by the parent method before calling keywordMatcher
         * - empty or blank keyword => return false
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
                        searchProducts.keywordMatcher(normalizedProduct(product("sku-k1", null, 10.00, List.of("tech"))), "keyboard")
                )
        );

        runTest(
                "Validation: empty product name should not match any keyword",
                "Covers: product.name empty should return false and there is no need to attempt contains on an empty normalized name.",
                () -> assertFalse(
                        "empty product name should not match",
                        searchProducts.keywordMatcher(normalizedProduct(product("sku-k1a", "", 10.00, List.of("tech"))), "keyboard")
                )
        );

        runTest(
                "Validation: blank product name should not match any keyword",
                "Covers: product.name blank should return false.",
                () -> assertFalse(
                        "blank product name should not match",
                        searchProducts.keywordMatcher(normalizedProduct(product("sku-k2", "   ", 10.00, List.of("tech"))), "keyboard")
                )
        );

        runTest(
                "Behavior: substring match inside product name should return true",
                "Covers: if keyword is part of product name, return true.",
                () -> assertTrue(
                        "keyword substring in name should match",
                        searchProducts.keywordMatcher(normalizedProduct(product("sku-k3", "Mechanical Keyboard", 10.00, List.of("tech"))), "board")
                )
        );

        runTest(
                "Behavior: normalized lowercase keyword should match normalized product name",
                "Covers: normalized keyword should match normalized product name content.",
                () -> assertTrue(
                        "normalized keyword should match normalized name",
                        searchProducts.keywordMatcher(normalizedProduct(product("sku-k4", "Mechanical Keyboard", 10.00, List.of("tech"))), "keyboard")
                )
        );

        runTest(
                "Behavior: normalized spaces in product name should still match",
                "Covers: \"   abc    def  \" should be normalized to \"abc def\" for search.",
                () -> assertTrue(
                        "normalized product name should match normalized keyword",
                        searchProducts.keywordMatcher(normalizedProduct(product("sku-k5", "   abc    def  ", 10.00, List.of("tech"))), "abc def")
                )
        );

        runTest(
                "Behavior: first matching occurrence in product name is enough",
                "Covers: if found at first occurrence, stop iteration and return true.",
                () -> assertTrue(
                        "multiple occurrences in name should still return true once matched",
                        searchProducts.keywordMatcher(normalizedProduct(product("sku-k6", "abc def abc xyz", 10.00, List.of("tech"))), "abc")
                )
        );

        // Case group 2: product tags matching rules.
        runTest(
                "Validation: null product tags should not match any keyword",
                "Covers: null tags should return false when name does not match.",
                () -> assertFalse(
                        "null tags should not match",
                        searchProducts.keywordMatcher(normalizedProduct(product("sku-k7", "Desk Lamp", 10.00, null)), "wireless")
                )
        );

        runTest(
                "Validation: empty product tags should not match any keyword",
                "Covers: empty tags should return false when name does not match.",
                () -> assertFalse(
                        "empty tags should not match",
                        searchProducts.keywordMatcher(normalizedProduct(product("sku-k8", "Desk Lamp", 10.00, List.of())), "wireless")
                )
        );

        runTest(
                "Validation: empty tag value should not match any keyword",
                "Covers: empty tag text should return false and there is no need to attempt contains on an empty normalized tag.",
                () -> assertFalse(
                        "empty tag should not match",
                        searchProducts.keywordMatcher(normalizedProduct(product("sku-k8a", "Desk Lamp", 10.00, List.of(""))), "wireless")
                )
        );

        runTest(
                "Validation: blank tag value should not match any keyword",
                "Covers: blank tag text should return false and there is no need to attempt contains on a blank normalized tag.",
                () -> assertFalse(
                        "blank tag should not match",
                        searchProducts.keywordMatcher(normalizedProduct(product("sku-k8b", "Desk Lamp", 10.00, List.of("   "))), "wireless")
                )
        );

        runTest(
                "Behavior: substring match inside any tag should return true",
                "Covers: if any existing tag contains the substring normalized keyword, return true.",
                () -> assertTrue(
                        "substring inside tag should match",
                        searchProducts.keywordMatcher(normalizedProduct(product("sku-k9", "Desk Lamp", 10.00, List.of("home office", "lighting"))), "office")
                )
        );

        runTest(
                "Behavior: pre-normalized keyword should match normalized tag text",
                "Covers: normalized keyword should be used for tag matching too.",
                () -> assertTrue(
                        "pre-normalized keyword should match normalized tag",
                        searchProducts.keywordMatcher(normalizedProduct(product("sku-k10", "Desk Lamp", 10.00, Arrays.asList("  abc   def  ", "lighting"))), "abc def")
                )
        );

        // Case group 3: combination of name and tags.
        runTest(
                "Behavior: matching product name alone should return true even if tags do not match",
                "Covers: if any name rule is true, return true.",
                () -> assertTrue(
                        "name match should be enough even when tags miss",
                        searchProducts.keywordMatcher(normalizedProduct(product("sku-k11", "Gaming Keyboard", 10.00, List.of("office"))), "keyboard")
                )
        );

        runTest(
                "Behavior: matching tag alone should return true even if name does not match",
                "Covers: if any tag rule is true, return true.",
                () -> assertTrue(
                        "tag match should be enough even when name misses",
                        searchProducts.keywordMatcher(normalizedProduct(product("sku-k12", "Desk Lamp", 10.00, List.of("gaming gear"))), "gear")
                )
        );

        // Case group 4: keyword normalization and blank handling.
        runTest(
                "Behavior: pre-normalized keyword should match normalized name",
                "Covers: keywordMatcher expects already-normalized keyword input.",
                () -> assertTrue(
                        "pre-normalized keyword should match normalized name",
                        searchProducts.keywordMatcher(normalizedProduct(product("sku-k13", "abc def", 10.00, List.of("tech"))), "abc def")
                )
        );

        runTest(
                "Validation: empty keyword should return false",
                "Covers: empty keyword should return false.",
                () -> assertFalse(
                        "empty keyword should not match",
                        searchProducts.keywordMatcher(normalizedProduct(product("sku-k14", "Mechanical Keyboard", 10.00, List.of("tech"))), "")
                )
        );

        runTest(
                "Validation: blank keyword should return false",
                "Covers: blank keyword should return false.",
                () -> assertFalse(
                        "blank keyword should not match",
                        searchProducts.keywordMatcher(normalizedProduct(product("sku-k15", "Mechanical Keyboard", 10.00, List.of("tech"))), "   ")
                )
        );
    }

    private static void testSearchProducts() {
        /**
         * Test cases for searchProducts method:
         * > validation on input parameter (all null/blank cases)
         * > in case of SearchCriteria.keyword is null/blank post/prior to normalization
         * > in case products have null product
         * > in case name normalization fails
         * > in case tag normalization fails
         * > in case there's no match to keyword
         * > in case there are multiple matches with different relevance score and price to verify the sorting logic: relevance desc, price asc
         * > in case all products are matched with the same relevance score to verify the price sorting
         * > in case all products are matched with the same relevance score and price to verify the stability of sorting (insertion order should be kept)
         */

        printSection("searchProducts");

        SearchProducts searchProducts = new SearchProducts();

        runTest(
                "Validation: null products collection should be rejected",
                "Covers: products cannot be null.",
                () -> assertThrows(
                        "null products should be rejected",
                        IllegalArgumentException.class,
                        () -> searchProducts.searchProducts(null, searchCriteria("keyboard"))
                )
        );

        runTest(
                "Validation: null searchCriteria should be rejected",
                "Covers: searchCriteria cannot be null.",
                () -> assertThrows(
                        "null searchCriteria should be rejected",
                        IllegalArgumentException.class,
                        () -> searchProducts.searchProducts(new LinkedHashSet<>(), null)
                )
        );

        runTest(
                "Validation: negative maxPrice should be rejected",
                "Covers: maxPrice below zero should throw IllegalArgumentException.",
                () -> assertThrows(
                        "negative maxPrice should be rejected",
                        IllegalArgumentException.class,
                        () -> searchProducts.searchProducts(new LinkedHashSet<>(), searchCriteria("keyboard", -0.01))
                )
        );

        runTest(
                "Validation: maxPrice with more than two decimals should be rejected",
                "Covers: maxPrice should allow only two decimal places.",
                () -> assertThrows(
                        "maxPrice with more than two decimals should be rejected",
                        IllegalArgumentException.class,
                        () -> searchProducts.searchProducts(new LinkedHashSet<>(), searchCriteria("keyboard", 10.999))
                )
        );

        runTest(
                "Behavior: null keyword should return empty result",
                "Covers: null keyword prior to normalization should return an empty list.",
                () -> assertEquals(
                        "null keyword should return an empty list",
                        0,
                        searchProducts.searchProducts(Set.of(product("sku-s0", "Keyboard", 10.00, List.of("tech"))), searchCriteria(null)).size()
                )
        );

        runTest(
                "Behavior: blank keyword should return empty result",
                "Covers: blank keyword prior to normalization should return an empty list.",
                () -> assertEquals(
                        "blank keyword should return an empty list",
                        0,
                        searchProducts.searchProducts(Set.of(product("sku-s0b", "Keyboard", 10.00, List.of("tech"))), searchCriteria("   ")).size()
                )
        );

        runTest(
                "Validation: null product inside products should be rejected",
                "Covers: products containing a null product should fail during normalization.",
                () -> {
                    Set<Product> products = new LinkedHashSet<>();
                    products.add(product("sku-s1", "Keyboard", 10.00, List.of("tech")));
                    products.add(null);

                    assertThrows(
                            "null product inside products should be rejected",
                            IllegalArgumentException.class,
                            () -> searchProducts.searchProducts(products, searchCriteria("keyboard"))
                    );
                }
        );

        runTest(
                "Behavior: product with null name should not match keyword",
                "Covers: name normalization failure path should not match and should not break the search.",
                () -> {
                    Set<Product> products = new LinkedHashSet<>();
                    products.add(product("sku-s2", null, 10.00, List.of("tech")));
                    products.add(product("sku-s3", "Gaming Keyboard", 20.00, List.of("tech")));

                    List<Product> result = searchProducts.searchProducts(products, searchCriteria("keyboard"));

                    assertEquals("only the valid matching product should be returned", 1, result.size());
                    assertProductEquals("matching product should remain searchable", product("sku-s3", "Gaming Keyboard", 20.00, List.of("tech")), result.get(0));
                }
        );

        runTest(
                "Behavior: product with null and blank tags should still search safely",
                "Covers: tag normalization failure path should not match broken tags and should not break the search.",
                () -> {
                    Set<Product> products = new LinkedHashSet<>();
                    Product brokenTags = product("sku-s4", "Desk Lamp", 10.00, Arrays.asList(null, "   "));
                    Product matchingTags = product("sku-s5", "Desk Lamp", 20.00, List.of("home office"));
                    products.add(brokenTags);
                    products.add(matchingTags);

                    List<Product> result = searchProducts.searchProducts(products, searchCriteria("office"));

                    assertEquals("only the product with a real matching tag should be returned", 1, result.size());
                    assertProductEquals("matching tag product should be returned", matchingTags, result.get(0));
                }
        );

        runTest(
                "Behavior: no keyword match should return empty result",
                "Covers: when no products match the keyword, searchProducts should return an empty list.",
                () -> assertEquals(
                        "no matches should return an empty list",
                        0,
                        searchProducts.searchProducts(
                                Set.of(
                                        product("sku-s6", "Desk Lamp", 10.00, List.of("lighting")),
                                        product("sku-s7", "Office Chair", 20.00, List.of("furniture"))
                                ),
                                searchCriteria("keyboard")
                        ).size()
                )
        );

        runTest(
                "Behavior: results should sort by relevance descending then price ascending",
                "Covers: multiple matches with different relevance and price should sort by relevance desc, then price asc.",
                () -> {
                    Set<Product> products = new LinkedHashSet<>();
                    Product topRelevance = product("sku-s8", "keyboard keyboard", 100.00, List.of("tech"));
                    Product mediumRelevanceLowerPrice = product("sku-s9", "keyboard", 20.00, List.of("keyboard"));
                    Product mediumRelevanceHigherPrice = product("sku-s10", "keyboard", 30.00, List.of("keyboard"));
                    products.add(topRelevance);
                    products.add(mediumRelevanceHigherPrice);
                    products.add(mediumRelevanceLowerPrice);

                    List<Product> result = searchProducts.searchProducts(products, searchCriteria("keyboard"));

                    assertEquals("all matching products should be returned", 3, result.size());
                    assertProductEquals("highest relevance product should be first", topRelevance, result.get(0));
                    assertProductEquals("lower priced product should win the tie on equal relevance", mediumRelevanceLowerPrice, result.get(1));
                    assertProductEquals("higher priced tied product should come after the lower priced one", mediumRelevanceHigherPrice, result.get(2));
                }
        );

        runTest(
                "Behavior: equal relevance products should sort by price ascending",
                "Covers: when all products have the same relevance score, lower price should come first.",
                () -> {
                    Set<Product> products = new LinkedHashSet<>();
                    Product cheapest = product("sku-s11", "keyboard", 10.00, List.of("tech"));
                    Product middle = product("sku-s12", "keyboard", 20.00, List.of("tech"));
                    Product highest = product("sku-s13", "keyboard", 30.00, List.of("tech"));
                    products.add(highest);
                    products.add(cheapest);
                    products.add(middle);

                    List<Product> result = searchProducts.searchProducts(products, searchCriteria("keyboard"));

                    assertEquals("all equally relevant products should be returned", 3, result.size());
                    assertProductEquals("cheapest product should be first", cheapest, result.get(0));
                    assertProductEquals("middle priced product should be second", middle, result.get(1));
                    assertProductEquals("highest priced product should be third", highest, result.get(2));
                }
        );

        runTest(
                "Behavior: equal relevance and equal price should keep insertion order",
                "Covers: stable sorting should preserve insertion order when relevance and price are identical.",
                () -> {
                    Set<Product> products = new LinkedHashSet<>();
                    Product first = product("sku-s14", "keyboard", 25.00, List.of("tech"));
                    Product second = product("sku-s15", "keyboard", 25.00, List.of("office"));
                    Product third = product("sku-s16", "keyboard", 25.00, List.of("gaming"));
                    products.add(first);
                    products.add(second);
                    products.add(third);

                    List<Product> result = searchProducts.searchProducts(products, searchCriteria("keyboard"));

                    assertEquals("all tied products should be returned", 3, result.size());
                    assertProductEquals("first inserted product should stay first", first, result.get(0));
                    assertProductEquals("second inserted product should stay second", second, result.get(1));
                    assertProductEquals("third inserted product should stay third", third, result.get(2));
                }
        );
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
}

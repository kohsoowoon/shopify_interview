package shopify.auto.complete;

import java.util.List;
import java.util.Objects;

public class AutocompleteSuggestionTest {
    private static int totalTests = 0;
    private static int passedTests = 0;
    private static int failedTests = 0;

    public static void main(String[] args) {
        printSuiteHeader("AutocompleteSystem — plain Java test runner");

        testNullAndEmptyProductList();
        testNullAndBlankPrefixValidation();
        testBasicTokenMatching();
        testTokenWeightedScoring();

        printSuiteSummary();
    }

    // ---------------------------------------------------------------
    // Test sections
    // ---------------------------------------------------------------

    private static void testNullAndEmptyProductList() {
        printSection("null and empty product list validation");

        runTest(
                "Validation: null product list returns empty list without throwing",
                "Covers: constructing with a null product list and querying should return empty without any operation.",
                () -> {
                    AutocompleteSystem system = new AutocompleteSystem(null);
                    assertListEquals("null product list should return empty list",
                            List.of(), system.getAutocompleteSuggestions("iphone", 10));
                }
        );

        runTest(
                "Validation: empty product list returns empty list without throwing",
                "Covers: constructing with an empty product list and querying should return empty without any operation.",
                () -> {
                    AutocompleteSystem system = new AutocompleteSystem(List.of());
                    assertListEquals("empty product list should return empty list",
                            List.of(), system.getAutocompleteSuggestions("iphone", 10));
                }
        );
    }

    private static void testNullAndBlankPrefixValidation() {
        printSection("null and blank prefix validation");

        runTest(
                "Validation: null prefix returns empty list",
                "Covers: null prefix should return an empty list without throwing.",
                () -> {
                    AutocompleteSystem system = new AutocompleteSystem(List.of("iPhone 15", "iPad"));
                    assertListEquals("null prefix should return empty list",
                            List.of(), system.getAutocompleteSuggestions(null, 10));
                }
        );

        runTest(
                "Validation: empty string prefix returns empty list",
                "Covers: empty string prefix should return an empty list.",
                () -> {
                    AutocompleteSystem system = new AutocompleteSystem(List.of("iPhone 15", "iPad"));
                    assertListEquals("empty string prefix should return empty list",
                            List.of(), system.getAutocompleteSuggestions("", 10));
                }
        );

        runTest(
                "Validation: whitespace-only prefix returns empty list",
                "Covers: a prefix made only of spaces should return an empty list.",
                () -> {
                    AutocompleteSystem system = new AutocompleteSystem(List.of("iPhone 15", "iPad"));
                    assertListEquals("whitespace-only prefix should return empty list",
                            List.of(), system.getAutocompleteSuggestions("   ", 10));
                }
        );
    }

    private static void testBasicTokenMatching() {
        printSection("basic token matching");

        runTest(
                "Behavior: prefix matching no product token returns empty list",
                "Covers: when no product token starts with any prefix token, result should be empty.",
                () -> {
                    AutocompleteSystem system = new AutocompleteSystem(List.of("iPhone 15", "MacBook Pro"));
                    assertListEquals("unrelated prefix should return empty list",
                            List.of(), system.getAutocompleteSuggestions("xyz", 10));
                }
        );

        runTest(
                "Behavior: single product matches the prefix token",
                "Covers: only the product whose token starts with the prefix token should be returned.",
                () -> {
                    AutocompleteSystem system = new AutocompleteSystem(List.of("iPhone 15", "MacBook Pro"));
                    assertListEquals("'mac' should match only MacBook Pro",
                            List.of("MacBook Pro"), system.getAutocompleteSuggestions("mac", 10));
                }
        );

        runTest(
                "Behavior: multiple products match the prefix token",
                "Covers: all products with a matching token should be included in the result.",
                () -> {
                    AutocompleteSystem system = new AutocompleteSystem(
                            List.of("iPhone 15", "iPhone 14", "MacBook Pro"));
                    List<String> results = system.getAutocompleteSuggestions("iphone", 10);
                    assertContains("'iphone' prefix should include iPhone 15", results, "iPhone 15");
                    assertContains("'iphone' prefix should include iPhone 14", results, "iPhone 14");
                    assertEquals("'iphone' prefix should exclude MacBook Pro", 2, results.size());
                }
        );

        runTest(
                "Behavior: prefix found in the middle of a token is not a match",
                "Covers: 'iph' should not match 'Giphy 10' because 'giphy' does not start with 'iph' — only token-prefix matching is supported, not substring matching.",
                () -> {
                    AutocompleteSystem system = new AutocompleteSystem(List.of("Giphy 10", "iPhone 15"));
                    List<String> results = system.getAutocompleteSuggestions("iph", 10);
                    assertContains("'iph' prefix should match iPhone 15", results, "iPhone 15");
                    assertEquals("'iph' should not match 'Giphy 10' as a mid-token substring", 1, results.size());
                }
        );

        runTest(
                "Behavior: maxSuggestions caps the number of results",
                "Covers: result size should not exceed maxSuggestions even when more products match.",
                () -> {
                    AutocompleteSystem system = new AutocompleteSystem(
                            List.of("iphone 15", "iphone 14", "iphone 13", "iphone 12"));
                    List<String> results = system.getAutocompleteSuggestions("iph", 2);
                    assertEquals("result size should be capped at maxSuggestions", 2, results.size());
                }
        );
    }

    private static void testTokenWeightedScoring() {
        printSection("token-weighted scoring");

        runTest(
                "Scoring: product matching more prefix tokens ranks higher regardless of token order",
                "Covers: '15 iphone' matches both prefix tokens 'iph' and '15', so it should rank above 'iphone' which matches only 'iph'.",
                () -> {
                    AutocompleteSystem system = new AutocompleteSystem(List.of("iphone", "15 iphone"));
                    List<String> results = system.getAutocompleteSuggestions("iph 15", 10);
                    assertRankedBefore(
                            "'15 iphone' (2 token matches) should rank above 'iphone' (1 token match)",
                            results, "15 iphone", "iphone");
                }
        );

        runTest(
                "Scoring: in-order token matching ranks higher than out-of-order at the same match count",
                "Covers: 'iphone 15' tokens match prefix tokens 'iph','15' in sequence, so it should rank above '15 iphone' which matches in reverse.",
                () -> {
                    AutocompleteSystem system = new AutocompleteSystem(List.of("15 iphone", "iphone 15"));
                    List<String> results = system.getAutocompleteSuggestions("iph 15", 10);
                    assertRankedBefore(
                            "'iphone 15' (in-order match) should rank above '15 iphone' (reversed match)",
                            results, "iphone 15", "15 iphone");
                }
        );

        runTest(
                "Scoring: full ranking for 'iph 15' is iphone 15 > 15 iphone > iphone > 15",
                "Covers: combined match count, sequence order, and first-token priority produce the expected ranking.",
                () -> {
                    AutocompleteSystem system = new AutocompleteSystem(
                            List.of("15", "iphone", "15 iphone", "iphone 15"));
                    List<String> results = system.getAutocompleteSuggestions("iph 15", 10);
                    assertListEquals(
                            "full ranking should be: iphone 15 > 15 iphone > iphone > 15",
                            List.of("iphone 15", "15 iphone", "iphone", "15"),
                            results);
                }
        );

        runTest(
                "Scoring: matching the first prefix token ranks higher than matching a later prefix token",
                "Covers: '15 cents' matches the first prefix token '15' so it should rank above 'iphone' which matches only the second prefix token 'iph'.",
                () -> {
                    AutocompleteSystem system = new AutocompleteSystem(List.of("iphone", "15 cents"));
                    List<String> results = system.getAutocompleteSuggestions("15 iph", 10);
                    assertRankedBefore(
                            "'15 cents' (first-token match) should rank above 'iphone' (second-token match)",
                            results, "15 cents", "iphone");
                }
        );

        runTest(
                "Scoring: 3-token prefix — product matching all three tokens ranks above products matching only two",
                "Covers: 'iphone 15 refurbished' matches all three prefix tokens 'iph','15','ref' so it should rank above products that match only two.",
                () -> {
                    AutocompleteSystem system = new AutocompleteSystem(List.of(
                            "iphone 15 refurbished", "iphone 15 open box",
                            "iphone 14 refurbished", "iphone 15"));
                    List<String> results = system.getAutocompleteSuggestions("iph 15 ref", 10);
                    assertRankedBefore("3-token match should rank above 2-token match (iphone 15 open box)",
                            results, "iphone 15 refurbished", "iphone 15 open box");
                    assertRankedBefore("3-token match should rank above 2-token match (iphone 14 refurbished)",
                            results, "iphone 15 refurbished", "iphone 14 refurbished");
                    assertRankedBefore("3-token match should rank above 2-token match (iphone 15)",
                            results, "iphone 15 refurbished", "iphone 15");
                }
        );

        runTest(
                "Scoring: tied scores are broken by case-insensitive alphabetical order",
                "Covers: when all products score equally, results should be sorted alphabetically (case-insensitive) — iphon, iphone, iphone 13, iphone 15, iphone 19, iphone app.",
                () -> {
                    AutocompleteSystem system = new AutocompleteSystem(List.of(
                            "iphone 19", "iphone app", "iphone 13", "iphone", "iphon", "iphone 15"));
                    List<String> results = system.getAutocompleteSuggestions("iph", 10);
                    assertListEquals(
                            "tied scores should fall back to alphabetical order",
                            List.of("iphon", "iphone", "iphone 13", "iphone 15", "iphone 19", "iphone app"),
                            results);
                }
        );

        runTest(
                "Scoring: exact token match ranks higher than prefix-only token match",
                "Covers: when prefix is 'iphone', the product 'iphone' (exact match) should rank above 'iphone15' (prefix match) because full token equality is stronger than a partial prefix hit.",
                () -> {
                    AutocompleteSystem system = new AutocompleteSystem(List.of("iphone15", "iphone"));
                    List<String> results = system.getAutocompleteSuggestions("iphone", 10);
                    assertRankedBefore(
                            "'iphone' (exact token match) should rank above 'iphone15' (prefix-only match)",
                            results, "iphone", "iphone15");
                }
        );
    }

    // ---------------------------------------------------------------
    // Assertion helpers
    // ---------------------------------------------------------------

    private static void assertListEquals(String message, List<String> expected, List<String> actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + ". Expected " + expected + " but was " + actual + ".");
        }
    }

    private static void assertEquals(String message, Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + ". Expected <" + expected + "> but was <" + actual + ">.");
        }
    }

    private static void assertContains(String message, List<String> list, String expected) {
        if (!list.contains(expected)) {
            throw new AssertionError(message + ". List " + list + " does not contain '" + expected + "'.");
        }
    }

    private static void assertRankedBefore(String message, List<String> results, String higher, String lower) {
        int higherIndex = results.indexOf(higher);
        int lowerIndex = results.indexOf(lower);
        if (higherIndex < 0) {
            throw new AssertionError(message + ". '" + higher + "' was not found in results " + results + ".");
        }
        if (lowerIndex < 0) {
            throw new AssertionError(message + ". '" + lower + "' was not found in results " + results + ".");
        }
        if (higherIndex >= lowerIndex) {
            throw new AssertionError(message + ". Expected '" + higher + "' at index " + higherIndex
                    + " to appear before '" + lower + "' at index " + lowerIndex + ".");
        }
    }

    // ---------------------------------------------------------------
    // Test runner infrastructure
    // ---------------------------------------------------------------

    private static void runTest(String testName, String description, ThrowingRunnable testBody) {
        totalTests++;
        System.out.println();
        System.out.println("TEST " + totalTests + ": " + testName);
        System.out.println("  " + description);
        try {
            testBody.run();
            passedTests++;
            System.out.println("  RESULT: PASS");
        } catch (AssertionError e) {
            failedTests++;
            System.out.println("  RESULT: FAIL");
            System.out.println("  REASON: " + e.getMessage());
        } catch (Throwable t) {
            failedTests++;
            System.out.println("  RESULT: FAIL");
            System.out.println("  REASON: Unexpected " + t.getClass().getSimpleName() + " — " + t.getMessage());
        }
    }

    private static void printSuiteHeader(String title) {
        System.out.println("==============================================");
        System.out.println(title);
        System.out.println("==============================================");
    }

    private static void printSection(String sectionName) {
        System.out.println();
        System.out.println("----------------------------------------------");
        System.out.println("Running tests for: " + sectionName);
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

package shopify.product.search;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class TestHelper {
    private static int totalTests = 0;
    private static int passedTests = 0;
    private static int failedTests = 0;
    private static final SearchProducts SEARCH_PRODUCTS = new SearchProducts();

    private TestHelper() {
    }

    public static Product product(String id, String name, double price, List<String> tags) {
        return new Product(id, name, price, tags);
    }

    public static NormalizedProduct normalizedProduct(Product product) {
        return SEARCH_PRODUCTS.normalizeProduct(product);
    }

    public static SearchCriteria searchCriteria(String keyword) {
        return searchCriteria(keyword, Double.MAX_VALUE);
    }

    public static SearchCriteria searchCriteria(String keyword, Double maxPrice) {
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.keyword = keyword;
        searchCriteria.maxPrice = maxPrice;
        return searchCriteria;
    }

    public static Product findById(Set<Product> products, String id) {
        return products.stream()
                .filter(product -> Objects.equals(product.id(), id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Could not find product with id '" + id + "'."));
    }

    public static void runTest(String testName, String description, ThrowingRunnable testBody) {
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

    public static void assertEquals(String failureMessage, Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(failureMessage + ". Expected <" + expected + "> but was <" + actual + ">.");
        }
    }

    public static void assertTrue(String failureMessage, boolean actual) {
        if (!actual) {
            throw new AssertionError(failureMessage + ". Expected <true> but was <false>.");
        }
    }

    public static void assertFalse(String failureMessage, boolean actual) {
        if (actual) {
            throw new AssertionError(failureMessage + ". Expected <false> but was <true>.");
        }
    }

    public static void assertProductEquals(String failureMessage, Product expected, Product actual) {
        if (!Objects.equals(expected.id(), actual.id())
                || !Objects.equals(expected.name(), actual.name())
                || Double.compare(expected.price(), actual.price()) != 0
                || !Objects.equals(expected.tags(), actual.tags())) {
            throw new AssertionError(failureMessage + ". Expected <" + expected + "> but was <" + actual + ">.");
        }
    }

    public static void assertThrows(String failureMessage, Class<? extends Throwable> expectedType, ThrowingRunnable action) {
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

    public static void printSuiteHeader(String title) {
        System.out.println("==============================================");
        System.out.println(title);
        System.out.println("==============================================");
    }

    public static void printSection(String sectionName) {
        System.out.println();
        System.out.println("----------------------------------------------");
        System.out.println("Running tests for " + sectionName);
        System.out.println("----------------------------------------------");
    }

    public static void printSuiteSummary() {
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
    public interface ThrowingRunnable {
        void run() throws Exception;
    }
}

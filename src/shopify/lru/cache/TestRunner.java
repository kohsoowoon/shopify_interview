package shopify.lru.cache;

public class TestRunner {

    public static void run(String name, Runnable test) {
        try {
            test.run();
            System.out.println("PASS: " + name);
        } catch (AssertionError e) {
            System.out.println("FAIL: " + name + " — " + e.getMessage());
        }
    }
}

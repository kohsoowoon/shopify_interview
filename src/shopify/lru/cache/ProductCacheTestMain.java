package shopify.lru.cache;

public class ProductCacheTestMain {

    public static void main(String[] args) {
        System.out.println("=== ProductCacheEmptyTest ===");
        ProductCacheEmptyTest.runAll();

        System.out.println("\n=== ProductCacheGetTest ===");
        ProductCacheGetTest.runAll();

        System.out.println("\n=== ProductCachePutTest ===");
        ProductCachePutTest.runAll();
    }
}

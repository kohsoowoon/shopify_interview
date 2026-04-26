package shopify.lru.cache;

public class ProductCacheEmptyTest {

    public static void runAll() {
        TestRunner.run("testGetOnEmptyCache", ProductCacheEmptyTest::testGetOnEmptyCache);
        TestRunner.run("testGetWithNullProductId", ProductCacheEmptyTest::testGetWithNullProductId);
        TestRunner.run("testGetWithBlankProductId", ProductCacheEmptyTest::testGetWithBlankProductId);
        TestRunner.run("testPutWithNullProductId", ProductCacheEmptyTest::testPutWithNullProductId);
        TestRunner.run("testPutWithBlankProductId", ProductCacheEmptyTest::testPutWithBlankProductId);
        TestRunner.run("testPutWithNullProductDetail", ProductCacheEmptyTest::testPutWithNullProductDetail);
    }

    // get on empty cache should return null
    static void testGetOnEmptyCache() {
        ProductCache cache = new ProductCache(5);
        assert cache.get("p1") == null : "Expected null for missing productId";
    }

    // get with null productId should return null
    static void testGetWithNullProductId() {
        ProductCache cache = new ProductCache(5);
        assert cache.get(null) == null : "Expected null for null productId";
    }

    // get with blank productId should return null
    static void testGetWithBlankProductId() {
        ProductCache cache = new ProductCache(5);
        assert cache.get("") == null : "Expected null for blank productId";
        assert cache.get("   ") == null : "Expected null for whitespace productId";
    }

    // put with null productId should do nothing
    static void testPutWithNullProductId() {
        ProductCache cache = new ProductCache(5);
        cache.put(null, new ProductDetail("p1", null));
        assert cache.get(null) == null : "Expected null after put with null productId";
    }

    // put with blank productId should do nothing
    static void testPutWithBlankProductId() {
        ProductCache cache = new ProductCache(5);
        cache.put("", new ProductDetail("p1", null));
        assert cache.get("") == null : "Expected null after put with blank productId";
        cache.put("   ", new ProductDetail("p1", null));
        assert cache.get("   ") == null : "Expected null after put with whitespace productId";
    }

    // put with null productDetail should do nothing
    static void testPutWithNullProductDetail() {
        ProductCache cache = new ProductCache(5);
        cache.put("p1", null);
        assert cache.get("p1") == null : "Expected null after put with null productDetail";
    }
}

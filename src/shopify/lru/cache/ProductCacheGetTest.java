package shopify.lru.cache;

public class ProductCacheGetTest {

    public static void runAll() {
        TestRunner.run("get returns product detail when productId exists in cache",
                ProductCacheGetTest::testGetReturnsProductDetailForExistingProductId);
        TestRunner.run("get returns null when productId does not exist in cache",
                ProductCacheGetTest::testGetReturnsNullForNonExistentProductId);
        TestRunner.run("get updates access order so accessed product is not evicted first",
                ProductCacheGetTest::testGetUpdatesAccessOrderForLruEviction);
    }

    // put a product, then get it — should return the same product detail
    static void testGetReturnsProductDetailForExistingProductId() {
        ProductCache cache = new ProductCache(5);
        ProductDetail product = new ProductDetail("p1", "metadata-p1");
        cache.put("p1", product);

        ProductDetail result = cache.get("p1");

        assert result != null : "Expected product detail but got null for productId 'p1'";
        assert result.productId.equals("p1") : "Expected productId 'p1' but got: " + result.productId;
    }

    // get a productId that was never inserted — should return null
    static void testGetReturnsNullForNonExistentProductId() {
        ProductCache cache = new ProductCache(5);
        cache.put("p1", new ProductDetail("p1", "metadata-p1"));

        ProductDetail result = cache.get("p99");

        assert result == null : "Expected null for non-existent productId 'p99' but got: " + result;
    }

    // fill cache, access p1 to make it most recently used,
    // then insert a new product — p1 should survive eviction, p2 (oldest) should be evicted
    static void testGetUpdatesAccessOrderForLruEviction() {
        ProductCache cache = new ProductCache(3);
        cache.put("p1", new ProductDetail("p1", "metadata-p1"));
        cache.put("p2", new ProductDetail("p2", "metadata-p2"));
        cache.put("p3", new ProductDetail("p3", "metadata-p3"));

        // access p1 — it becomes most recently used, p2 is now the oldest
        cache.get("p1");

        // insert p4 — cache is full, p2 (oldest accessed) should be evicted
        cache.put("p4", new ProductDetail("p4", "metadata-p4"));

        assert cache.get("p1") != null : "Expected 'p1' to still be in cache after get promoted it";
        assert cache.get("p2") == null : "Expected 'p2' to be evicted as the least recently used";
        assert cache.get("p3") != null : "Expected 'p3' to still be in cache";
        assert cache.get("p4") != null : "Expected 'p4' to be in cache after insertion";
    }
}

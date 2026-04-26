package shopify.lru.cache;

public class ProductCachePutTest {

    public static void runAll() {
        TestRunner.run("put adds product to cache when cache is below capacity",
                ProductCachePutTest::testPutAddsProductWhenCacheIsBelowCapacity);
        TestRunner.run("put overwrites existing product detail with latest data for same productId",
                ProductCachePutTest::testPutOverwritesExistingProductDetailWithLatestData);
        TestRunner.run("put on existing productId promotes it to most recently used so it is not evicted first",
                ProductCachePutTest::testPutOnExistingProductIdUpdatesAccessOrder);
        TestRunner.run("put evicts least recently used product and cache size never exceeds capacity",
                ProductCachePutTest::testPutEvictsLruAndCacheSizeNeverExceedsCapacity);
    }

    // cache has room — product should be retrievable after put
    static void testPutAddsProductWhenCacheIsBelowCapacity() {
        ProductCache cache = new ProductCache(5);
        cache.put("p1", new ProductDetail("p1", "metadata-p1"));

        ProductDetail result = cache.get("p1");

        assert result != null : "Expected 'p1' to be in cache after put but got null";
        assert result.productId.equals("p1") : "Expected productId 'p1' but got: " + result.productId;
    }

    // putting same productId twice — second put should overwrite with latest product detail
    static void testPutOverwritesExistingProductDetailWithLatestData() {
        ProductCache cache = new ProductCache(5);
        cache.put("p1", new ProductDetail("p1", "original-metadata"));
        cache.put("p1", new ProductDetail("p1", "updated-metadata"));

        ProductDetail result = cache.get("p1");

        assert result != null : "Expected 'p1' to be in cache but got null";
        assert result.productMetadata.equals("updated-metadata")
                : "Expected updated metadata after second put but got: " + result.productMetadata;
    }

    // fill cache to capacity, then overwrite p1 — p1 becomes mru, p2 becomes lru
    // insert p4 — p2 should be evicted, p1 survives because overwrite promoted it
    static void testPutOnExistingProductIdUpdatesAccessOrder() {
        ProductCache cache = new ProductCache(3);
        cache.put("p1", new ProductDetail("p1", "metadata-p1"));
        cache.put("p2", new ProductDetail("p2", "metadata-p2"));
        cache.put("p3", new ProductDetail("p3", "metadata-p3"));

        // overwrite p1 — promotes p1 to mru, p2 is now the lru
        cache.put("p1", new ProductDetail("p1", "metadata-p1-updated"));

        // insert p4 — p2 should be evicted as lru
        cache.put("p4", new ProductDetail("p4", "metadata-p4"));

        assert cache.get("p1") != null : "Expected 'p1' to still be in cache — overwrite should have promoted it to mru";
        assert cache.get("p2") == null : "Expected 'p2' to be evicted as least recently used";
        assert cache.get("p3") != null : "Expected 'p3' to still be in cache";
        assert cache.get("p4") != null : "Expected 'p4' to be in cache after insertion";
    }

    // fill cache to capacity, insert one more — lru evicted, size stays at capacity
    // also verify capacity 1 edge case — only the latest inserted product survives
    static void testPutEvictsLruAndCacheSizeNeverExceedsCapacity() {
        ProductCache cache = new ProductCache(3);
        cache.put("p1", new ProductDetail("p1", "metadata-p1"));
        cache.put("p2", new ProductDetail("p2", "metadata-p2"));
        cache.put("p3", new ProductDetail("p3", "metadata-p3"));

        // p1 is lru — inserted first, never accessed again
        cache.put("p4", new ProductDetail("p4", "metadata-p4"));

        assert cache.get("p1") == null : "Expected 'p1' to be evicted as least recently used";
        assert cache.get("p2") != null : "Expected 'p2' to still be in cache";
        assert cache.get("p3") != null : "Expected 'p3' to still be in cache";
        assert cache.get("p4") != null : "Expected 'p4' to be in cache after insertion";

        // capacity 1 edge case — each new insert evicts the only existing product
        ProductCache singleCapacityCache = new ProductCache(1);
        singleCapacityCache.put("p1", new ProductDetail("p1", "metadata-p1"));
        singleCapacityCache.put("p2", new ProductDetail("p2", "metadata-p2"));

        assert singleCapacityCache.get("p1") == null : "Expected 'p1' to be evicted when capacity is 1 and 'p2' was inserted";
        assert singleCapacityCache.get("p2") != null : "Expected 'p2' to be the only product in a capacity-1 cache";
    }
}

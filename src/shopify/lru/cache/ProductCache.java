package shopify.lru.cache;

import java.util.LinkedHashMap;

public class ProductCache {

    private final int capacity;
    private final LinkedHashMap<String, ProductDetail> cache;

    public ProductCache(int capacity) {
        this.capacity = capacity;
        this.cache = new LinkedHashMap<>(capacity, 0.75f, true);
    }

    public synchronized ProductDetail get(String productId) {
        if (productId == null || productId.isBlank()) return null;
        return cache.get(productId);
    }

    public synchronized void put(String productId, ProductDetail productDetails) {
        if (productId == null || productId.isBlank()) return;
        if (productDetails == null) return;

        if (cache.containsKey(productId)) {
            // overwrite with latest data and promote to most recently used
            cache.remove(productId);
            cache.put(productId, productDetails);
            return;
        }

        if (cache.size() >= capacity) {
            String lruKey = cache.keySet().iterator().next();
            cache.remove(lruKey);
        }

        cache.put(productId, productDetails);
    }
}

class ProductDetail {
    String productId;
    Object productMetadata;

    ProductDetail(String productId, Object productMetadata) {
        this.productId = productId;
        this.productMetadata = productMetadata;
    }
}
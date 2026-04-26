package shopify.lru.cache;

import java.util.LinkedHashMap;

public class ProductCache {

    private final LinkedHashMap<String, ProductDetail> cache;

    public ProductCache(int capacity) {
        this.cache = new LinkedHashMap<>();
    }

    public ProductDetail get(String productId) {
        return null;
    }

    public void put(String productId, ProductDetail productDetails) {
        // TODO
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
package shopify.product.search;

import java.util.List;

public record NormalizedProduct(Product originalProduct, String normalizedName, List<String> normalizedTags) {
    public String id() {
        return originalProduct.id();
    }

    public String name() {
        return originalProduct.name();
    }

    public double price() {
        return originalProduct.price();
    }

    public List<String> tags() {
        return originalProduct.tags();
    }
}

package shopify.product.search;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public class SearchProducts {
    public List<Product> searchProducts(Set<Product> products, SearchCriteria searchCriteria){
        /**
         * validate the search criteria:
         * - keyword null/blank > return empty list
         * - maxPrice < 0 : throw IllegalArgumentException
         * - maxPrice has more than 2 decimals: throw IllegalArgumentException
         * Start the stream on products
         * - filter by the searchByCriteria method
         * - sort by relevanceCalculator desc then comparing by price asc
         * - collect to a lit and return
         */
        return null;
    }

    public void addProduct(Set<Product> products, Product product){
        validateProduct(product);

        products.remove(product);
        products.add(product);
    }

    public boolean keywordMatcher(Product product, String keyword){
        /**
         * normalize the keyword: trim and remove multiple spaces into one
         * normalize the product name
         * check the substring match by contain
         * if true, return true
         * else, iterate through tags, normalize each tag and do contain(keyword) to check the match
         * if at any point of iteration, the match found, return true
         * if the iteration ended and still no match, return false.
         */
        if (product == null) {
            throw new IllegalArgumentException("product cannot be null");
        }

        String normalizedKeyword = normalize(keyword);
        if (normalizedKeyword == null || normalizedKeyword.isEmpty()) {
            return false;
        }

        String normalizedName = normalize(product.name());
        if (normalizedName != null && !normalizedName.isBlank() && normalizedName.contains(normalizedKeyword)) {
            return true;
        }

        if (product.tags() == null || product.tags().isEmpty()) {
            return false;
        }

        for (String tag : product.tags()) {
            String normalizedTag = normalize(tag);
            if (normalizedTag != null && !normalizedTag.isBlank() && normalizedTag.contains(normalizedKeyword)) {
                return true;
            }
        }

        return false;
    }

    public boolean searchByCriteria(Product product, SearchCriteria searchCriteria){
        return keywordMatcher(product, searchCriteria.keyword) && product.price() <= searchCriteria.maxPrice;
    }

    public int relevanceCalculator(Product product, SearchCriteria searchCriteria){
        return 0;
    }

    private void validateProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("product cannot be null");
        }

        if (isBlank(product.id())) {
            throw new IllegalArgumentException("product id cannot be null or blank");
        }

        if (isBlank(product.name())) {
            throw new IllegalArgumentException("product name cannot be null or blank");
        }

        if (product.price() < 0) {
            throw new IllegalArgumentException("product price must be greater than or equal to 0");
        }

        if (hasMoreThanTwoDecimals(product.price())) {
            throw new IllegalArgumentException("product price cannot have more than two decimal places");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean hasMoreThanTwoDecimals(double price) {
        return BigDecimal.valueOf(price).stripTrailingZeros().scale() > 2;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        return trimmed.replaceAll("\\s+", " ").toLowerCase();
    }
}

class SearchCriteria{
    String keyword;
    Double maxPrice;
    //sorting defaulted to: relevance(number of keyword matches) desc + price asc order
    //relevance score calculation rules:
}

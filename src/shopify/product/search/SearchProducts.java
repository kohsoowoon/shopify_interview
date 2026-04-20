package shopify.product.search;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class SearchProducts {
    public List<Product> searchProducts(Set<Product> products, SearchCriteria searchCriteria){
        /**
         * validate the search criteria:
         * - keyword null/blank > return empty list
         * - maxPrice < 0 : throw IllegalArgumentException
         * - maxPrice has more than 2 decimals: throw IllegalArgumentException
         * - searchCriteria is null: throw IllegalArgumentException
         * Start the stream on products
         * - filter by the searchByCriteria method
         * - sort by relevanceCalculator desc then comparing by price asc
         * - collect to a lit and return
         */
        if (products == null) {
            throw new IllegalArgumentException("products cannot be null");
        }

        validateSearchCriteria(searchCriteria);

        String normalizedKeyword = normalize(searchCriteria.keyword);
        if (normalizedKeyword == null || normalizedKeyword.isEmpty()) {
            return List.of();
        }

        SearchCriteria normalizedSearchCriteria = new SearchCriteria();
        normalizedSearchCriteria.keyword = normalizedKeyword;
        normalizedSearchCriteria.maxPrice = searchCriteria.maxPrice;

        return products.stream()
                .map(this::normalizeProduct)
                .filter(normalizedProduct -> searchByCriteria(normalizedProduct, normalizedSearchCriteria))
                .sorted(
                        Comparator.comparingInt((NormalizedProduct normalizedProduct) -> relevanceCalculator(normalizedProduct, normalizedSearchCriteria))
                                .reversed()
                                .thenComparingDouble(NormalizedProduct::price)
                )
                .map(NormalizedProduct::originalProduct)
                .toList();
    }

    public void addProduct(Set<Product> products, Product product){
        validateProduct(product);

        products.remove(product);
        products.add(product);
    }

    public boolean keywordMatcher(NormalizedProduct product, String keyword){
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

        return keywordMatcherNormalized(product, keyword);
    }

    public boolean searchByCriteria(NormalizedProduct product, SearchCriteria searchCriteria){
        return keywordMatcherNormalized(product, searchCriteria.keyword) && ((searchCriteria.maxPrice !=null && product.price() <= searchCriteria.maxPrice) || searchCriteria.maxPrice ==null);
    }

    public int relevanceCalculator(NormalizedProduct product, SearchCriteria searchCriteria){
        /**
         * normalize the keyword
         * normalize the product name
         * count the number of matches (non-overlapping) of the keyword in the product name store in the score variable
         * each single match score is 10, total score from name match is 10 * keyword occurence capped by 50
         * if the score from name match reaches 50, move on to tag comparison
         * iterate through each tag, normalize the tag and count the number of match (non-overlapping)
         * each match from tag score is 5, total score from tags : 5 * keyword occurence capped by 30
         * the score from tag should be added to the score from the name
         * if the score from tag reaches 30, break the loop and return the total score.
         */
        String normalizedKeyword = searchCriteria.keyword;
        int totalScore = 0;

        int nameMatchCount = countNonOverlappingOccurrences(product.normalizedName(), normalizedKeyword, 5);
        totalScore += Math.min(nameMatchCount * 10, 50);

        if (product.normalizedTags() == null || product.normalizedTags().isEmpty()) {
            return totalScore;
        }

        int tagScore = 0;
        for (String normalizedTag : product.normalizedTags()) {
            int remainingTagMatches = (30 - tagScore) / 5;
            int tagMatchCount = countNonOverlappingOccurrences(normalizedTag, normalizedKeyword, remainingTagMatches);
            tagScore += tagMatchCount * 5;

            if (tagScore >= 30) {
                tagScore = 30;
                break;
            }
        }

        return totalScore + tagScore;
    }

    private boolean keywordMatcherNormalized(NormalizedProduct product, String normalizedKeyword) {
        if (normalizedKeyword == null || normalizedKeyword.isEmpty()) {
            return false;
        }

        if (product.normalizedName() != null && !product.normalizedName().isBlank() && product.normalizedName().contains(normalizedKeyword)) {
            return true;
        }

        if (product.normalizedTags() == null || product.normalizedTags().isEmpty()) {
            return false;
        }

        for (String normalizedTag : product.normalizedTags()) {
            if (normalizedTag != null && !normalizedTag.isBlank() && normalizedTag.contains(normalizedKeyword)) {
                return true;
            }
        }

        return false;
    }

    public NormalizedProduct normalizeProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("product cannot be null");
        }

        List<String> normalizedTags = product.tags() == null
                ? null
                : product.tags().stream()
                .map(this::normalize)
                .toList();

        return new NormalizedProduct(product, normalize(product.name()), normalizedTags);
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

    private void validateSearchCriteria(SearchCriteria searchCriteria) {
        if (searchCriteria == null) {
            throw new IllegalArgumentException("searchCriteria cannot be null");
        }

        if (searchCriteria.maxPrice != null) {
            if (searchCriteria.maxPrice < 0) {
                throw new IllegalArgumentException("maxPrice must be greater than or equal to 0");
            }

            if (hasMoreThanTwoDecimals(searchCriteria.maxPrice)) {
                throw new IllegalArgumentException("maxPrice cannot have more than two decimal places");
            }
        }
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

    private int countNonOverlappingOccurrences(String text, String keyword, int maxCount) {
        if (text == null || text.isBlank() || keyword == null || keyword.isBlank() || maxCount <= 0) {
            return 0;
        }

        int count = 0;
        int fromIndex = 0;

        while (true) {
            int matchIndex = text.indexOf(keyword, fromIndex);
            if (matchIndex < 0) {
                return count;
            }

            count++;
            if (count >= maxCount) {
                return count;
            }
            fromIndex = matchIndex + keyword.length();
        }
    }
}

class SearchCriteria{
    String keyword;
    Double maxPrice;
    //sorting defaulted to: relevance(number of keyword matches) desc + price asc order
    //relevance score calculation rules:
}

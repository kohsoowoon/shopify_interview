package shopify.product.search;

import java.util.List;
import java.util.Set;

public class SearchProducts {
    public List<Product> searchProducts(Set<Product> products, SearchCriteria searchCriteria){
        return null;
    }

    public void addProduct(Set<Product> products, Product product){

    }
}

class SearchCriteria{
    String keyword;
    Double maxPrice;
    //sorting defaulted to: relevance(number of keyword matches) desc + price asc order
    //relevance score calculation rules:
}

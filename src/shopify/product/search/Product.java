package shopify.product.search;

import java.util.List;
import java.util.Objects;

/***
 * shopify.product.search.Product record to represent a product with id, name, price and tags.
 * @param id : required
 * @param name : required > null/blank not allowed
 * @param price : required => 0
 * @param tags : optional > could be null, empty or blank as a List. Also each tag could be null/blank.
 */
public record Product (String id, String name, double price, List<String> tags){
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Product product = (Product) o;

        return Objects.equals(product.id, id);
    }

    @Override
    public int hashCode(){
        return Objects.hash(id);
    }
}

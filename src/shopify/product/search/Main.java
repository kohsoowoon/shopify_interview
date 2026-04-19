package shopify.product.search;

public class Main {
    public static void main(String[] args) {
        testAddProduct();
        testSearchProducts();
    }

    private static void testSearchProducts() {
        /**
         * Test cases for searchProducts method:
         * 1.
         */
    }

    private static void testAddProduct() {
        /**
         * Test cases for addProduct method:
         * 1. New product validation:
         *  - id cannot be null or blank, also has to be unique across the given list of products.
         *  - name cannot be null or blank. Will normalize it for search later.
         *  - price bigger or equal to 0, 2 digit decimal places.
         *  - tags: nullable, emptiable, blankable list, each tag could be also blankable : nothing to validate here
         *
         *  2. Add a valid product to a list
         *  - if no matching id, add the new product
         *  - if the id already exists, update the existing product with the new product data
         */
    }

}
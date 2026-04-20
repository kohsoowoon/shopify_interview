package shopify.shopping.cart;


public class ItemCount {
    private Item item;
    private int count;

    public ItemCount(Item item, int count) {
        this.item = item;
        this.count = count;
    }

    public Item getItem() {
        return item;
    }
    public int getCount(){
        return count;
    }
}

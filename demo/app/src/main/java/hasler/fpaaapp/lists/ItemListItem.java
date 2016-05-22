package hasler.fpaaapp.lists;

public class ItemListItem {
    private String itemTitle, itemValue;

    public String getTitle() {
        return itemTitle;
    }
    public String getValue() {
        return itemValue;
    }
    public void setTitle(Object title) {
        itemTitle = String.valueOf(title);
    }
    public void setValue(Object value) {
        itemValue = String.valueOf(value);
    }

    public ItemListItem(String title, Object value) {
        itemTitle = title;
        itemValue = String.valueOf(value);
    }
}

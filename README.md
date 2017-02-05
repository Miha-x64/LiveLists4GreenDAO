# LiveLists4GreenDAO
Helps in creating live, auto-updated RecyclerViews based on data from GreenDAO.

Example:
```java
recycler.setAdapter(
        new LiveAdapter<Item, ItemHolder>(app.getItemDao().queryBuilder().build(), dataLayer) {
            @Override public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new ItemHolder(getLayoutInflater()
                        .inflate(android.R.layout.simple_list_item_1, parent, false));
            }
        });
```

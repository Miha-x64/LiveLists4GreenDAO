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

Depends on another my projects, «Blitz!», which provides collections of longs.
Still not published on any Maven repo, so you may clone that
project in order to try out this one.
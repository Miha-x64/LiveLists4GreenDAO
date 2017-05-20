# LiveLists4GreenDAO
Helps in creating live, auto-updated RecyclerViews based on data from GreenDAO.

### What's new

* Using stable IDs

* New abstraction LiveList and its implementation GreenLiveList
* LiveAdapter accepts LiveList which encapsulates
both LiveDataLayer and Query;
* you can change query via LiveAdapter.changeList(LiveList),
argument must be of same type (GreenLiveList) and must be hosted on
the same data layer, only query may be changed;
* multi-thread crash fix;
* entities must implement new onDelete method which is expected to null
out their primary key;
* it's able to notify a data layer about any changes:
there are `saved()` and `removed()` methods in LiveDataLayer

Sample usage of LiveAdapter:
```java
recycler.setAdapter(
        new LiveAdapter<Item, ItemHolder>(new GreenLiveList(dataaLayer, app.getItemDao().queryBuilder().build())) {
            @Override public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new ItemHolder(getLayoutInflater()
                        .inflate(android.R.layout.simple_list_item_1, parent, false));
            }
        });
```

Depends on another my project, [Blitz!](https://github.com/Miha-x64/Blitz/),
which provides collections of longs.
Still not published on any Maven repo, so you may clone that
project in order to try out this one.

Adding via Gradle:
```
    compile project(':greenLiveLists') // GreenLiveList
    compile project(':liveListsAndroid') // LiveAdapter
```
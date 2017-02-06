package net.aquadc.greenreactive.example;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import net.aquadc.greenreactive.LiveAdapter;
import net.aquadc.greenreactive.LiveDataLayer;
import net.aquadc.greenreactive.R;

import org.greenrobot.greendao.query.Query;

import java.util.Date;

public final class MainActivity extends AppCompatActivity {

    final Handler handler = new Handler();
    LiveDataLayer<Item, Query<Item>> dataLayer;
    LiveDataLayer.SingleSubscriber<Item> subscriber;
    LiveAdapter<Item, ItemHolder> adapter;
    ItemDao dao;

    TextView zeroItem;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        ImageButton fab = (ImageButton) findViewById(R.id.fab);
        fab.setOnClickListener(clickListener);

        final App app = ((App) getApplication());
        LiveDataLayer<Item, Query<Item>> dataLayer = app.getItemLayer();
        this.dataLayer = dataLayer;
        Item item = dataLayer.get(1L);
        if (item == null) {
            item = new Item();
        }
        item.setText("hello");
        dataLayer.save(item);

        zeroItem = (TextView) findViewById(R.id.zeroItem);
        subscriber = new LiveDataLayer.SingleSubscriber<Item>() {
            @Override public void onChange(Item newT) {
                zeroItem.setText(newT.getText());
            }
        };

        RecyclerView recycler = (RecyclerView) findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        dao = app.getItemDao();
        adapter = new LiveAdapter<Item, ItemHolder>(
                dao.queryBuilder().orderAsc(ItemDao.Properties.Order).build(), dataLayer) {
            @Override
            public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new ItemHolder(getLayoutInflater()
                        .inflate(R.layout.simple_list_item_2, parent, false));
            }
        };
        recycler.setAdapter(adapter);
        recycler.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
    }

    private class ItemHolder extends LiveAdapter.ViewHolder<Item> implements View.OnClickListener {
        private final TextView text1;
        private final TextView text2;
        /*pkg*/ ItemHolder(View itemView) {
            super(itemView);
            text1 = (TextView) itemView.findViewById(android.R.id.text1);
            text2 = (TextView) itemView.findViewById(android.R.id.text2);
            itemView.setOnClickListener(this);
        }
        /*pkg*/ Item currentItem;
        @Override public void bind(Item item) {
            currentItem = item;
            text1.setText(item.getText());
        }
        @Override public void onClick(View v) {
            AlertDialog dialog = new AlertDialog.Builder(v.getContext())
                    .setTitle("Edit")
                    .setView(R.layout.item_edit)
                    .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Item current = currentItem;
                            Dialog di = (Dialog) dialog;
                            EditText text = (EditText) di.findViewById(R.id.text);
                            EditText order = (EditText) di.findViewById(R.id.order);
                            current.setText(text.getText().toString());

                            String orderStr = order.getText().toString();
                            current.setOrder(orderStr.isEmpty() ? 0 : Integer.parseInt(orderStr));
                            dataLayer.save(current);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .setNeutralButton("Delete", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dataLayer.remove(currentItem);
                        }
                    })
                    .show();

            String text = currentItem.getText();
            EditText et = (EditText) dialog.findViewById(R.id.text);
            et.setText(text);
            et.setSelection(text.length());
            EditText order = (EditText) dialog.findViewById(R.id.order);
            order.setText(Integer.toString(currentItem.getOrder()));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                LiveDataLayer<Item, Query<Item>> dataLayer = MainActivity.this.dataLayer;
                Item item = dataLayer.get(1L);
                if (item != null) { // fixme: will be null if enqueued by save() but not persisted yet
                    item.setText("now: " + new Date());
                    dataLayer.save(item);
                }

                handler.postDelayed(this, 1000);
            }
        }, 1000);

        dataLayer.subscribeOnSingle(1L, subscriber);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacksAndMessages(null);
        dataLayer.unsubscribe(subscriber);
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);

        SearchView searchView = null;
        if (searchItem != null) {
            searchView = (SearchView) searchItem.getActionView();
        }
        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    adapter.changeQuery(
                            dao.queryBuilder()
                                    .where(ItemDao.Properties.Text.like('%' + newText + '%'))
                                    .orderAsc(ItemDao.Properties.Order)
                                    .build());
                    return true;
                }
            });
        }
        return super.onCreateOptionsMenu(menu);
    }

    private final View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.fab:
                    final EditText et = new EditText(MainActivity.this);
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Add")
                            .setView(et)
                            .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dataLayer.save(new Item(null, et.getText().toString(), 0));
                                }
                            })
                            .show();
                    break;

                default:
                    throw new UnsupportedOperationException();
            }
        }
    };
}

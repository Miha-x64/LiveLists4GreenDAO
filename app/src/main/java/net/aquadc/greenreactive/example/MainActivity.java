package net.aquadc.greenreactive.example;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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

    TextView zeroItem;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        recycler.setAdapter(
                new LiveAdapter<Item, ItemHolder>(app.getItemDao().queryBuilder().build(), dataLayer) {
                    @Override public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                        return new ItemHolder(getLayoutInflater()
                                .inflate(R.layout.simple_list_item_2, parent, false));
                    }
                });
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
            Context ctx = v.getContext();
            final EditText et = new EditText(ctx);
            String text = currentItem.getText();
            et.setText(text);
            et.setSelection(text.length());
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Edit")
                    .setView(et)
                    .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Item current = currentItem;
                            current.setText(et.getText().toString());
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
                                    dataLayer.save(new Item(null, et.getText().toString()));
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

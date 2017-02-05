package net.aquadc.greenreactive.example;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;

import net.aquadc.greenreactive.LiveDataLayer;
import net.aquadc.greenreactive.GreenDataLayer;

import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.query.Query;

/**
 * Created by miha on 03.02.17
 */

public final class App extends Application {

    private LiveDataLayer<Item, Query<Item>> itemLayer;
    private ItemDao itemDao;

    @Override
    public void onCreate() {
        super.onCreate();

        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, "db");
        Database db = helper.getWritableDb();
        DaoSession daoSession = new DaoMaster(db).newSession();
        ItemDao itemDao = daoSession.getItemDao();
        itemLayer = new GreenDataLayer<>(itemDao, (SQLiteDatabase) db.getRawDatabase());
        this.itemDao = itemDao;
    }

    public LiveDataLayer<Item, Query<Item>> getItemLayer() {
        return itemLayer;
    }

    public ItemDao getItemDao() {
        return itemDao;
    }
}
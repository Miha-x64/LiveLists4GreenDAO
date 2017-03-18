package net.aquadc.livelists.example;

import android.app.Application;

import net.aquadc.livelists.greendao.GreenDataLayer;

import org.greenrobot.greendao.database.Database;

/**
 * Created by miha on 03.02.17
 */

public final class App extends Application {

    private GreenDataLayer<Item> itemLayer;
    private ItemDao itemDao;

    @Override
    public void onCreate() {
        super.onCreate();

        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, "db");
        Database db = helper.getWritableDb();
        DaoSession daoSession = new DaoMaster(db).newSession();
        ItemDao itemDao = daoSession.getItemDao();
        itemLayer = new GreenDataLayer<>(itemDao);
        this.itemDao = itemDao;
    }

    public GreenDataLayer<Item> getItemLayer() {
        return itemLayer;
    }

    public ItemDao getItemDao() {
        return itemDao;
    }
}

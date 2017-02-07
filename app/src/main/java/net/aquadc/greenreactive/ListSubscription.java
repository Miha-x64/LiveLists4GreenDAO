package net.aquadc.greenreactive;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import org.greenrobot.greendao.query.LazyList;
import org.greenrobot.greendao.query.Query;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.greenrobot.greendao.query.GreenLists$Internal$QuerySpy.getParameters;
import static org.greenrobot.greendao.query.GreenLists$Internal$QuerySpy.getSql;

/**
 * Created by miha on 04.02.17
 */

/*pkg*/ final class ListSubscription<T extends LiveDataLayer.WithId> {

    private final Handler handler;
    private final SQLiteDatabase db;
    /*pkg*/ final LiveDataLayer.BaseListSubscriber<T> subscriber;

    private Query<T> query;

    private LazyList<T> list;
    private long[] ids;

    /*pkg*/ ListSubscription(Handler handler, SQLiteDatabase db,
                             LiveDataLayer.BaseListSubscriber<T> subscriber, Query<T> query) {
        this.handler = handler;
        this.db = db;
        this.subscriber = subscriber;
        this.query = query;

        LazyList<T> list = query.listLazy();
        this.list = list;
        long[] ids = loadIds(db, query);
        this.ids = ids;

        List<T> uList = Collections.unmodifiableList(list);
        long[] uIds = ids.clone(); // fixme don't clone in some cases
        Set<Long> changes = Collections.emptySet();
        if (subscriber instanceof LiveDataLayer.ListSubscriberWithPayload) {
            LiveDataLayer.ListSubscriberWithPayload sub =
                    (LiveDataLayer.ListSubscriberWithPayload) subscriber;
            sub.onStructuralChange(uList, uIds, changes, sub.calculatePayload(uList, uIds, changes));
        } else if (subscriber instanceof LiveDataLayer.ListSubscriber) {
            ((LiveDataLayer.ListSubscriber<T>) subscriber).onStructuralChange(
                    uList, uIds, changes);
        }
    }

    @WorkerThread
    /*pkg*/ void dispatchUpdate(T newT) {
        if (containId(newT.getId())) {
            dispatchNonStructuralChange(newT.getId());
        }
    }

    @WorkerThread // null when changing query, will be replaced with Set<Long>
    /*pkg*/ boolean dispatchStructuralChange(@Nullable final Long idOfInsertedOrRemoved) {
        final LazyList<T> oldList = list;
        final LazyList<T> newList = query.listLazy();
        final long[] oldIds = ids;
        final long[] newIds = loadIds(db, query);

        int newSize = newIds.length;
        int oldSize = oldIds.length;

        if (BuildConfig.DEBUG) {
            if (newIds.length != newList.size() || oldIds.length != oldList.size()) {
                throw new AssertionError();
            }
        }

        if (newSize == oldSize) { // no insertions/deletions
            if (Arrays.equals(oldIds, newIds)) { // no moves
                newList.close();
                return false; // change not affected this query
            }
        }

        list = newList;
        ids = newIds;

        final List<T> uList = Collections.unmodifiableList(newList);
        final long[] uIds = newIds.clone(); // fixme
        final Set<Long> changed = idOfInsertedOrRemoved == null
                ? Collections.<Long>emptySet() : Collections.singleton(idOfInsertedOrRemoved);

        LiveDataLayer.BaseListSubscriber<T> sub = subscriber;
        final Object payload = sub instanceof LiveDataLayer.ListSubscriberWithPayload
                ? ((LiveDataLayer.ListSubscriberWithPayload) sub).calculatePayload(uList, newIds, changed)
                : null;
        handler.post(new Runnable() {
            @Override
            public void run() {
                LiveDataLayer.BaseListSubscriber<T> sub = subscriber;
                if (sub instanceof LiveDataLayer.ListSubscriberWithPayload) {
                    ((LiveDataLayer.ListSubscriberWithPayload) sub).onStructuralChange(uList, uIds, changed, payload);
                } else if (sub instanceof LiveDataLayer.ListSubscriber) {
                    ((LiveDataLayer.ListSubscriber) sub).onStructuralChange(uList, uIds, changed);
                }
                oldList.close();
            }
        });
        return true;
    }

    @WorkerThread
    private boolean containId(Long pk) {
        long id = pk/*.longValue()*/;
        for (long l : ids) {
            if (l == id) {
                return true;
            }
        }
        return false;
    }

    @WorkerThread
    private void dispatchNonStructuralChange(final Long idOfChanged) {
        // simple update can lead to move due to change of a value given query ordered by
        // todo: don't check for structural changes if a query has no ORDER BY
        if (!dispatchStructuralChange(idOfChanged)) {
            final LazyList<T> list = this.list;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    subscriber.onChange(list, idOfChanged == null
                            ? Collections.<Long>emptySet() : Collections.singleton(idOfChanged));
                }
            });
        }
    }

    /*pkg*/ void dispose() {
        handler.removeCallbacksAndMessages(null);
        list.close();
    }

    @WorkerThread
    /*pkg*/ void changeQuery(Query<T> newQuery) {
        this.query = newQuery;
        dispatchStructuralChange(null);
    }

    private static final long[] EMPTY_LONGS = {};
    private static final Pattern _FROM_ = Pattern.compile("\\Q FROM \\E");
    private static long[] loadIds(SQLiteDatabase db, Query<? extends LiveDataLayer.WithId> query) {
        // fixme: why getting ids is slower than using listLazy?
        long[] ids = null;
        if (BuildConfig.DEBUG) {
            // get IDs in awful way
            long nanos = System.nanoTime();
            LazyList<? extends LiveDataLayer.WithId> list = query.listLazyUncached();
            ids = new long[list.size()];
            for (int i = 0; i < list.size(); i++) {
                ids[i] = list.get(i).getId();
            }
            nanos = System.nanoTime() - nanos;
            System.out.println("Got IDs in awful way in " + (nanos/1000/1000.0) + " ms");
        }

        long nanos = System.nanoTime();
        //SELECT T."_id",T."TEXT",T."ORDER" FROM "ITEM" T  ORDER BY T.'ORDER' ASC
        // get IDs directly, by querying only IDs
        Cursor cur = db.rawQuery("SELECT T.\"_id\" FROM " + _FROM_.split(getSql(query), 2)[1], getParameters(query));
        if (!cur.moveToFirst()) {
            cur.close();
            return EMPTY_LONGS;
        }

        long[] array = new long[cur.getCount()];
        int pos = 0;
        do {
            array[pos] = cur.getLong(0);
            pos++;
        } while (cur.moveToNext());
        cur.close();

        if (BuildConfig.DEBUG) {
            nanos = System.nanoTime() - nanos;
            System.out.println("Got IDs directly in " + (nanos/1000/1000.0) + " ms");
            if (!Arrays.equals(ids, array)) {
                throw new AssertionError();
            }
        }

        return array;
    }
}

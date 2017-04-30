package net.aquadc.livelists.greendao;

import android.database.Cursor;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import net.aquadc.blitz.LongSet;
import net.aquadc.blitz.impl.ImmutableLongTreeSet;
import net.aquadc.livelists.LiveDataLayer;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.query.LazyList;
import org.greenrobot.greendao.query.Query;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.Collections.unmodifiableList;
import static org.greenrobot.greendao.query.GreenLists$Internal$QuerySpy.getParameters;
import static org.greenrobot.greendao.query.GreenLists$Internal$QuerySpy.getSql;

/**
 * Created by miha on 04.02.17
 */

/*pkg*/ final class ListSubscription<T extends LiveDataLayer.WithId> {

    private final Handler handler;
    private final AbstractDao<T, Long> dao;
    /*pkg*/ final LiveDataLayer.BaseListSubscriber<? super T> subscriber;

    private Query<T> query;
    private String idQuery;
    private String[] idQueryParams;
    private boolean orderedQuery;

    private LazyList<T> list;
    private long[] ids;

    /*pkg*/ ListSubscription(Handler handler, AbstractDao<T, Long> dao,
                             LiveDataLayer.BaseListSubscriber<? super T> subscriber, Query<T> query) {
        this.handler = handler;
        this.dao = dao;
        this.subscriber = subscriber;

        this.query = query;
        String sql = getSql(query);
        this.idQuery = "SELECT T.\"_id\" FROM " + _FROM_.split(sql, 2)[1];
        this.idQueryParams = getParameters(query);
        this.orderedQuery = sql.contains("ORDER BY");

        LazyList<T> list = query.listLazy();
        this.list = list;
        long[] ids = loadIds(query);
        this.ids = ids;

        List<T> uList = unmodifiableList(list);
        long[] uIds = ids.clone(); // fixme don't clone in some cases
        LongSet changes = ImmutableLongTreeSet.empty();
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
            dispatchNonStructuralChange(ImmutableLongTreeSet.singleton(newT.getId()));
        }
    }

    @WorkerThread
    /*pkg*/ boolean dispatchStructuralChange(@NonNull final LongSet idsOfChanged) {
        final LazyList<T> oldList = list;
        final LazyList<T> newList = query.forCurrentThread().listLazy();
        final long[] oldIds = ids;
        final long[] newIds = loadIds(query);

        int newSize = newIds.length;
        int oldSize = oldIds.length;

        if (BuildConfig.DEBUG) {
            if (newSize != newList.size() || oldSize != oldList.size()) {
                throw new AssertionError();
            }
        }

        list = newList;
        ids = newIds;

        if (newSize == oldSize) { // no insertions/deletions
            if (Arrays.equals(oldIds, newIds)) { // no moves
                oldList.close();
                return false; // change not affected this query order. But we've already updated list!
            }
        }

        final List<T> uList = unmodifiableList(newList);
        final long[] uIds = newIds.clone(); // fixme

        LiveDataLayer.BaseListSubscriber<? super T> sub = subscriber;
        final Object payload = sub instanceof LiveDataLayer.ListSubscriberWithPayload
                ? ((LiveDataLayer.ListSubscriberWithPayload) sub).calculatePayload(uList, newIds, idsOfChanged)
                : null;
        handler.post(new Runnable() {
            @Override
            public void run() {
                LiveDataLayer.BaseListSubscriber<? super T> sub = subscriber;
                if (sub instanceof LiveDataLayer.ListSubscriberWithPayload) {
                    ((LiveDataLayer.ListSubscriberWithPayload) sub).onStructuralChange(uList, uIds, idsOfChanged, payload);
                } else if (sub instanceof LiveDataLayer.ListSubscriber) {
                    ((LiveDataLayer.ListSubscriber) sub).onStructuralChange(uList, uIds, idsOfChanged);
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
    /*pkg*/ void dispatchNonStructuralChange(final LongSet idsOfChanged) {
        // simple update can lead to move due to change of a value given in query's 'ordered by'
        boolean structuralChangeDispatched = dispatchStructuralChange(idsOfChanged) && orderedQuery;
        if (!structuralChangeDispatched) {
            final LazyList<T> list = this.list;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    subscriber.onChange(list, idsOfChanged);
                }
            });
        }
    }

    /*pkg*/ void dispose() {
        handler.removeCallbacksAndMessages(null);
        list.close();
    }

    private static final Pattern _FROM_ = Pattern.compile("\\Q FROM \\E");
    @WorkerThread
    /*pkg*/ void changeQuery(Query<T> newQuery) {
        this.query = newQuery;
        String sql = getSql(newQuery);
        this.idQuery = "SELECT T.\"_id\" FROM " + _FROM_.split(sql, 2)[1];
        this.idQueryParams = getParameters(newQuery);
        this.orderedQuery = sql.contains("ORDER BY");
        dispatchStructuralChange(ImmutableLongTreeSet.empty());
    }

    private static final long[] EMPTY_LONGS = {};
    private long[] loadIds(Query<? extends LiveDataLayer.WithId> query) {
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
        // get IDs directly, by querying only IDs
        Cursor cur = dao.getDatabase().rawQuery(idQuery, idQueryParams);
        if (!cur.moveToFirst()) {
            cur.close();
            return EMPTY_LONGS;
        }

        int count = cur.getCount();
        long[] array = new long[count];
        for (int i = 0; i < count; i++) {
            if (!cur.moveToPosition(i)) {
                throw new AssertionError();
            }
            array[i] = cur.getLong(0);
        }
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

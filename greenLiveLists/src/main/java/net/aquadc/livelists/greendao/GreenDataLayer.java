package net.aquadc.livelists.greendao;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Pair;

import net.aquadc.livelists.LiveDataLayer;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.query.Query;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * An observer that is waiting for changes on a data set and ready to notify observers about them.
 */
public final class GreenDataLayer<T extends GreenDataLayer.WithId> implements LiveDataLayer<T, Query<T>> {

    private static final int SAVE = 0;
    private static final int REMOVE = 1;
    private static final int CHANGE = 2;

    /*pkg*/ static final HandlerThread handlerThread = new HandlerThread("GreenDataLayerThread", 2);
    static {
        handlerThread.start();
        // todo: start a thread with a first subscriber, stop after last unsubscriber
    }
    /*pkg*/ final Map<SingleSubscriber<T>, Pair<Long, Handler>> singleSubscriptions = new ConcurrentHashMap<>();
    /*pkg*/ final Map<BaseListSubscriber<T>, ListSubscription<T>> listSubscriptions = new ConcurrentHashMap<>();
    /*pkg*/ final Set<Long> enqueuedForRemoval = new CopyOnWriteArraySet<>();
    private final Handler handler;
    /*pkg*/ final AbstractDao<T, Long> dao;

    public GreenDataLayer(AbstractDao<T, Long> dao) {
        this.dao = dao;
        handler = new DataLayerHandler();
    }

    // simple proxy

    @Override @Nullable public T get(Long pk) {
        return dao.load(required(pk, "pk"));
    }

    @Override public void save(T t) {
        handler.dispatchMessage(handler.obtainMessage(SAVE, required(t, "object to save")));
    }

    @Override public void remove(T t) {
        Long id = required(t, "object to remove").getId();
        for (Map.Entry<SingleSubscriber<T>, Pair<Long, Handler>> entry : new HashMap<>(singleSubscriptions).entrySet()) {
            if (entry.getValue().first.equals(id)) {
                throw new IllegalStateException("Can't remove " + t + ": it is currently being observed by " +
                        entry.getKey() + ". Unsubscribe first.");
            }
        }
        if (!enqueuedForRemoval.add(id)) {
            throw new IllegalStateException(t + " is already enqueued for removal.");
        }
        handler.dispatchMessage(handler.obtainMessage(REMOVE, t.getId()));
    }

    // single record

    @Override
    public void subscribeOnSingle(Long pk, SingleSubscriber<T> subscriber) {
        required(pk, "pk");
        required(subscriber, "subscriber");
        if (enqueuedForRemoval.contains(pk)) {
            throw new IllegalStateException("Item with PK " + pk + " is enqueued for removal.");
        }

        T t = dao.load(pk);
        if (t == null) {
            throw new NoSuchElementException("Can's subscribe on element with pk " + pk + ": no such element.");
        }

        subscriber.onChange(t);
        if (singleSubscriptions.put(subscriber, new Pair<>(pk, new Handler())) != null) {
            throw new IllegalStateException(subscriber + " is already subscribed."); // broken state
        }
    }
    @Override
    public void unsubscribe(SingleSubscriber<T> subscriber) {
        Pair<Long, Handler> data = singleSubscriptions.remove(required(subscriber, "subscriber"));
        if (data == null) {
            throw new NoSuchElementException(subscriber + " is not subscribed.");
        }
        data.second.removeCallbacksAndMessages(null);
    }

    // list

    @Override
    public void subscribeOnList(Query<T> query, BaseListSubscriber<T> subscriber) {
        ListSubscription<T> sub = new ListSubscription<>(
                new Handler(), dao, required(subscriber, "subscriber"), query);
        if (listSubscriptions.put(subscriber, sub) != null) {
            throw new IllegalStateException(subscriber + " is already subscribed."); // broken state
        }
    }

    @Override
    public void changeQuery(BaseListSubscriber<T> subscriber, Query<T> newQuery) {
        ListSubscription<T> sub = listSubscriptions.get(required(subscriber, "subscriber"));
        if (sub == null) {
            throw new IllegalStateException(subscriber + " is not subscribed");
        }
        handler.dispatchMessage(handler.obtainMessage(CHANGE, new Pair<>(sub, required(newQuery, "new query"))));
    }

    @Override
    public void unsubscribe(BaseListSubscriber<T> subscriber) {
        ListSubscription sub = listSubscriptions.remove(subscriber);
        if (sub == null) {
            throw new NoSuchElementException(subscriber + " is not subscribed.");
        }
        sub.dispose();
    }

    // handler

    /*pkg*/ static final int INSERTION = 0;
    /*pkg*/ static final int UPDATE = 1;
    /*pkg*/ static final int REMOVAL = 2;

    @IntDef( { INSERTION, UPDATE, REMOVAL } )
    @Retention(RetentionPolicy.SOURCE)
    /*pkg*/ @interface SingleChange { }

    @SuppressLint("HandlerLeak") // whole DataLayer is long lived, and should die along with this Handler
    private class DataLayerHandler extends Handler {
        DataLayerHandler() {
            super(handlerThread.getLooper());
        }
        @Override public void handleMessage(Message msg) {
            switch (msg.what) {
                case SAVE: {
                    T t = (T) msg.obj;
                    boolean insert = t.getId() == null;
                    dao.save(t);
                    dispatchSingleChange(
                            insert ? INSERTION : UPDATE,
                            insert ? t.getId() : t);
                    break;
                }
                case REMOVE: {
                    Long key = (Long) msg.obj;
                    dao.deleteByKey(key);
                    if (!enqueuedForRemoval.remove(key)) {
                        throw new IllegalStateException(
                                "enqueuedForRemoval does not contain PK of removed object: " + key);
                    }
                    dispatchSingleChange(REMOVAL, key);
                    break;
                }
                case CHANGE: {
                    Pair<ListSubscription<T>, Query<T>> pair = (Pair<ListSubscription<T>, Query<T>>) msg.obj;
                    pair.first.changeQuery(pair.second);
                    break;
                }
                default:
                    throw new UnsupportedOperationException();
            }
        }
        @WorkerThread
        private void dispatchSingleChange(@SingleChange int kind, Object payload) {
            if (kind == UPDATE) {
                final T newT = (T) payload;
                Long id = newT.getId();
                // notify items that listen for single changes
                for (final Map.Entry<SingleSubscriber<T>, Pair<Long, Handler>> entry : new HashMap<>(singleSubscriptions).entrySet()) {
                    Pair<Long, Handler> val = entry.getValue();
                    final SingleSubscriber<T> subscriber = entry.getKey();
                    if (val.first.equals(id)) {
                        val.second.post(new Runnable() {
                            @Override
                            public void run() {
                                subscriber.onChange(newT);
                            }
                        });
                    }
                }
            }

            for (final ListSubscription<T> sub: new HashMap<>(listSubscriptions).values()) {
                switch (kind) {
                    case INSERTION:
                        sub.dispatchStructuralChange((Long) payload);
                        break;

                    case UPDATE:
                        sub.dispatchUpdate((T) payload);
                        break;

                    case REMOVAL:
                        sub.dispatchStructuralChange((Long) payload);
                        break;

                    default:
                        throw new UnsupportedOperationException();
                }
            }
        }
    }

    // util

    private static <T> T required(T whatever, String name) {
        if (whatever == null) {
            throw new NullPointerException(name + " required, got null");
        }
        return whatever;
    }
}

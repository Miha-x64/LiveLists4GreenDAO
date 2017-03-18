package net.aquadc.livelists;

import net.aquadc.blitz.LongSet;

import java.util.List;

/**
 * Created by miha on 03.02.17
 */
public interface LiveDataLayer<T extends LiveDataLayer.WithId, QUERY> {

    T get(Long id);
    void save(T t);
    void remove(T t);

    void subscribeOnSingle(Long pk, SingleSubscriber<T> subscriber);
    void unsubscribe(SingleSubscriber<T> subscriber);

    List<T> snapshot(QUERY query);
    int size(QUERY query);

    void subscribeOnList(QUERY query, BaseListSubscriber<? super T> subscriber);
    void unsubscribe(BaseListSubscriber<? super T> subscriber);

    void saved(T t);
    void saved(Long id);
    void saved(LongSet t);
    void removed(Long id);
    void removed(LongSet ids);

    interface SingleSubscriber<T> {
        /**
         * An object has changed.
         * @param newT    updated object
         */
        void onChange(T newT);
    }

    interface ListSubscriber<T> extends BaseListSubscriber<T> {
        /**
         * There was a structural change: at least one item was inserted or removed.
         * @param newList           list containing new items
         * @param newIds            IDs of all items (to detect structural changes)
         * @param changedItemIds    IDs of changed items
         */
        void onStructuralChange(List<T> newList, long[] newIds, LongSet changedItemIds);
    }
    interface ListSubscriberWithPayload<T, PL> extends BaseListSubscriber<T> {
        void onStructuralChange(List<T> newList, long[] newIds, LongSet changedItemIds, PL payload);
        PL calculatePayload(List<T> newList, long[] newIds, LongSet changedItemIds);
    }
    interface BaseListSubscriber<T> {
        /**
         * Items with given IDs were changed. There were no insertions or removals.
         * @param newList           list containing new items
         * @param changedItemIds    IDs of changed items
         */
        void onChange(List<? extends T> newList, LongSet changedItemIds);
    }

    interface WithId {
        Long getId();
        void onDelete(); // this entity was deleted, null out id
    }

}

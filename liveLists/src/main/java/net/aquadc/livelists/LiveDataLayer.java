package net.aquadc.livelists;

import java.util.List;
import net.aquadc.blitz.LongSet;

/**
 * Created by miha on 03.02.17
 */
public interface LiveDataLayer<T extends LiveDataLayer.WithId, QUERY> {

    T get(Long id);
    void save(T t);
    void remove(T t);

    void subscribeOnSingle(Long pk, SingleSubscriber<T> subscriber);
    void unsubscribe(SingleSubscriber<T> subscriber);

    void subscribeOnList(QUERY query, BaseListSubscriber<T> subscriber);
    void unsubscribe(BaseListSubscriber<T> subscriber);
    void changeQuery(BaseListSubscriber<T> subscriber, QUERY newQuery); // todo document in russian article

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
        void onChange(List<T> newList, LongSet changedItemIds);
    }

    interface WithId {
        Long getId();
    }

}

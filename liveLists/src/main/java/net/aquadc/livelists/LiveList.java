package net.aquadc.livelists;

import java.util.List;

/**
 * Created by miha on 20.02.17
 */

public interface LiveList<T extends LiveDataLayer.WithId> {
    void subscribeOnList(LiveDataLayer.BaseListSubscriber<? super T> subscriber);
    void unsubscribe(LiveDataLayer.BaseListSubscriber<? super T> subscriber);

    // unsubscribe from this, subscribe to another, notify about a change
    void moveTo(LiveDataLayer.BaseListSubscriber<? super T> subscriber, LiveList<?> another);
    List<T> snapshot();
    int size();
}

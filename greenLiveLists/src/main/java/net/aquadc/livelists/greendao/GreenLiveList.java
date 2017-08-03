package net.aquadc.livelists.greendao;

import net.aquadc.livelists.LiveDataLayer;
import net.aquadc.livelists.LiveList;

import org.greenrobot.greendao.query.Query;

import java.util.List;

import static net.aquadc.livelists.greendao.GreenDataLayer.required;

/**
 * Created by miha on 07.03.17
 */

public final class GreenLiveList<T extends LiveDataLayer.WithId> implements LiveList<T> {

    private final GreenDataLayer<T> layer;
    private final Query<T> query;

    public GreenLiveList(GreenDataLayer<T> layer, Query<T> query) {
        this.layer = layer;
        this.query = query;
    }

    @Override public void subscribeOnList(LiveDataLayer.BaseListSubscriber<? super T> subscriber) {
        layer.subscribeOnList(query, subscriber);
    }

    @Override public void unsubscribe(LiveDataLayer.BaseListSubscriber<? super T> subscriber) {
        layer.unsubscribe(subscriber);
    }

    @Override public List<? extends T> snapshot() {
        return layer.snapshot(query);
    }

    @Override public int size() {
        return layer.size(query);
    }

    @Override
    public void moveTo(LiveDataLayer.BaseListSubscriber<? super T> subscriber, LiveList<?> another) {
        required(subscriber, "subscriber");
        if (!(another instanceof GreenLiveList)) {
            throw new IllegalArgumentException("Can move only to a LiveList of the same type. " +
                    "This is GreenLiveList, another is " + (another == null ? "null" : another.getClass().getSimpleName()));
        }
        GreenLiveList<? extends T> anotherGreen = (GreenLiveList<? extends T>) another;
        if (layer != anotherGreen.layer) {
            throw new IllegalArgumentException("Can move only to a LiveList hosted on the same GreenDataLayer. " +
                    "this.layer = " + layer + ", another.layer = " + anotherGreen.layer);
        }
        layer.changeQuery(subscriber, anotherGreen.query);
    }
}

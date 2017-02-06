package net.aquadc.greenreactive.example;

import net.aquadc.greenreactive.GreenDataLayer;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

/**
 * Created by miha on 03.02.17
 */
@Entity
public final class Item implements GreenDataLayer.WithId {

    @Id
    private Long id;

    private String text;

    private int order;

    @Generated(hash = 490057792)
    public Item(Long id, String text, int order) {
        this.id = id;
        this.text = text;
        this.order = order;
    }

    @Generated(hash = 1470900980)
    public Item() {
    }

    @Override
    public Long getId() {
        return this.id;
    }

    /*pkg*/ void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return this.text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "Item{#" + id + ", text=" + text + '}';
    }

    public int getOrder() {
        return this.order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}

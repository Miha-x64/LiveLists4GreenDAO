package net.aquadc.livelists.greendao;

import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import org.greenrobot.greendao.query.Query;

import java.util.List;
import net.aquadc.blitz.LongSet;
import net.aquadc.livelists.LiveDataLayer;

/**
 * Created by miha on 05.02.17
 */

public abstract class LiveAdapter<MDL extends LiveDataLayer.WithId, VH extends LiveAdapter.ViewHolder<MDL>>
        extends RecyclerView.Adapter<VH> {

    private final LiveDataLayer<MDL, Query<MDL>> layer;

    private Query<MDL> query;

    protected LiveAdapter(Query<MDL> query, LiveDataLayer<MDL, Query<MDL>> layer) {
        this.query = query;
        this.layer = layer;
    }

    /*pkg*/ List<MDL> list;
    /*pkg*/ long[] ids;

    private final LiveDataLayer.ListSubscriberWithPayload<MDL, DiffUtil.DiffResult> subscriber =
            new LiveDataLayer.ListSubscriberWithPayload<MDL, DiffUtil.DiffResult>() {

                @Override
                public DiffUtil.DiffResult calculatePayload(final List<MDL> newList, final long[] newIds, final LongSet changedItemIds) {
                    final List<MDL> oldList = list;
                    if (oldList == null) {
                        return null;
                    }
                    final long[] oldIds = ids;

                    return DiffUtil.calculateDiff(new DiffUtil.Callback() {
                        @Override public int getOldListSize() { return oldList.size(); }
                        @Override public int getNewListSize() { return newList.size(); }
                        @Override public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                            return oldIds[oldItemPosition] == newIds[newItemPosition];
                        }
                        @Override public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                            return !changedItemIds.contains(newIds[newItemPosition]);
                        }
                    });
                }

                @Override
                public void onStructuralChange(List<MDL> newList, long[] newIds, LongSet changedItemIds, DiffUtil.DiffResult diff) {
                    list = newList;
                    ids = newIds;
                    if (diff != null) {
                        diff.dispatchUpdatesTo(LiveAdapter.this);
                    }
                }

                @Override
                public void onChange(List<MDL> newList, LongSet changedItemIds) {
                    list = newList;

                    int changed = 0;
                    for (int i = 0, size = newList.size(); i < size; i++) {
                        if (changedItemIds.contains(newList.get(i).getId())) {
                            notifyItemChanged(i);
                            changed++;
                        }
                    }

                    if (changed != changedItemIds.size()) {
                        throw new IllegalStateException("changedItemIds has size of " + changedItemIds.size() +
                                " but we changed only " + changed);
                    }
                }
            };

    @Override
    public void onBindViewHolder(VH holder, int position) {
        holder.bind(list.get(position));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }


    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        layer.subscribeOnList(query, subscriber);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        layer.unsubscribe(subscriber);
    }

    public void changeQuery(Query<MDL> newQuery) {
        this.query = newQuery;
        layer.changeQuery(subscriber, newQuery);
    }


    public abstract static class ViewHolder<MDL> extends RecyclerView.ViewHolder {
        protected ViewHolder(View itemView) {
            super(itemView);
        }
        public abstract void bind(MDL model);
    }
}

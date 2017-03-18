package net.aquadc.livelists.greendao;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import net.aquadc.blitz.LongSet;
import net.aquadc.livelists.LiveDataLayer;
import net.aquadc.livelists.LiveList;

import java.util.List;

import static net.aquadc.livelists.greendao.GreenDataLayer.required;

/**
 * Created by miha on 05.02.17
 */

public abstract class LiveAdapter<
        MDL/* extends LiveDataLayer.WithId*/,
        VH extends LiveAdapter.ViewHolder<MDL>>
        extends RecyclerView.Adapter<VH> {

    @Nullable private LiveList<? extends MDL> liveList;
    @Nullable private final LiveDataLayer.ListSubscriberWithPayload<MDL, DiffUtil.DiffResult> subscriber;

    protected LiveAdapter(LiveList<? extends MDL> _list) {
        this.liveList = required(_list, "list");
        this.subscriber = new LiveDataLayer.ListSubscriberWithPayload<MDL, DiffUtil.DiffResult>() {

            @Override public DiffUtil.DiffResult calculatePayload(
                    final List<MDL> newList, final long[] newIds, final LongSet changedItemIds) {
                final List<? extends MDL> oldList = list;
                if (oldList == null) {
                    return null;
                }
                final long[] oldIds = ids;

                return DiffUtil.calculateDiff(
                        new DiffUtilCallback<>(oldList, newList, oldIds, newIds, changedItemIds));
            }
            @Override public void onStructuralChange(
                    List<MDL> newList, long[] newIds, LongSet changedItemIds, DiffUtil.DiffResult diff) {
                list = newList;
                ids = newIds;
                if (diff != null) {
                    int scrollX = recycler.getScrollX();
                    int scrollY = recycler.getScrollY();
                    diff.dispatchUpdatesTo(LiveAdapter.this);
                    recycler.scrollTo(scrollX, scrollY);
                }
            }
            @Override public void onChange(List<? extends MDL> newList, LongSet changedItemIds) {
                list = newList;

                int changed = 0;
                for (int i = 0, size = newList.size(); i < size; i++) {
                    if (changedItemIds.contains(((LiveDataLayer.WithId) newList.get(i)).getId())) {
                        //                       ^ the only place where checkcast to WithId used
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
    }

    protected LiveAdapter(List<? extends MDL> list) {
        // a way to use this adapter like an ordinary ListAdapter
        this.liveList = null;
        this.subscriber = null;

        this.list = list;
    }

    /*pkg*/ List<? extends MDL> list;
    /*pkg*/ long[] ids;
    /*pkg*/ RecyclerView recycler;

    private static final class DiffUtilCallback<MDL> extends DiffUtil.Callback {
        private final List<? extends MDL> oldList;
        private final List<? extends MDL> newList;
        private final long[] oldIds;
        private final long[] newIds;
        private final LongSet changedItemIds;
        /*pkg*/ DiffUtilCallback(
                List<? extends MDL> oldList, List<? extends MDL> newList,
                long[] oldIds, long[] newIds, LongSet changedItemIds) {
            this.oldList = oldList;
            this.newList = newList;
            this.oldIds = oldIds;
            this.newIds = newIds;
            this.changedItemIds = changedItemIds;
        }
        @Override public int getOldListSize() { return oldList.size(); }
        @Override public int getNewListSize() { return newList.size(); }
        @Override public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldIds[oldItemPosition] == newIds[newItemPosition];
        }
        @Override public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return !changedItemIds.contains(newIds[newItemPosition]);
        }
    }

    @Override public void onBindViewHolder(VH holder, int position) {
        holder.bindInternal(list.get(position));
    }

    @Override public int getItemViewType(int position) {
        return getItemViewType(list.get(position));
    }

    protected int getItemViewType(MDL model) {
        return 0;
    }

    @Override public int getItemCount() {
        return list.size();
    }


    @Override public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        if (recycler != null) throw new IllegalStateException("already attached");
        this.recycler = recyclerView;

        if (liveList != null) liveList.subscribeOnList(subscriber);
//        layer.subscribeOnList(query, subscriber);
    }

    @Override public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        if (this.recycler == null) throw new IllegalStateException("already detached");
        this.recycler = null;
        if (liveList != null) liveList.unsubscribe(subscriber);
//        layer.unsubscribe(subscriber);
    }

    public void changeList(LiveList<? extends MDL> newList) {
        if (liveList == null) {
            throw new NullPointerException("Can move to another list only if in reactive mode.");
        }
        liveList.moveTo(subscriber, required(newList, "newList"));
        this.liveList = newList;
    }

    protected void onItemClick(MDL model, int adapterPosition) {}

    public abstract static class ViewHolder<MDL> extends RecyclerView.ViewHolder {
        protected ViewHolder(View itemView) {
            super(itemView);
        }
        /*pkg*/ void bindInternal(MDL model) {
            bind(model);
        }
        protected abstract void bind(@NonNull MDL model);

        /*pkg*/ void onRecycleInternal() {
            onRecycle();
        }
        protected void onRecycle() {}
    }

    public abstract static class ClickableViewHolder<MDL> extends ViewHolder<MDL> {
        public ClickableViewHolder(final LiveAdapter<MDL, ?> adapter, View itemView) {
            super(itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MDL model = ClickableViewHolder.this.model;
                    if (model == null) {
                        throw new IllegalStateException("click on detached item view");
                    }

                    adapter.onItemClick(model, getAdapterPosition());
                }
            });
            itemView.setClickable(true);
            itemView.setFocusable(true);
        }
        /*pkg*/ MDL model;
        @Override /*pkg*/ void bindInternal(MDL model) {
            this.model = model;
            bind(model);
        }
        @Override /*pkg*/ void onRecycleInternal() {
            onRecycle();
            model = null;
        }
    }
}

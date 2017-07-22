package net.aquadc.livelists.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import net.aquadc.blitz.LongSet;
import net.aquadc.livelists.LiveDataLayer;
import net.aquadc.livelists.LiveList;

import java.util.List;

/**
 * Created by miha on 05.02.17
 */

public abstract class LiveAdapter<
        MDL/* extends LiveDataLayer.WithId*/>
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

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

                if (logUpdates) {
                    Log.d(LiveAdapter.this.toString(), "calculatePayload: " + changedItemIds);
                }

                return DiffUtil.calculateDiff(
                        new DiffUtilCallback<>(oldList, newList, oldIds, newIds, changedItemIds));
            }
            @Override public void onStructuralChange(
                    List<MDL> newList, long[] newIds, LongSet changedItemIds, DiffUtil.DiffResult diff) {

                list = newList;
                ids = newIds;

                if (diff == null) {
                    notifyItemRangeInserted(0, newIds.length);
                } else {
                    final RecyclerView.LayoutManager lm = recycler.getLayoutManager();

                    int pos = -1;
                    if (lm instanceof LinearLayoutManager) {
                        pos = ((LinearLayoutManager) lm).findFirstVisibleItemPosition();
                    } else {
                        Log.e("LiveAdapter", "saving scroll position for " +
                                lm.getClass().getSimpleName() + " is not supported.");
                    }

                    diff.dispatchUpdatesTo(LiveAdapter.this);

                    if (pos != -1) {
                        recycler.scrollToPosition(pos);
                    }
                }

                if (logUpdates) {
                    Log.d(LiveAdapter.this.toString(), "onStructuralChange: " + changedItemIds + ", " + diff);
                }
            }
            @Override public void onChange(List<? extends MDL> newList, LongSet changedItemIds) {
                list = newList;

                int changed = 0;
                for (int i = 0, size = newList.size(); i < size; i++) {
                    // fixme: looks ugly & inefficient, it's better to iterate long[] ids
                    if (changedItemIds.contains(((LiveDataLayer.WithId) newList.get(i)).getId())) {
                        //                       ^ the only place where checkcast to WithId used
                        notifyItemChanged(i);
                        changed++;
                    }
                }
                if (changed != changedItemIds.size()) {
                    Log.e("LiveAdapter", "onChange: changedItemIds has size of " +
                            changedItemIds.size() + " but we were able to notify only " + changed +
                            " items. It's OK only if updated list part & a part being observed" +
                            " are differ.");
                }

                if (logUpdates) {
                    Log.d(LiveAdapter.this.toString(), "onChange: " + changedItemIds);
                }
            }
        };
        super.setHasStableIds(true);
    }

    /**
     * A way to use this adapter like an ordinary ListAdapter.
     * @param list data
     */
    protected LiveAdapter(List<? extends MDL> list) {
        this.liveList = null;
        this.subscriber = null;

        this.list = list;
        super.setHasStableIds(true);
    }

    @Deprecated @Override public final void setHasStableIds(boolean hasStableIds) {
        throw new UnsupportedOperationException("Always using stable IDs.");
    }

    @Override public long getItemId(int position) {
        return ids == null ? position : ids[position];
    }

    /**
     * A list with actual data. Gets filled in onChange (in live mode) or from constructor (ListAdapter mode).
     */
    /*pkg*/ List<? extends MDL> list;
    /**
     * IDs, which are required for diff computation.
     * Gets filled in onChange along with {@link #list} field, {@code null} in ListAdapter mode.
     * Used also as 'stable ids'.
     */
    /*pkg*/ long[] ids;

    /*pkg*/ RecyclerView recycler;

    private boolean logUpdates = false;

    protected void logUpdates() {
        Log.d(toString(), "updates logging enabled");
        logUpdates = true;
    }

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

    @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        // I could extend `ViewHolder<MDL>`, but this would prohibit use of ordinary adapters.
        ((ViewHolder<MDL>) holder).bindInternal(list.get(position));
    }

    @Override public int getItemViewType(int position) {
        return getItemViewType(list.get(position));
    }

    protected int getItemViewType(@NonNull MDL model) {
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

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        ((ViewHolder) holder).onRecycleInternal();
    }

    public void changeList(LiveList<? extends MDL> newList) {
        if (liveList == null) {
            throw new IllegalStateException("Can move to another live list only if in live mode.");
        }
        liveList.moveTo(subscriber, required(newList, "newList"));
        this.liveList = newList;
    }

    public void changeList(List<? extends MDL> newList) { // just like changing adapter
        if (liveList != null) {
            throw new IllegalStateException("Can move to another list only if in list mode.");
        }
        this.list = newList;
        notifyDataSetChanged();
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
        public ClickableViewHolder(final LiveAdapter<MDL> adapter, View itemView) {
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
            super.bindInternal(model);
        }
        @Override /*pkg*/ void onRecycleInternal() { // fixme: unused!!
            onRecycle();
            model = null;
        }
    }

    /*pkg*/ static <T> T required(T whatever, String name) {
        if (whatever == null) throw new NullPointerException(name + " required, got null");
        return whatever;
    }
}

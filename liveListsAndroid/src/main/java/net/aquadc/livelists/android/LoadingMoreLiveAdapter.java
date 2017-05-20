package net.aquadc.livelists.android;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import net.aquadc.livelists.LiveList;

import java.util.List;

/**
 * Created by miha on 09.04.17, moved here on 20.05.17
 */
public abstract class LoadingMoreLiveAdapter<MDL, VH extends LiveAdapter.ViewHolder<MDL>>
        extends LiveAdapter<MDL, RecyclerView.ViewHolder> {

    public static final int LOADING_ITEM_VIEW_TYPE = 2_147_483_647;
    public static final int LOADING_ITEM_ID = 2_147_483_647;

    protected LoadingMoreLiveAdapter(LiveList<? extends MDL> list) { super(list); }

    protected LoadingMoreLiveAdapter(List<? extends MDL> list) { super(list); }

    private boolean loadingMore;

    public void setLoadingMore(boolean loadingMore) {
        if (this.loadingMore != loadingMore) {
            this.loadingMore = loadingMore;
            if (loadingMore) notifyItemInserted(super.getItemCount());
            else notifyItemRemoved(super.getItemCount());
        }
    }

    @Override public int getItemCount() {
        return super.getItemCount() + (loadingMore ? 1 : 0);
    }

    @Override
    public long getItemId(int position) {
        return loadingMore && position == super.getItemCount() ? LOADING_ITEM_ID : super.getItemId(position);
    }

    @Override public int getItemViewType(int position) {
        if (position == super.getItemCount()) return LOADING_ITEM_VIEW_TYPE;
        else return super.getItemViewType(position);
    }

    @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == LOADING_ITEM_VIEW_TYPE) return createLoadingViewHolder(parent);
        else return createMdlViewHolder(parent, viewType);
    }

    protected abstract RecyclerView.ViewHolder createLoadingViewHolder(ViewGroup parent);
    protected abstract VH createMdlViewHolder(ViewGroup parent, int viewType);

    @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == LOADING_ITEM_VIEW_TYPE) return; // no binding for loading VH
        super.onBindViewHolder(holder, position);
    }
}

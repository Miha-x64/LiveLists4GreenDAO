package net.aquadc.livelists.android;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ViewGroup;
import net.aquadc.livelists.LiveList;

import java.util.List;

/**
 * Created by miha on 09.04.17, moved here on 20.05.17
 */
public abstract class LoadingMoreLiveAdapter<MDL>
        extends LiveAdapter<MDL> {

    public static final int LOADING_ITEM_VIEW_TYPE = 2_147_483_647;
    public static final int LOADING_ITEM_ID = 2_147_483_647;

    protected LoadingMoreLiveAdapter(LiveList<? extends MDL> list) { super(list); }

    protected LoadingMoreLiveAdapter(List<? extends MDL> list) { super(list); }

    private boolean loadingMore;

    /**
     * Have you ever met 'IllegalStateException: Cannot call this method while RecyclerView is computing a layout
     * or scrolling'? Use this method instead of {@link #setLoadingMore} then.
     * @param loadingMore whether it loading more or not
     */
    public final void postLoadingMore(boolean loadingMore) {
        if (this.loadingMore != loadingMore) {
            if (loadingMoreCaller == null) {
                loadingMoreCaller = new LoadingMoreCaller();
            }
            loadingMoreCaller.loadingMore = loadingMore;
            recycler.post(loadingMoreCaller);
        }
    }
    private LoadingMoreCaller loadingMoreCaller; // this object is being reused every time one calls [postLoadingMore].
    private final /*inner*/ class LoadingMoreCaller implements Runnable {
        /*pkg*/ boolean loadingMore;

        @Override public void run() {
            setLoadingMore(loadingMore);
        }
    }

    public final void setLoadingMore(boolean loadingMore) {
        if (isLoggingEnabled()) Log.d(toString(), "setLoadingMore: " + this.loadingMore + " -> " + loadingMore);
        if (this.loadingMore != loadingMore) {
            this.loadingMore = loadingMore;
            if (loadingMore) notifyItemInserted(super.getItemCount());
            else notifyItemRemoved(super.getItemCount());
        }
    }

    @Override public int getItemCount() {
        int actualItemCount = super.getItemCount();
        if (isLoggingEnabled()) Log.d(toString(), "getItemCount: " + actualItemCount + " + " + loadingMore);
        return actualItemCount + (loadingMore ? 1 : 0);
    }

    @Override
    public long getItemId(int position) {
        boolean isLoadingIndicator = loadingMore && position == super.getItemCount();
        if (isLoggingEnabled()) {
            Log.d(toString(), "getItemId: item at " + position + " is " +
                    (isLoadingIndicator ? "loading indicator" : "#" + super.getItemId(position)));
        }
        return isLoadingIndicator ? LOADING_ITEM_ID : super.getItemId(position);
    }

    @Override public int getItemViewType(int position) {
        boolean isLoadingIndicator = loadingMore && position == super.getItemCount();
        if (isLoggingEnabled()) {
            Log.d(toString(), "getItemViewType: item at " + position +
                    (isLoadingIndicator ? " is loading indicator" : " has type " + super.getItemViewType(position)));
        }

        if (isLoadingIndicator) return LOADING_ITEM_VIEW_TYPE;
        else return super.getItemViewType(position);
    }

    @Override public final RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == LOADING_ITEM_VIEW_TYPE) return createLoadingViewHolder(parent);
        else return createMdlViewHolder(parent, viewType);
    }

    protected abstract RecyclerView.ViewHolder createLoadingViewHolder(@NonNull ViewGroup parent);
    protected abstract LiveAdapter.ViewHolder<MDL> createMdlViewHolder(@NonNull ViewGroup parent, int viewType);

    @Override public final void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        boolean isLoadingIndicator = holder.getItemViewType() == LOADING_ITEM_VIEW_TYPE;

        if (isLoggingEnabled()) {
            Log.d(toString(), "onBindViewHolder: binding " +
                    (isLoadingIndicator ? "loading indicator" : holder.getClass().getSimpleName()) + " at " + position);
        }

        if (isLoadingIndicator) return; // no binding for loading VH
        super.onBindViewHolder(holder, position);
    }

    @Override public void onViewRecycled(RecyclerView.ViewHolder holder) {
        boolean isLoadingIndicator = holder.getItemViewType() == LOADING_ITEM_VIEW_TYPE;
        if (isLoggingEnabled()) {
            Log.d(toString(),
                    (isLoadingIndicator ? "loading indicator" : "holder with type #" + holder.getItemViewType()) +
                            " has been recycled");
        }
        if (isLoadingIndicator) return;
        super.onViewRecycled(holder);
    }
}

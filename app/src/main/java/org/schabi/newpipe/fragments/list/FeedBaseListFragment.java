package org.schabi.newpipe.fragments.list;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import org.schabi.newpipe.R;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.channel.ChannelInfoItem;
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.fragments.BaseStateFragment;
import org.schabi.newpipe.fragments.FeedBaseStateFragment;
import org.schabi.newpipe.fragments.MainFragment;
import org.schabi.newpipe.fragments.OnScrollBelowItemsListener;
import org.schabi.newpipe.info_list.FeedInfoListAdapter;
import org.schabi.newpipe.info_list.SubscriptionInfoListAdapter;
import org.schabi.newpipe.local.dialog.PlaylistAppendDialog;
import org.schabi.newpipe.info_list.InfoItemDialog;
import org.schabi.newpipe.info_list.InfoListAdapter;
import org.schabi.newpipe.player.playqueue.SinglePlayQueue;
import org.schabi.newpipe.util.NavigationHelper;
import org.schabi.newpipe.util.OnClickGesture;
import org.schabi.newpipe.util.StateSaver;
import org.schabi.newpipe.views.HiveRecyclerView;
import org.schabi.newpipe.views.HiveRecyclerViewNew;
import org.schabi.newpipe.views.HiveRecyclerViewSubscription;

import java.util.Collections;
import java.util.List;
import java.util.Queue;

import static org.schabi.newpipe.util.AnimationUtils.animateView;

public abstract class FeedBaseListFragment<I, N> extends FeedBaseStateFragment<I> implements ListViewContract<I, N>, StateSaver.WriteRead {

    /*//////////////////////////////////////////////////////////////////////////
    // Views
    //////////////////////////////////////////////////////////////////////////*/

    protected FeedInfoListAdapter  feedinfoListAdapter;
    public static HiveRecyclerViewNew newList;
    protected RelativeLayout newlayout;
    protected RelativeLayout subscriblayout;

    protected SubscriptionInfoListAdapter infoListAdapter;
    public static HiveRecyclerViewSubscription itemsList;
    public static int clickPosition=-1;
    public static int clickPosition_down=-1;
    public static LinearLayoutManager msfeed;
    public static LinearLayoutManager ms;

    /*//////////////////////////////////////////////////////////////////////////
    // LifeCycle
    //////////////////////////////////////////////////////////////////////////*/
 

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        feedinfoListAdapter = new FeedInfoListAdapter(activity);
        infoListAdapter = new SubscriptionInfoListAdapter(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        feedinfoListAdapter.unRegister();
        infoListAdapter.unRegister();
        StateSaver.onDestroy(savedState);
    }

    /*//////////////////////////////////////////////////////////////////////////
    // State Saving
    //////////////////////////////////////////////////////////////////////////*/

    protected StateSaver.SavedState savedState;

    @Override
    public String generateSuffix() {
        // Naive solution, but it's good for now (the items don't change)
        return "." + feedinfoListAdapter.getItemsList().size() + ".list";
    }

    @Override
    public void writeTo(Queue<Object> objectsToSave) {
        objectsToSave.add(feedinfoListAdapter.getItemsList());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void readFrom(@NonNull Queue<Object> savedObjects) throws Exception {
        feedinfoListAdapter.getItemsList().clear();
        feedinfoListAdapter.getItemsList().addAll((List<InfoItem>) savedObjects.poll());
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        savedState = StateSaver.tryToSave(activity.isChangingConfigurations(), savedState, bundle, this);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        savedState = StateSaver.tryToRestore(bundle, this);
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Init
    //////////////////////////////////////////////////////////////////////////*/

    protected View getListHeader() {
        return null;
    }

    protected View getListFooter() {
        return activity.getLayoutInflater().inflate(R.layout.pignate_footer, newList, false);
    }

    protected HiveRecyclerView.LayoutManager getListLayoutManager() {
        return new LinearLayoutManager(activity);
    }

    @Override
    protected void initViews(View rootView, Bundle savedInstanceState) {
        super.initViews(rootView, savedInstanceState);
        subscriblayout=rootView.findViewById(R.id.subscriblayout);
        newlayout=rootView.findViewById(R.id.newlayout);
        newList = rootView.findViewById(R.id.newlist);
        msfeed= new LinearLayoutManager(activity);
        msfeed.setOrientation(LinearLayoutManager.HORIZONTAL);// 设置 recyclerview 布局方式为横向布局
        newList.setLayoutManager(msfeed);
        newList.setAdapter(feedinfoListAdapter);

        itemsList = rootView.findViewById(R.id.items_list);
        ms= new LinearLayoutManager(activity);
        ms.setOrientation(LinearLayoutManager.HORIZONTAL);// 设置 recyclerview 布局方式为横向布局
        itemsList.setLayoutManager(ms);
        itemsList.addItemDecoration(new SpacesItemDecoration(10));
        itemsList.setAdapter(infoListAdapter);
    }

    protected void onItemSelected(InfoItem selectedItem) {
        if (DEBUG) Log.d(TAG, "onItemSelected() called with: selectedItem = [" + selectedItem + "]");
    }

    @Override
    protected void initListeners() {
        super.initListeners();
        feedinfoListAdapter.setOnStreamSelectedListener(new OnClickGesture<StreamInfoItem>() {
            @Override
            public void selected(StreamInfoItem selectedItem) {
               // final int index = Math.max(feedinfoListAdapter.getItemsList().indexOf(selectedItem), 0);
                clickPosition=0;
                onStreamSelected(selectedItem);
            }

            @Override
            public void held(StreamInfoItem selectedItem) {
               // showStreamDialog(selectedItem);
            }
        });

        feedinfoListAdapter.setOnChannelSelectedListener(new OnClickGesture<ChannelInfoItem>() {
            @Override
            public void selected(ChannelInfoItem selectedItem) {
                onItemSelected(selectedItem);
                NavigationHelper.openChannelFragment(useAsFrontPage ? getParentFragment().getFragmentManager() : getFragmentManager(),
                        selectedItem.getServiceId(), selectedItem.getUrl(), selectedItem.getName());
            }
        });

        feedinfoListAdapter.setOnPlaylistSelectedListener(new OnClickGesture<PlaylistInfoItem>() {
            @Override
            public void selected(PlaylistInfoItem selectedItem) {
                onItemSelected(selectedItem);
                NavigationHelper.openPlaylistFragment(useAsFrontPage ? getParentFragment().getFragmentManager() : getFragmentManager(),
                        selectedItem.getServiceId(), selectedItem.getUrl(), selectedItem.getName());
            }
        });

        newList.clearOnScrollListeners();
        newList.addOnScrollListener(new OnScrollBelowItemsListener() {
            @Override
            public void onScrolledDown(RecyclerView recyclerView) {
                onScrollToBottom();
            }
        });

        infoListAdapter.setOnChannelSelectedListener(new OnClickGesture<ChannelInfoItem>() {
            @Override
            public void selected(ChannelInfoItem selectedItem) {
                // Requires the parent fragment to find holder for fragment replacement
                final int index = Math.max(infoListAdapter.getItemsList().indexOf(selectedItem), 0);
                clickPosition_down=index;
                NavigationHelper.openChannelFragment(MainFragment.manager,
                        selectedItem.getServiceId(), selectedItem.getUrl(), selectedItem.getName());
            }
        });
    }

    private void onStreamSelected(StreamInfoItem selectedItem) {
        onItemSelected(selectedItem);
        NavigationHelper.openVideoDetailFragment(getParentFragment().getFragmentManager(),
                selectedItem.getServiceId(), selectedItem.getUrl(), selectedItem.getName());
    }

    protected void onScrollToBottom() {
        if (hasMoreItems() && !isLoading.get()) {
            loadMoreItems();
        }
    }

    protected void showStreamDialog(final StreamInfoItem item) {
        final Context context = getContext();
        final Activity activity = getActivity();
        if (context == null || context.getResources() == null || getActivity() == null) return;

        final String[] commands = new String[]{
                context.getResources().getString(R.string.enqueue_on_background),
                context.getResources().getString(R.string.enqueue_on_popup),
                context.getResources().getString(R.string.append_playlist)
        };

        final DialogInterface.OnClickListener actions = (dialogInterface, i) -> {
            switch (i) {
                case 0:
                    NavigationHelper.enqueueOnBackgroundPlayer(context, new SinglePlayQueue(item));
                    break;
                case 1:
                    NavigationHelper.enqueueOnPopupPlayer(activity, new SinglePlayQueue(item));
                    break;
                case 2:
                    if (getFragmentManager() != null) {
                        PlaylistAppendDialog.fromStreamInfoItems(Collections.singletonList(item))
                                .show(getFragmentManager(), TAG);
                    }
                    break;
                default:
                    break;
            }
        };

        new InfoItemDialog(getActivity(), item, commands, actions).show();
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Menu
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (DEBUG) Log.d(TAG, "onCreateOptionsMenu() called with: menu = [" + menu + "], inflater = [" + inflater + "]");
        super.onCreateOptionsMenu(menu, inflater);
        ActionBar supportActionBar = activity.getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayShowTitleEnabled(true);
            if (useAsFrontPage) {
                supportActionBar.setDisplayHomeAsUpEnabled(false);
            } else {
                supportActionBar.setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Load and handle
    //////////////////////////////////////////////////////////////////////////*/

    protected abstract void loadMoreItems();

    protected abstract boolean hasMoreItems();

    /*//////////////////////////////////////////////////////////////////////////
    // Contract
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public void showLoading() {
        super.showLoading();
        // animateView(itemsList, false, 400);
    }

    @Override
    public void hideLoading() {
        super.hideLoading();
        animateView(itemsList, true, 300);
        animateView(newList, true, 300);
    }

    @Override
    public void showError(String message, boolean showRetryButton) {
        super.showError(message, showRetryButton);
        showListFooter(false);
       /* animateView(itemsList, false, 200);
        animateView(newList, false, 200);*/
    }

    @Override
    public void showEmptyState() {
        super.showEmptyState();
        showListFooter(false);
    }

    @Override
    public void showListFooter(final boolean show) {
        newList.post(() -> {
            if (feedinfoListAdapter != null && newList != null) {
                feedinfoListAdapter.showFooter(show);
            }
        });
    }

    @Override
    public void handleNextItems(N result) {
        isLoading.set(false);
    }


    public class SpacesItemDecoration extends RecyclerView.ItemDecoration {
        private int space;
        public SpacesItemDecoration(int space) {
            this.space = space;
        }
        @Override
        public void getItemOffsets(Rect outRect, View view,
                                   RecyclerView parent, RecyclerView.State state) {
            //outRect.left = space;
            outRect.right = space;
           // outRect.bottom = space;
            // Add top margin only for the first item to avoid double space between items
           // if(parent.getChildPosition(view) == 0)
              //  outRect.top = space;
        }
    }
}

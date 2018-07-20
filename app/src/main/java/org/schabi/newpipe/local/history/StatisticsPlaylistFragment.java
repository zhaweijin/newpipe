package org.schabi.newpipe.local.history;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.schabi.newpipe.App;
import org.schabi.newpipe.R;
import org.schabi.newpipe.database.LocalItem;
import org.schabi.newpipe.database.stream.StreamStatisticsEntry;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.fragments.MainFragment;
import org.schabi.newpipe.info_list.InfoItemDialog;
import org.schabi.newpipe.local.HistoryAdapter;
import org.schabi.newpipe.local.HistorygridFragment;
import org.schabi.newpipe.player.playqueue.PlayQueue;
import org.schabi.newpipe.player.playqueue.SinglePlayQueue;
import org.schabi.newpipe.report.UserAction;
import org.schabi.newpipe.util.NavigationHelper;
import org.schabi.newpipe.util.OnClickGesture;
import org.schabi.newpipe.util.ThemeHelper;
import org.schabi.newpipe.views.BackgroundBlurPopupWindow;
import org.schabi.newpipe.views.DeletePopWindow;
import org.schabi.newpipe.views.WindowInterface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import icepick.State;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class StatisticsPlaylistFragment
        extends HistorygridFragment<List<StreamStatisticsEntry>, Void>{

    private Button headerPlayAllButton;
    /*private View headerPopupButton;
    private View headerBackgroundButton;*/
    private View playlistCtrl;
    private View sortButton;
    private ImageView sortButtonIcon;
    private TextView sortButtonText;

    @State
    protected Parcelable itemsListState;

    /* Used for independent events */
    private Subscription databaseSubscription;
    private HistoryRecordManager recordManager;
    private CompositeDisposable disposables = new CompositeDisposable();
    private DeletePopWindow deletePopWindow;
    private BackgroundBlurPopupWindow mPopupWindow;
    private View popupWindowView;
    public static int deleteLocation=-1;
    public static int clickPosition=-1;
    public static boolean statisticPlayAll = false;
    public static boolean statisticOnePlayAll = false;

    private enum StatisticSortMode {
        LAST_PLAYED,
        MOST_PLAYED,
    }

    StatisticSortMode sortMode = StatisticSortMode.LAST_PLAYED;

    protected List<StreamStatisticsEntry> processResult(final List<StreamStatisticsEntry> results) {
        switch (sortMode) {
            case LAST_PLAYED:
                Collections.sort(results, (left, right) ->
                    right.latestAccessDate.compareTo(left.latestAccessDate));
                return results;
            case MOST_PLAYED:
                Collections.sort(results, (left, right) ->
                        ((Long) right.watchCount).compareTo(left.watchCount));
                return results;
            default: return null;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Fragment LifeCycle - Creation
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        recordManager = new HistoryRecordManager(getContext());
        popupWindowView = LayoutInflater.from(activity).inflate(R.layout.delete_pop_layout, null);
        mPopupWindow = new BackgroundBlurPopupWindow(popupWindowView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,activity,true);
        mPopupWindow.setFocusable(true);
        mPopupWindow.setBackgroundDrawable(new BitmapDrawable());
        mPopupWindow.setAnimationStyle(android.R.style.Animation_Dialog);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playlist, container, false);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Fragment LifeCycle - Views
    ///////////////////////////////////////////////////////////////////////////

    @Override
    protected void initViews(View rootView, Bundle savedInstanceState) {
        super.initViews(rootView, savedInstanceState);
        setTitle(getString(R.string.title_last_played));
    }

    @Override
    protected View getListHeader() {
        final View headerRootLayout = activity.getLayoutInflater().inflate(R.layout.statistic_playlist_control,
                itemsList, false);
        playlistCtrl = headerRootLayout.findViewById(R.id.playlist_control);
        headerPlayAllButton = headerRootLayout.findViewById(R.id.playlist_ctrl_play_all_button);
        sortButton = headerRootLayout.findViewById(R.id.sortButton);
        sortButton.setVisibility(View.GONE);
        sortButtonIcon = headerRootLayout.findViewById(R.id.sortButtonIcon);
        sortButtonText = headerRootLayout.findViewById(R.id.sortButtonText);
        headerPlayAllButton.setOnKeyListener(onKeyListener);
        headerPlayAllButton.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(b){
                    if(HistoryAdapter.mPopupWindow!=null&&HistoryAdapter.mPopupWindow.isShowing()){
                        HistoryAdapter.mPopupWindow.dismiss();
                    }
                }
            }
        });
        //NavigationHelper.playOnMainPlayer(activity, getPlayQueue()
        headerPlayAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavigationHelper.playOnMainPlayer(activity, getPlayQueue());
                statisticPlayAll = true;
                App.isAutoPlay  =true;
                MainFragment.btn_find.setFocusable(false);
                MainFragment.layout_recommend.setFocusable(false);
                MainFragment.layout_subscribe.setFocusable(false);
                MainFragment.layout_collection.setFocusable(false);
                MainFragment.layout_history.setFocusable(false);
            }
        });
        return headerRootLayout;
    }

    @Override
    protected void initListeners() {
        super.initListeners();

        itemListAdapter.setSelectedListener(new OnClickGesture<LocalItem>() {
            @Override
            public void selected(LocalItem selectedItem) {
                if (selectedItem instanceof StreamStatisticsEntry) {
                    final int index = Math.max(itemListAdapter.getItemsList().indexOf(selectedItem), 0);
                    clickPosition=index;
                    if(HistoryAdapter.mPopupWindow!=null&&HistoryAdapter.mPopupWindow.isShowing()){
                        HistoryAdapter.mPopupWindow.dismiss();
                    }
                    final StreamStatisticsEntry item = (StreamStatisticsEntry) selectedItem;
                    NavigationHelper.openVideoDetailFragment(getParentFragment().getFragmentManager(),
                            item.serviceId, item.url, item.title);
                }
            }

            @Override
            public void held(LocalItem selectedItem) {
                if (selectedItem instanceof StreamStatisticsEntry) {
                   // showStreamDialog((StreamStatisticsEntry) selectedItem);
                    showWindows((StreamStatisticsEntry) selectedItem);
                }
            }
        });
    }

    ///////////////////////////////////////////////////////////////////////////
    // Fragment LifeCycle - Loading
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void startLoading(boolean forceLoad) {
        super.startLoading(forceLoad);
        recordManager.getStreamStatistics()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(getHistoryObserver());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Fragment LifeCycle - Destruction
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onPause() {
        super.onPause();
        itemsListState = itemsList.getLayoutManager().onSaveInstanceState();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (itemListAdapter != null) itemListAdapter.unsetSelectedListener();
        //if (headerBackgroundButton != null) headerBackgroundButton.setOnClickListener(null);
        if (headerPlayAllButton != null) headerPlayAllButton.setOnClickListener(null);
        //if (headerPopupButton != null) headerPopupButton.setOnClickListener(null);

        if (databaseSubscription != null) databaseSubscription.cancel();
        databaseSubscription = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        recordManager = null;
        itemsListState = null;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Statistics Loader
    ///////////////////////////////////////////////////////////////////////////

    private Subscriber<List<StreamStatisticsEntry>> getHistoryObserver() {
        return new Subscriber<List<StreamStatisticsEntry>>() {
            @Override
            public void onSubscribe(Subscription s) {
                showLoading();

                if (databaseSubscription != null) databaseSubscription.cancel();
                databaseSubscription = s;
                databaseSubscription.request(1);
            }

            @Override
            public void onNext(List<StreamStatisticsEntry> streams) {
                handleResult(streams);
                if (databaseSubscription != null) databaseSubscription.request(1);
            }

            @Override
            public void onError(Throwable exception) {
                StatisticsPlaylistFragment.this.onError(exception);
            }

            @Override
            public void onComplete() {
            }
        };
    }

    @Override
    public void handleResult(@NonNull List<StreamStatisticsEntry> result) {
        super.handleResult(result);
        if (itemListAdapter == null) return;

        playlistCtrl.setVisibility(View.VISIBLE);

        itemListAdapter.clearStreamItemList();

        if (result.isEmpty()) {
            MainFragment.btn_find.setFocusable(false);
            MainFragment.layout_recommend.setFocusable(false);
            MainFragment.layout_subscribe.setFocusable(false);
            MainFragment.layout_collection.setFocusable(false);
            MainFragment.layout_history.setFocusable(true);
            MainFragment.layout_history.requestFocus();
            showEmptyState();
            return;
        }

        itemListAdapter.addItems(processResult(result));
        if (itemsListState != null) {
            itemsList.getLayoutManager().onRestoreInstanceState(itemsListState);
            itemsListState = null;
        }

        /*headerPlayAllButton.setOnClickListener(view ->
                NavigationHelper.playOnMainPlayer(activity, getPlayQueue()));*/
        sortButton.setOnClickListener(view -> toggleSortMode());

        hideLoading();
    }
    ///////////////////////////////////////////////////////////////////////////
    // Fragment Error Handling
    ///////////////////////////////////////////////////////////////////////////

    @Override
    protected void resetFragment() {
        super.resetFragment();
        if (databaseSubscription != null) databaseSubscription.cancel();
    }

    @Override
    protected boolean onError(Throwable exception) {
        if (super.onError(exception)) return true;

        onUnrecoverableError(exception, UserAction.SOMETHING_ELSE,
                "none", "History Statistics", R.string.general_error);
        return true;
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Utils
    //////////////////////////////////////////////////////////////////////////*/

    private void toggleSortMode() {
        if(sortMode == StatisticSortMode.LAST_PLAYED) {
            sortMode = StatisticSortMode.MOST_PLAYED;
            setTitle(getString(R.string.title_most_played));
            sortButtonIcon.setImageResource(ThemeHelper.getIconByAttr(R.attr.history, getContext()));
            sortButtonText.setText(R.string.title_last_played);
        } else {
            sortMode = StatisticSortMode.LAST_PLAYED;
            setTitle(getString(R.string.title_last_played));
            sortButtonIcon.setImageResource(ThemeHelper.getIconByAttr(R.attr.filter, getContext()));
            sortButtonText.setText(R.string.title_most_played);
        }
        startLoading(true);
    }

    private void showStreamDialog(final StreamStatisticsEntry item) {
        final Context context = getContext();
        final Activity activity = getActivity();
        if (context == null || context.getResources() == null || getActivity() == null) return;
        final StreamInfoItem infoItem = item.toStreamInfoItem();

        final String[] commands = new String[]{
                context.getResources().getString(R.string.enqueue_on_background),
                context.getResources().getString(R.string.enqueue_on_popup),
                context.getResources().getString(R.string.start_here_on_main),
                context.getResources().getString(R.string.start_here_on_background),
                context.getResources().getString(R.string.start_here_on_popup),
                context.getResources().getString(R.string.delete),
        };

        final DialogInterface.OnClickListener actions = (dialogInterface, i) -> {
            final int index = Math.max(itemListAdapter.getItemsList().indexOf(item), 0);
            switch (i) {
                case 0:
                    NavigationHelper.enqueueOnBackgroundPlayer(context, new SinglePlayQueue(infoItem));
                    break;
                case 1:
                    NavigationHelper.enqueueOnPopupPlayer(activity, new SinglePlayQueue(infoItem));
                    break;
                case 2:
                    NavigationHelper.playOnMainPlayer(context, getPlayQueue(index));
                    statisticOnePlayAll = true;
                    break;
                case 3:
                    NavigationHelper.playOnBackgroundPlayer(context, getPlayQueue(index));
                    break;
                case 4:
                    NavigationHelper.playOnPopupPlayer(activity, getPlayQueue(index));
                    break;
                case 5:
                    deleteEntry(index);
                    break;
                default:
                    break;
            }
        };

        new InfoItemDialog(getActivity(), infoItem, commands, actions).show();
    }

    private void deleteEntry(final int index) {
        final LocalItem infoItem = itemListAdapter.getItemsList()
                .get(index);
        if(infoItem instanceof StreamStatisticsEntry) {
            final StreamStatisticsEntry entry = (StreamStatisticsEntry) infoItem;
            final Disposable onDelete = recordManager.deleteStreamHistory(entry.streamId)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            howManyDelted -> {
                                if(getView() != null) {
                                    /*Snackbar.make(getView(), R.string.one_item_deleted,
                                            Snackbar.LENGTH_SHORT).show();*/


                                } else {
                                   /* Toast.makeText(getContext(),
                                            R.string.one_item_deleted,
                                            Toast.LENGTH_SHORT).show();*/
                                }
                            },
                            throwable -> showSnackBarError(throwable,
                                    UserAction.DELETE_FROM_HISTORY, "none",
                                    "Deleting item failed", R.string.general_error));

            disposables.add(onDelete);
        }
    }

    private PlayQueue getPlayQueue() {
        return getPlayQueue(0);
    }

    private PlayQueue getPlayQueue(final int index) {
        if (itemListAdapter == null) {
            return new SinglePlayQueue(Collections.emptyList(), 0);
        }

        final List<LocalItem> infoItems = itemListAdapter.getItemsList();
        List<StreamInfoItem> streamInfoItems = new ArrayList<>(infoItems.size());
        for (final LocalItem item : infoItems) {
            if (item instanceof StreamStatisticsEntry) {
                streamInfoItems.add(((StreamStatisticsEntry) item).toStreamInfoItem());
            }
        }
        return new SinglePlayQueue(streamInfoItems, index);
    }
    private void showWindows(final StreamStatisticsEntry item) {
        mPopupWindow.setBlurRadius(4);
        mPopupWindow.setDownScaleFactor(1.5f);
        mPopupWindow.setDarkColor(Color.parseColor("#c0000000"));
        mPopupWindow.resetDarkPosition();
        mPopupWindow.darkFillScreen();
        mPopupWindow.showAtLocation(popupWindowView, Gravity.CENTER, 0, 0);
        Button confirm=(Button)popupWindowView.findViewById(R.id.confirm);
        Button cancel=(Button)popupWindowView.findViewById(R.id.cancel);
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final int index = Math.max(itemListAdapter.getItemsList().indexOf(item), 0);
                deleteEntry(index);
                deleteLocation=index;
                mPopupWindow.dismiss();
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPopupWindow.dismiss();
            }
        });
    }
    View.OnKeyListener onKeyListener=new View.OnKeyListener() {
        @Override
        public boolean onKey(View view, int keyCode, KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if(view.getId()==R.id.playlist_ctrl_play_all_button && keyCode==KeyEvent.KEYCODE_DPAD_UP){
                    MainFragment.layout_history.setFocusable(true);
                    MainFragment.layout_history.requestFocus();
                }else if(view.getId()==R.id.playlist_ctrl_play_all_button && keyCode==KeyEvent.KEYCODE_DPAD_DOWN){
                    StatisticsPlaylistFragment.clickPosition=0;
                    itemListAdapter.notifyDataSetChanged();
                }else if(view.getId()==R.id.playlist_ctrl_play_all_button && keyCode==KeyEvent.KEYCODE_BACK){
                    MainFragment.layout_history.setFocusable(true);
                    MainFragment.layout_history.requestFocus();
                    return true;
                }else if(view.getId()==R.id.playlist_ctrl_play_all_button && keyCode==KeyEvent.KEYCODE_DPAD_LEFT){
                    return true;
                }else if(view.getId()==R.id.playlist_ctrl_play_all_button && keyCode==KeyEvent.KEYCODE_DPAD_RIGHT){
                    return true;
                }
            }
            return false;
        }
    };


}


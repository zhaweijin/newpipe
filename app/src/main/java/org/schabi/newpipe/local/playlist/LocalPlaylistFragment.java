package org.schabi.newpipe.local.playlist;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.schabi.newpipe.App;
import org.schabi.newpipe.NewPipeDatabase;
import org.schabi.newpipe.R;
import org.schabi.newpipe.database.LocalItem;
import org.schabi.newpipe.database.playlist.PlaylistMetadataEntry;
import org.schabi.newpipe.database.playlist.PlaylistStreamEntry;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.fragments.MainFragment;
import org.schabi.newpipe.fragments.detail.VideoDetailFragment;
import org.schabi.newpipe.local.BaseLocalListFragment;
import org.schabi.newpipe.info_list.InfoItemDialog;
import org.schabi.newpipe.local.BookMarkBaseLocalListFragment;
import org.schabi.newpipe.local.HistoryAdapter;
import org.schabi.newpipe.local.LocalItemListAdapterNew;
import org.schabi.newpipe.player.playqueue.PlayQueue;
import org.schabi.newpipe.player.playqueue.SinglePlayQueue;
import org.schabi.newpipe.report.UserAction;
import org.schabi.newpipe.util.Localization;
import org.schabi.newpipe.util.NavigationHelper;
import org.schabi.newpipe.util.OnClickGesture;
import org.schabi.newpipe.views.BackgroundBlurPopupWindow;
import org.schabi.newpipe.views.HiveRecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import icepick.State;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.PublishSubject;

import static org.schabi.newpipe.util.AnimationUtils.animateView;

public class LocalPlaylistFragment extends BookMarkBaseLocalListFragment<List<PlaylistStreamEntry>, Void> {

    // Save the list 10 seconds after the last change occurred
    private static final long SAVE_DEBOUNCE_MILLIS = 10000;
    private static final int MINIMUM_INITIAL_DRAG_VELOCITY = 12;

    private View headerRootLayout;
    private TextView headerTitleView;
    private TextView headerStreamCount;

    private View playlistControl;
    private Button headerPlayAllButton;

    @State
    protected Long playlistId;
    @State
    protected String name;
    @State
    protected Parcelable itemsListState;

    private ItemTouchHelper itemTouchHelper;

    private LocalPlaylistManager playlistManager;
    private Subscription databaseSubscription;

    private PublishSubject<Long> debouncedSaveSignal;
    private CompositeDisposable disposables;

    /* Has the playlist been fully loaded from db */
    private AtomicBoolean isLoadingComplete;
    /* Has the playlist been modified (e.g. items reordered or deleted) */
    private AtomicBoolean isModified;
    private BackgroundBlurPopupWindow mPopupWindow;
    private View popupWindowView;
    public static int clickPosition=-1;
    public static boolean localPlayAll  = false;
    public static boolean localOnePlayAll = false;

    public static LocalPlaylistFragment getInstance(long playlistId, String name) {
        LocalPlaylistFragment instance = new LocalPlaylistFragment();
        //instance.setInitialData(playlistId, name);
        return instance;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Fragment LifeCycle - Creation
    ///////////////////////////////////////////////////////////////////////////

    //********************add
    private Disposable playlistReactor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        playlistManager = new LocalPlaylistManager(NewPipeDatabase.getInstance(getContext()));
        debouncedSaveSignal = PublishSubject.create();

        disposables = new CompositeDisposable();

        isLoadingComplete = new AtomicBoolean();
        isModified = new AtomicBoolean();


        popupWindowView = LayoutInflater.from(activity).inflate(R.layout.cancel_pop_layout, null);
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
        return inflater.inflate(R.layout.fragment_bookmarks, container, false);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Fragment Lifecycle - Views
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void setTitle(final String title) {
        super.setTitle(title);

        if (headerTitleView != null) {
            headerTitleView.setText(title);
        }
    }

    @Override
    protected void initViews(View rootView, Bundle savedInstanceState) {
        super.initViews(rootView, savedInstanceState);
        setTitle(name);
    }

    @Override
    protected View getListHeader() {
        headerRootLayout = activity.getLayoutInflater().inflate(R.layout.local_playlist_header_new,
                itemsList, false);

        headerTitleView = headerRootLayout.findViewById(R.id.playlist_title_view);
        headerTitleView.setSelected(true);

        headerStreamCount = headerRootLayout.findViewById(R.id.playlist_stream_count);

        playlistControl = headerRootLayout.findViewById(R.id.playlist_control);
        headerPlayAllButton = headerRootLayout.findViewById(R.id.playlist_ctrl_play_all_button);
        headerPlayAllButton.setOnKeyListener(onKeyListener);
        headerPlayAllButton.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(b){
                    if(LocalItemListAdapterNew.mPopupWindow!=null&&LocalItemListAdapterNew.mPopupWindow.isShowing()){
                        LocalItemListAdapterNew.mPopupWindow.dismiss();
                    }
                }
            }
        });
        return headerRootLayout;
    }

    @Override
    protected void initListeners() {
        super.initListeners();

        headerTitleView.setOnClickListener(view -> createRenameDialog());

        itemTouchHelper = new ItemTouchHelper(getItemTouchCallback());
        itemTouchHelper.attachToRecyclerView(itemsList);

        itemListAdapter.setSelectedListener(new OnClickGesture<LocalItem>() {
            @Override
            public void selected(LocalItem selectedItem) {
                if (selectedItem instanceof PlaylistStreamEntry) {
                    final int index = Math.max(itemListAdapter.getItemsList().indexOf(selectedItem), 0);
                    clickPosition=index;
                    if(LocalItemListAdapterNew.mPopupWindow!=null&&LocalItemListAdapterNew.mPopupWindow.isShowing()){
                        LocalItemListAdapterNew.mPopupWindow.dismiss();
                    }
                    final PlaylistStreamEntry item = (PlaylistStreamEntry) selectedItem;
                    NavigationHelper.openVideoDetailFragment(getParentFragment().getFragmentManager(),
                            item.serviceId, item.url, item.title);
                }
            }

            @Override
            public void held(LocalItem selectedItem) {
                if (selectedItem instanceof PlaylistStreamEntry) {
                   // showStreamItemDialog((PlaylistStreamEntry) selectedItem);
                    showWindows((PlaylistStreamEntry) selectedItem);
                }
            }

            @Override
            public void drag(LocalItem selectedItem, HiveRecyclerView.ViewHolder viewHolder) {
                if (itemTouchHelper != null) itemTouchHelper.startDrag(viewHolder);
            }
        });
    }

    ///////////////////////////////////////////////////////////////////////////
    // Fragment Lifecycle - Loading
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void showLoading() {
        super.showLoading();
        if (headerRootLayout != null) animateView(headerRootLayout, false, 200);
        if (playlistControl != null) animateView(playlistControl, false, 200);
    }

    @Override
    public void hideLoading() {
        super.hideLoading();
        if (headerRootLayout != null) animateView(headerRootLayout, true, 200);
        if (playlistControl != null) animateView(playlistControl, true, 200);
    }

    @Override
    public void startLoading(boolean forceLoad) {
        super.startLoading(forceLoad);

        if (disposables != null) disposables.clear();
        disposables.add(getDebouncedSaver());

        isLoadingComplete.set(false);
        isModified.set(false);

        playlistReactor = playlistManager.getPlaylists()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<PlaylistMetadataEntry>>() {
                    @Override
                    public void accept(List<PlaylistMetadataEntry> playlistMetadataEntries) throws Exception {


                        if(playlistReactor!=null && !playlistReactor.isDisposed()){
                            playlistReactor.dispose();
                        }

                        if(playlistMetadataEntries.size()<=0){
                            showEmptyState();
                            return;
                        }

                        playlistId = playlistMetadataEntries.get(0).uid;
                        if(playlistManager!=null&&playlistId!=null){
                            playlistManager.getPlaylistStreams(playlistId)
                                    .onBackpressureLatest()
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(getPlaylistObserver());
                        }

                    }
                });


    }

    ///////////////////////////////////////////////////////////////////////////
    // Fragment Lifecycle - Destruction
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onPause() {
        super.onPause();
        itemsListState = itemsList.getLayoutManager().onSaveInstanceState();

        // Save on exit
        saveImmediate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (itemListAdapter != null) itemListAdapter.unsetSelectedListener();
       if (headerPlayAllButton != null) headerPlayAllButton.setOnClickListener(null);

        if (databaseSubscription != null) databaseSubscription.cancel();
        if (disposables != null) disposables.clear();

        databaseSubscription = null;
        itemTouchHelper = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (debouncedSaveSignal != null) debouncedSaveSignal.onComplete();
        if (disposables != null) disposables.dispose();

        debouncedSaveSignal = null;
        playlistManager = null;
        disposables = null;

        isLoadingComplete = null;
        isModified = null;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Playlist Stream Loader
    ///////////////////////////////////////////////////////////////////////////

    private Subscriber<List<PlaylistStreamEntry>> getPlaylistObserver() {
        return new Subscriber<List<PlaylistStreamEntry>>() {
            @Override
            public void onSubscribe(Subscription s) {
                showLoading();
                isLoadingComplete.set(false);

                if (databaseSubscription != null) databaseSubscription.cancel();
                databaseSubscription = s;
                databaseSubscription.request(1);
            }

            @Override
            public void onNext(List<PlaylistStreamEntry> streams) {
                // Skip handling the result after it has been modified
                if (isModified == null || !isModified.get()) {
                    handleResult(streams);
                    isLoadingComplete.set(true);
                }

                if (databaseSubscription != null) databaseSubscription.request(1);
            }

            @Override
            public void onError(Throwable exception) {
                LocalPlaylistFragment.this.onError(exception);
            }

            @Override
            public void onComplete() {}
        };
    }

    @Override
    public void handleResult(@NonNull List<PlaylistStreamEntry> result) {
        super.handleResult(result);
        if (itemListAdapter == null) return;
        playlistControl.setVisibility(View.VISIBLE);
        itemListAdapter.clearStreamItemList();
        if (result.isEmpty()) {
            showEmptyState();
            return;
        }
         if(result.size()!=0){
             itemsList.setVisibility(View.VISIBLE);
         }
        itemListAdapter.addItems(result);
        if (itemsListState != null) {
            itemsList.getLayoutManager().onRestoreInstanceState(itemsListState);
            itemsListState = null;
        }
        setVideoCount(itemListAdapter.getItemsList().size());

        headerPlayAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavigationHelper.playOnMainPlayer(activity, LocalPlaylistFragment.this.getPlayQueue());
                localPlayAll = true;
                App.isAutoPlay = true;
                MainFragment.btn_find.setFocusable(false);
                MainFragment.layout_recommend.setFocusable(false);
                MainFragment.layout_subscribe.setFocusable(false);
                MainFragment.layout_collection.setFocusable(false);
                MainFragment.layout_history.setFocusable(false);
            }
        });

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
                "none", "Local Playlist", R.string.general_error);
        return true;
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Playlist Metadata/Streams Manipulation
    //////////////////////////////////////////////////////////////////////////*/

    private void createRenameDialog() {
        if (playlistId == null || name == null || getContext() == null) return;

        final View dialogView = View.inflate(getContext(), R.layout.dialog_playlist_name, null);
        EditText nameEdit = dialogView.findViewById(R.id.playlist_name);
        nameEdit.setText(name);
        nameEdit.setSelection(nameEdit.getText().length());

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext())
                .setTitle(R.string.rename_playlist)
                .setView(dialogView)
                .setCancelable(true)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.rename, (dialogInterface, i) -> {
                    changePlaylistName(nameEdit.getText().toString());
                });

        dialogBuilder.show();
    }

    private void changePlaylistName(final String name) {
        if (playlistManager == null) return;

        this.name = name;
        setTitle(name);

        Log.d(TAG, "Updating playlist id=[" + playlistId +
                "] with new name=[" + name + "] items");

        final Disposable disposable = playlistManager.renamePlaylist(playlistId, name)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(longs -> {/*Do nothing on success*/}, this::onError);
        disposables.add(disposable);
    }

    private void changeThumbnailUrl(final String thumbnailUrl) {
        if (playlistManager == null) return;

        final Toast successToast = Toast.makeText(getActivity(),
                R.string.playlist_thumbnail_change_success,
                Toast.LENGTH_SHORT);

        Log.d(TAG, "Updating playlist id=[" + playlistId +
                "] with new thumbnail url=[" + thumbnailUrl + "]");

        final Disposable disposable = playlistManager
                .changePlaylistThumbnail(playlistId, thumbnailUrl)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(ignore -> successToast.show(), this::onError);
        disposables.add(disposable);
    }

    private void deleteItem(final PlaylistStreamEntry item) {
        if (itemListAdapter == null) return;

        itemListAdapter.removeItem(item);
        if(itemListAdapter.getItemsList().size()==0){
          /*  itemsList.setVisibility(View.GONE);
            if(LocalItemListAdapterNew.mPopupWindow!=null&&LocalItemListAdapterNew.mPopupWindow.isShowing()){
                LocalItemListAdapterNew.mPopupWindow.dismiss();
            }*/
            MainFragment.btn_find.setFocusable(false);
            MainFragment.layout_recommend.setFocusable(false);
            MainFragment.layout_subscribe.setFocusable(false);
            MainFragment.layout_history.setFocusable(false);
            MainFragment.layout_collection.setFocusable(true);
            MainFragment.layout_collection.requestFocus();
            showEmptyState();
        }
        setVideoCount(itemListAdapter.getItemsList().size());
        saveChanges();
    }

    private void saveChanges() {
        if (isModified == null || debouncedSaveSignal == null) return;

        isModified.set(true);
        debouncedSaveSignal.onNext(System.currentTimeMillis());
    }

    private Disposable getDebouncedSaver() {
        if (debouncedSaveSignal == null) return Disposables.empty();

        return debouncedSaveSignal
                .debounce(SAVE_DEBOUNCE_MILLIS, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long ignored) throws Exception {
                        LocalPlaylistFragment.this.saveImmediate();
                    }
                }, this::onError);
    }

    private void saveImmediate() {
        if (playlistManager == null || itemListAdapter == null) return;

        // List must be loaded and modified in order to save
        if (isLoadingComplete == null || isModified == null ||
                !isLoadingComplete.get() || !isModified.get()) {
            Log.w(TAG, "Attempting to save playlist when local playlist " +
                    "is not loaded or not modified: playlist id=[" + playlistId + "]");
            return;
        }

        final List<LocalItem> items = itemListAdapter.getItemsList();
        List<Long> streamIds = new ArrayList<>(items.size());
        for (final LocalItem item : items) {
            if (item instanceof PlaylistStreamEntry) {
                streamIds.add(((PlaylistStreamEntry) item).streamId);
            }
        }

        Log.d(TAG, "Updating playlist id=[" + playlistId +
                "] with [" + streamIds.size() + "] items");

        final Disposable disposable = playlistManager.updateJoin(playlistId, streamIds)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action() {
                            @Override
                            public void run() throws Exception {
                                if (isModified != null) isModified.set(false);
                            }
                        },
                        this::onError
                );
        disposables.add(disposable);
    }


    private ItemTouchHelper.SimpleCallback getItemTouchCallback() {
        return new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                ItemTouchHelper.ACTION_STATE_IDLE) {
            @Override
            public int interpolateOutOfBoundsScroll(RecyclerView recyclerView, int viewSize,
                                                    int viewSizeOutOfBounds, int totalSize,
                                                    long msSinceStartScroll) {
                final int standardSpeed = super.interpolateOutOfBoundsScroll(recyclerView, viewSize,
                        viewSizeOutOfBounds, totalSize, msSinceStartScroll);
                final int minimumAbsVelocity = Math.max(MINIMUM_INITIAL_DRAG_VELOCITY,
                        Math.abs(standardSpeed));
                return minimumAbsVelocity * (int) Math.signum(viewSizeOutOfBounds);
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source,
                                  RecyclerView.ViewHolder target) {
                if (source.getItemViewType() != target.getItemViewType() ||
                        itemListAdapter == null) {
                    return false;
                }

                final int sourceIndex = source.getAdapterPosition();
                final int targetIndex = target.getAdapterPosition();
                final boolean isSwapped = itemListAdapter.swapItems(sourceIndex, targetIndex);
                if (isSwapped) saveChanges();
                return isSwapped;
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }

            @Override
            public boolean isItemViewSwipeEnabled() {
                return false;
            }

            @Override
            public void onSwiped(HiveRecyclerView.ViewHolder viewHolder, int swipeDir) {}
        };
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Utils
    //////////////////////////////////////////////////////////////////////////*/

    protected void showStreamItemDialog(final PlaylistStreamEntry item) {
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
                context.getResources().getString(R.string.set_as_playlist_thumbnail),
                context.getResources().getString(R.string.delete)
        };

        final DialogInterface.OnClickListener actions = (dialogInterface, i) -> {
            final int index = Math.max(itemListAdapter.getItemsList().indexOf(item), 0);
            switch (i) {
                case 0:
                    NavigationHelper.enqueueOnBackgroundPlayer(context,
                            new SinglePlayQueue(infoItem));
                    break;
                case 1:
                    NavigationHelper.enqueueOnPopupPlayer(activity, new
                            SinglePlayQueue(infoItem));
                    break;
                case 2:
                    NavigationHelper.playOnMainPlayer(context, getPlayQueue(index));
                    localOnePlayAll = true;
                    break;
                case 3:
                    NavigationHelper.playOnBackgroundPlayer(context, getPlayQueue(index));
                    break;
                case 4:
                    NavigationHelper.playOnPopupPlayer(activity, getPlayQueue(index));
                    break;
                case 5:
                    changeThumbnailUrl(item.thumbnailUrl);
                    break;
                case 6:
                    deleteItem(item);
                    break;
                default:
                    break;
            }
        };

        new InfoItemDialog(getActivity(), infoItem, commands, actions).show();
    }

    private void showWindows(final PlaylistStreamEntry item) {
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
                deleteItem(item);
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



    private void setInitialData(long playlistId, String name) {
        this.playlistId = playlistId;
        this.name = !TextUtils.isEmpty(name) ? name : "";
    }

    private void setVideoCount(final long count) {
        if (activity != null && headerStreamCount != null) {
            headerStreamCount.setText(Localization.localizeStreamCount(activity, count));
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
            if (item instanceof PlaylistStreamEntry) {
                streamInfoItems.add(((PlaylistStreamEntry) item).toStreamInfoItem());
            }
        }
        return new SinglePlayQueue(streamInfoItems, index);
    }
    View.OnKeyListener onKeyListener=new View.OnKeyListener() {
        @Override
        public boolean onKey(View view, int keyCode, KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if(view.getId()==R.id.playlist_ctrl_play_all_button && keyCode==KeyEvent.KEYCODE_DPAD_UP){
                   MainFragment.layout_collection.setFocusable(true);
                   MainFragment.layout_collection.requestFocus();
                }else if(view.getId()==R.id.playlist_ctrl_play_all_button && keyCode==KeyEvent.KEYCODE_DPAD_DOWN){
                    if(itemsList!=null&&itemsList.getChildAt(0)!=null){
                        clickPosition=0;
                        itemListAdapter.notifyDataSetChanged();
                    }

                }else if(view.getId()==R.id.playlist_ctrl_play_all_button && keyCode==KeyEvent.KEYCODE_BACK){
                    MainFragment.layout_collection.setFocusable(true);
                    MainFragment.layout_collection.requestFocus();
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


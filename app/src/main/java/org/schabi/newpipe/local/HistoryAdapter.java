package org.schabi.newpipe.local;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import org.schabi.newpipe.R;
import org.schabi.newpipe.database.LocalItem;
import org.schabi.newpipe.database.stream.StreamStatisticsEntry;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.fragments.MainFragment;
import org.schabi.newpipe.local.history.StatisticsPlaylistFragment;
import org.schabi.newpipe.local.holder.LocalItemHolder;
import org.schabi.newpipe.local.holder.LocalPlaylistItemHolder;
import org.schabi.newpipe.local.holder.LocalPlaylistStreamItemHolder;
import org.schabi.newpipe.local.holder.LocalStatisticStreamItemHolder;
import org.schabi.newpipe.local.holder.RemotePlaylistItemHolder;
import org.schabi.newpipe.util.FallbackViewHolder;
import org.schabi.newpipe.util.ImageDisplayConstants;
import org.schabi.newpipe.util.Localization;
import org.schabi.newpipe.util.OnClickGesture;
import org.schabi.newpipe.views.HiveRecyclerView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

/*
 * Created by Christian Schabesberger on 01.08.16.
 *
 * Copyright (C) Christian Schabesberger 2016 <chris.schabesberger@mailbox.org>
 * InfoListAdapter.java is part of NewPipe.
 *
 * NewPipe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe.  If not, see <http://www.gnu.org/licenses/>.
 */

public class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    private static final String TAG = HistoryAdapter.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final int HEADER_TYPE = 0;
    private static final int FOOTER_TYPE = 1;

    private static final int STREAM_STATISTICS_HOLDER_TYPE = 0x1000;
    private static final int STREAM_PLAYLIST_HOLDER_TYPE = 0x1001;
    private static final int LOCAL_PLAYLIST_HOLDER_TYPE = 0x2000;
    private static final int REMOTE_PLAYLIST_HOLDER_TYPE = 0x2001;

    private final LocalItemBuilder localItemBuilder;
    private final ArrayList<LocalItem> localItems;
    private final DateFormat dateFormat;

    private boolean showFooter = false;
    private View header = null;
    private View footer = null;
    public static PopupWindow mPopupWindow;
    private Activity activity;
    private View popupWindowView;
    /**
     * 正常模式加载图标，特殊模式不加载图标
     */
   /* private boolean isLoadIconURL = true;
    private ReceiverSlidehistory receiverSlidehistory;*/
    public HistoryAdapter(Activity activity) {
        localItemBuilder = new LocalItemBuilder(activity);
        localItems = new ArrayList<>();
        dateFormat = DateFormat.getDateInstance(DateFormat.SHORT,
                Localization.getPreferredLocale(activity));
        this.activity=activity;
        popupWindowView = LayoutInflater.from(activity).inflate(R.layout.layout_tip, null);
        mPopupWindow = new PopupWindow(popupWindowView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,false);
        mPopupWindow.setBackgroundDrawable(new BitmapDrawable());
        mPopupWindow.setAnimationStyle(android.R.style.Animation_Dialog);
        //注册广播
      /*  receiverSlidehistory = new ReceiverSlidehistory();
        //实例化过滤器并设置要过滤的广播
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("org.schabi.newpipe.slidehistory");
        activity.registerReceiver(receiverSlidehistory, intentFilter);*/
    }

    public void setSelectedListener(OnClickGesture<LocalItem> listener) {
        localItemBuilder.setOnItemSelectedListener(listener);
    }

    public void unsetSelectedListener() {
        localItemBuilder.setOnItemSelectedListener(null);
    }

    public void addItems(List<? extends LocalItem> data) {
        if (data != null) {
            if (DEBUG) {
                Log.d(TAG, "addItems() before > localItems.size() = " +
                        localItems.size() + ", data.size() = " + data.size());
            }

            int offsetStart = sizeConsideringHeader();
            localItems.addAll(data);

            if (DEBUG) {
                Log.d(TAG, "addItems() after > offsetStart = " + offsetStart +
                        ", localItems.size() = " + localItems.size() +
                        ", header = " + header + ", footer = " + footer +
                        ", showFooter = " + showFooter);
            }

            notifyItemRangeInserted(offsetStart, data.size());

            if (footer != null && showFooter) {
                int footerNow = sizeConsideringHeader();
                notifyItemMoved(offsetStart, footerNow);

                if (DEBUG) Log.d(TAG, "addItems() footer from " + offsetStart +
                        " to " + footerNow);
            }
        }
    }

    public void removeItem(final LocalItem data) {
        final int index = localItems.indexOf(data);

        localItems.remove(index);
        notifyItemRemoved(index + (header != null ? 1 : 0));
    }

    public boolean swapItems(int fromAdapterPosition, int toAdapterPosition) {
        final int actualFrom = adapterOffsetWithoutHeader(fromAdapterPosition);
        final int actualTo = adapterOffsetWithoutHeader(toAdapterPosition);

        if (actualFrom < 0 || actualTo < 0) return false;
        if (actualFrom >= localItems.size() || actualTo >= localItems.size()) return false;

        localItems.add(actualTo, localItems.remove(actualFrom));
        notifyItemMoved(fromAdapterPosition, toAdapterPosition);
        return true;
    }

    public void clearStreamItemList() {
        if (localItems.isEmpty()) {
            return;
        }
        localItems.clear();
        notifyDataSetChanged();
    }

    public void setHeader(View header) {
        boolean changed = header != this.header;
        this.header = header;
        if (changed) notifyDataSetChanged();
    }

    public void setFooter(View view) {
        this.footer = view;
    }

    public void showFooter(boolean show) {
        if (DEBUG) Log.d(TAG, "showFooter() called with: show = [" + show + "]");
        if (show == showFooter) return;

        showFooter = show;
        if (show) notifyItemInserted(sizeConsideringHeader());
        else notifyItemRemoved(sizeConsideringHeader());
    }

    private int adapterOffsetWithoutHeader(final int offset) {
        return offset - (header != null ? 1 : 0);
    }

    private int sizeConsideringHeader() {
        return localItems.size() + (header != null ? 1 : 0);
    }

    public ArrayList<LocalItem> getItemsList() {
        return localItems;
    }

    @Override
    public int getItemCount() {
        int count = localItems.size();
        if (header != null) count++;
        if (footer != null && showFooter) count++;

        if (DEBUG) {
            Log.d(TAG, "getItemCount() called, count = " + count +
                    ", localItems.size() = " + localItems.size() +
                    ", header = " + header + ", footer = " + footer +
                    ", showFooter = " + showFooter);
        }
        return count;
    }

    @Override
    public int getItemViewType(int position) {
        if (DEBUG) Log.d(TAG, "getItemViewType() called with: position = [" + position + "]");

        if (header != null && position == 0) {
            return HEADER_TYPE;
        } else if (header != null) {
            position--;
        }
        if (footer != null && position == localItems.size() && showFooter) {
            return FOOTER_TYPE;
        }
        final LocalItem item = localItems.get(position);

        switch (item.getLocalItemType()) {
            case PLAYLIST_LOCAL_ITEM: return LOCAL_PLAYLIST_HOLDER_TYPE;
            case PLAYLIST_REMOTE_ITEM: return REMOTE_PLAYLIST_HOLDER_TYPE;

            case PLAYLIST_STREAM_ITEM: return STREAM_PLAYLIST_HOLDER_TYPE;
            case STATISTIC_STREAM_ITEM: return STREAM_STATISTICS_HOLDER_TYPE;
            default:
                Log.e(TAG, "No holder type has been considered for item: [" +
                        item.getLocalItemType() + "]");
                return -1;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
        if (DEBUG) Log.d(TAG, "onCreateViewHolder() called with: parent = [" +
                parent + "], type = [" + type + "]");
        switch (type) {
            case HEADER_TYPE:
                return new HeaderFooterHolder(header);
            case FOOTER_TYPE:
                return new HeaderFooterHolder(footer);
            case LOCAL_PLAYLIST_HOLDER_TYPE:
                return new LocalPlaylistItemHolder(localItemBuilder, parent);
            case REMOTE_PLAYLIST_HOLDER_TYPE:
                return new RemotePlaylistItemHolder(localItemBuilder, parent);
            case STREAM_PLAYLIST_HOLDER_TYPE:
                return new LocalPlaylistStreamItemHolder(localItemBuilder, parent);
            case STREAM_STATISTICS_HOLDER_TYPE:
                return new LocalStatisticStreamItemHolder(localItemBuilder, parent);
            default:
                Log.e(TAG, "No view type has been considered for holder: [" + type + "]");
                return new FallbackViewHolder(new View(parent.getContext()));
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (DEBUG) Log.d(TAG, "onBindViewHolder() called with: holder = [" +
                holder.getClass().getSimpleName() + "], position = [" + position + "]");
        int positionAction=position;
        if (holder instanceof LocalItemHolder) {
            // If header isn't null, offset the items by -1
            if (header != null) position--;

            ((LocalItemHolder) holder).updateFromItem(localItems.get(position), dateFormat);
        } else if (holder instanceof HeaderFooterHolder && position == 0 && header != null) {
            ((HeaderFooterHolder) holder).view = header;
        } else if (holder instanceof HeaderFooterHolder && position == sizeConsideringHeader()
                && footer != null && showFooter) {
            ((HeaderFooterHolder) holder).view = footer;
        }
        //删除焦点回落
        int pos= StatisticsPlaylistFragment.deleteLocation;
        if(pos!=-1){
            if(pos==1&&getItemCount()==2){
                if(positionAction==1){
                    holder.itemView.setFocusable(true);
                    holder.itemView.requestFocus();
                    pos=StatisticsPlaylistFragment.deleteLocation=-1;
                }
            }else{
                if(getItemCount()>1&&pos<getItemCount()-1&&pos>-1&&position==pos){
                    holder.itemView.setFocusable(true);
                    holder.itemView.requestFocus();
                    pos=StatisticsPlaylistFragment.deleteLocation=-1;
                }else if(getItemCount()>1&&pos>getItemCount()-2){
                    pos=getItemCount()-2;
                    if(position==pos){
                        holder.itemView.setFocusable(true);
                        holder.itemView.requestFocus();
                        pos=StatisticsPlaylistFragment.deleteLocation=-1;
                    }

                }
            }
        }

        //点击焦点回落
        int clickPos=StatisticsPlaylistFragment.clickPosition;
        if(clickPos!=-1){
            if(clickPos==0){
                if(positionAction==1){
                    holder.itemView.setFocusable(true);
                    holder.itemView.requestFocus();
                    if(mPopupWindow!=null&&!mPopupWindow.isShowing()){
                        showWindows();
                    }
                    clickPos=StatisticsPlaylistFragment.clickPosition=-1;
                }
            }else {
                if(getItemCount()>1&&clickPos<getItemCount()&&clickPos>-1&&position==clickPos){
                    holder.itemView.setFocusable(true);
                    holder.itemView.requestFocus();
                    if(mPopupWindow!=null&&!mPopupWindow.isShowing()){
                        showWindows();
                    }
                    clickPos=StatisticsPlaylistFragment.clickPosition=-1;
                }else if(getItemCount()>1&&clickPos>getItemCount()-2){
                    clickPos=getItemCount()-2;
                    if(position==clickPos){
                        holder.itemView.setFocusable(true);
                        holder.itemView.requestFocus();
                        if(mPopupWindow!=null&&!mPopupWindow.isShowing()){
                            showWindows();
                        }
                        clickPos=StatisticsPlaylistFragment.clickPosition=-1;
                    }
                }
            }
        }


        holder.itemView.setOnKeyListener(onKeyListener);
        holder.itemView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(b){
                   if(mPopupWindow!=null&&!mPopupWindow.isShowing()){
                       showWindows();
                   }
                }
            }
        });
    }
    View.OnKeyListener onKeyListener=new View.OnKeyListener() {
        @Override
        public boolean onKey(View view, int keyCode, KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if(keyCode==KeyEvent.KEYCODE_DPAD_LEFT){//itemRoot
                    int[] location = new  int[2] ;
                    view.getLocationInWindow(location);
                    if(location[0]<100){
                        return true;
                    }
                }else if(keyCode==KeyEvent.KEYCODE_BACK){
                    MainFragment.layout_history.setFocusable(true);
                    MainFragment.layout_history.requestFocus();
                    HistorygridFragment.itemsList.smoothScrollToPosition(0);
                    return true;
                }
            }
            return false;
        }
    };
    private void showWindows() {
        mPopupWindow.showAtLocation(popupWindowView, Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 0);

    }
    /*public class LocalStatisticStreamItemHolder extends HistoryLocalItemHolder {

        public final ImageView itemThumbnailView;
        public final TextView itemVideoTitleView;
        public final TextView itemUploaderView;
        public final TextView itemDurationView;
        public final TextView itemAdditionalDetails;

        public LocalStatisticStreamItemHolder(LocalItemBuilder infoItemBuilder, ViewGroup parent) {
            super(infoItemBuilder, R.layout.list_stream_item, parent);

            itemThumbnailView = itemView.findViewById(R.id.itemThumbnailView);
            itemVideoTitleView = itemView.findViewById(R.id.itemVideoTitleView);
            itemUploaderView = itemView.findViewById(R.id.itemUploaderView);
            itemDurationView = itemView.findViewById(R.id.itemDurationView);
            itemAdditionalDetails = itemView.findViewById(R.id.itemAdditionalDetails);
        }

        private String getStreamInfoDetailLine(final StreamStatisticsEntry entry,
                                               final DateFormat dateFormat) {
            final String watchCount = Localization.shortViewCount(itemBuilder.getContext(),
                    entry.watchCount);
            final String uploadDate = dateFormat.format(entry.latestAccessDate);
            final String serviceName = NewPipe.getNameOfService(entry.serviceId);
            return Localization.concatenateStrings(watchCount, uploadDate, serviceName);
        }

        @Override
        public void updateFromItem(final LocalItem localItem, final DateFormat dateFormat) {
            if (!(localItem instanceof StreamStatisticsEntry)) return;
            final StreamStatisticsEntry item = (StreamStatisticsEntry) localItem;

            itemVideoTitleView.setText(item.title);
            itemUploaderView.setText(item.uploader);

            if (item.duration > 0) {
                itemDurationView.setText(Localization.getDurationString(item.duration));
                itemDurationView.setBackgroundColor(ContextCompat.getColor(itemBuilder.getContext(),
                        R.color.duration_background_color));
                itemDurationView.setVisibility(View.VISIBLE);
            } else {
                itemDurationView.setVisibility(View.GONE);
            }

            itemAdditionalDetails.setText(getStreamInfoDetailLine(item, dateFormat));

            // Default thumbnail is shown on error, while loading and if the url is empty
            if(isLoadIconURL){
                itemBuilder.displayImage(item.thumbnailUrl, itemThumbnailView,
                        ImageDisplayConstants.DISPLAY_THUMBNAIL_OPTIONS);
            }else{
                itemThumbnailView.setImageResource(R.drawable.dummy_thumbnail);
            }


            itemView.setOnClickListener(view -> {
                if (itemBuilder.getOnItemSelectedListener() != null) {
                    itemBuilder.getOnItemSelectedListener().selected(item);
                }
            });

            itemView.setLongClickable(true);
            itemView.setOnLongClickListener(view -> {
                if (itemBuilder.getOnItemSelectedListener() != null) {
                    itemBuilder.getOnItemSelectedListener().held(item);
                }
                return true;
            });
        }
    }
    public abstract class HistoryLocalItemHolder extends HiveRecyclerView.ViewHolder {
        protected final LocalItemBuilder itemBuilder;

        public HistoryLocalItemHolder(LocalItemBuilder itemBuilder, int layoutId, ViewGroup parent) {
            super(LayoutInflater.from(itemBuilder.getContext())
                    .inflate(layoutId, parent, false));
            this.itemBuilder = itemBuilder;
        }

        public abstract void updateFromItem(final LocalItem item, final DateFormat dateFormat);
    }
    public class ReceiverSlidehistory extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean b=intent.getBooleanExtra("boolean",false);
            if(!isLoadIconURL && b){
                notifyItemRangeChanged(0,getItemCount());
            }
            isLoadIconURL=b;


        }

    }
    *//**
     * 取消注册
     *//*
    public void unRegister(){
        try{
            if(receiverSlidehistory!=null){
                activity.unregisterReceiver(receiverSlidehistory);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
*/

}

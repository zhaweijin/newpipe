package org.schabi.newpipe.info_list;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.schabi.newpipe.R;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.channel.ChannelInfoItem;
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamType;
import org.schabi.newpipe.fragments.list.SearchBaseListFragment;
import org.schabi.newpipe.info_list.holder.ChannelInfoItemHolder;
import org.schabi.newpipe.info_list.holder.ChannelMiniInfoItemHolder;
import org.schabi.newpipe.info_list.holder.InfoItemHolder;
import org.schabi.newpipe.info_list.holder.PlaylistInfoItemHolder;
import org.schabi.newpipe.info_list.holder.PlaylistMiniInfoItemHolder;
import org.schabi.newpipe.info_list.holder.StreamMiniInfoItemHolder;
import org.schabi.newpipe.util.FallbackViewHolder;
import org.schabi.newpipe.util.ImageDisplayConstants;
import org.schabi.newpipe.util.Localization;
import org.schabi.newpipe.util.OnClickGesture;
import org.schabi.newpipe.views.HiveRecyclerView;

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

public class SearchInfoListAdapter extends HiveRecyclerView.Adapter<HiveRecyclerView.ViewHolder> {
    private static final String TAG = SearchInfoListAdapter.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final int HEADER_TYPE = 0;
    private static final int FOOTER_TYPE = 1;

    private static final int MINI_STREAM_HOLDER_TYPE = 0x100;
    private static final int STREAM_HOLDER_TYPE = 0x101;
    private static final int MINI_CHANNEL_HOLDER_TYPE = 0x200;
    private static final int CHANNEL_HOLDER_TYPE = 0x201;
    private static final int MINI_PLAYLIST_HOLDER_TYPE = 0x300;
    private static final int PLAYLIST_HOLDER_TYPE = 0x301;

    private final InfoItemBuilder infoItemBuilder;
    private final ArrayList<InfoItem> infoItemList;
    private boolean useMiniVariant = false;
    private boolean showFooter = false;
    private View header = null;
    private View footer = null;
    /**
     * 正常模式加载图标，特殊模式不加载图标
     */
    private boolean isLoadIconURL = true;
    private ReceiverSlidefind receiverSlidefind;
    private Activity activity;

    public class HFHolder extends HiveRecyclerView.ViewHolder {
        public View view;

        public HFHolder(View v) {
            super(v);
            view = v;
        }
    }

    public SearchInfoListAdapter(Activity a) {
        infoItemBuilder = new InfoItemBuilder(a);
        infoItemList = new ArrayList<>();
        this.activity=a;
        //注册广播
        receiverSlidefind = new ReceiverSlidefind();
        //实例化过滤器并设置要过滤的广播
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("org.schabi.newpipe.slidefind");
        a.registerReceiver(receiverSlidefind, intentFilter);
    }

    public void setOnStreamSelectedListener(OnClickGesture<StreamInfoItem> listener) {
        infoItemBuilder.setOnStreamSelectedListener(listener);
    }

    public void setOnChannelSelectedListener(OnClickGesture<ChannelInfoItem> listener) {
        infoItemBuilder.setOnChannelSelectedListener(listener);
    }

    public void setOnPlaylistSelectedListener(OnClickGesture<PlaylistInfoItem> listener) {
        infoItemBuilder.setOnPlaylistSelectedListener(listener);
    }

    public void useMiniItemVariants(boolean useMiniVariant) {
        this.useMiniVariant = useMiniVariant;
    }

    public void addInfoItemList(List<InfoItem> data) {
        if (data != null) {
            if (DEBUG) {
                Log.d(TAG, "addInfoItemList() before > infoItemList.size() = " + infoItemList.size() + ", data.size() = " + data.size());
            }

            int offsetStart = sizeConsideringHeaderOffset();
            infoItemList.addAll(data);

            if (DEBUG) {
                Log.d(TAG, "addInfoItemList() after > offsetStart = " + offsetStart + ", infoItemList.size() = " + infoItemList.size() + ", header = " + header + ", footer = " + footer + ", showFooter = " + showFooter);
            }

            notifyItemRangeInserted(offsetStart, data.size());

           if (footer != null && showFooter) {
                int footerNow = sizeConsideringHeaderOffset();
                notifyItemMoved(offsetStart, footerNow);

                if (DEBUG) Log.d(TAG, "addInfoItemList() footer from " + offsetStart + " to " + footerNow);
            }
        }
    }

    public void addInfoItem(InfoItem data) {
        if (data != null) {
            if (DEBUG) {
                Log.d(TAG, "addInfoItem() before > infoItemList.size() = " + infoItemList.size() + ", thread = " + Thread.currentThread());
            }

            int positionInserted = sizeConsideringHeaderOffset();
            infoItemList.add(data);

            if (DEBUG) {
                Log.d(TAG, "addInfoItem() after > position = " + positionInserted + ", infoItemList.size() = " + infoItemList.size() + ", header = " + header + ", footer = " + footer + ", showFooter = " + showFooter);
            }
            notifyItemInserted(positionInserted);

            if (footer != null && showFooter) {
                int footerNow = sizeConsideringHeaderOffset();
                notifyItemMoved(positionInserted, footerNow);

                if (DEBUG) Log.d(TAG, "addInfoItem() footer from " + positionInserted + " to " + footerNow);
            }
        }
    }

    public void clearStreamItemList() {
        if (infoItemList.isEmpty()) {
            return;
        }
        infoItemList.clear();
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
        if (show) notifyItemInserted(sizeConsideringHeaderOffset());
        else notifyItemRemoved(sizeConsideringHeaderOffset());
    }


    private int sizeConsideringHeaderOffset() {
        int i = infoItemList.size() + (header != null ? 1 : 0);
        //int i = infoItemList.size();
        if (DEBUG) Log.d(TAG, "sizeConsideringHeaderOffset() called → " + i);
        return i;
    }

    public ArrayList<InfoItem> getItemsList() {
        return infoItemList;
    }

    @Override
    public int getItemCount() {
        int count = infoItemList.size();
        if (header != null) count++;
        if (footer != null && showFooter) count++;

        if (DEBUG) {
            Log.d(TAG, "getItemCount() called, count = " + count + ", infoItemList.size() = " + infoItemList.size() + ", header = " + header + ", footer = " + footer + ", showFooter = " + showFooter);
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
        if (footer != null && position == infoItemList.size() && showFooter) {
            return FOOTER_TYPE;
        }
        final InfoItem item = infoItemList.get(position);
        switch (item.getInfoType()) {
            case STREAM:
                return useMiniVariant ? MINI_STREAM_HOLDER_TYPE : STREAM_HOLDER_TYPE;
            case CHANNEL:
                return useMiniVariant ? MINI_CHANNEL_HOLDER_TYPE : CHANNEL_HOLDER_TYPE;
            case PLAYLIST:
                return useMiniVariant ? MINI_PLAYLIST_HOLDER_TYPE : PLAYLIST_HOLDER_TYPE;
            default:
                Log.e(TAG, "Trollolo");
                return -1;
        }
    }

    @Override
    public HiveRecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
        if (DEBUG) Log.d(TAG, "onCreateViewHolder() called with: parent = [" + parent + "], type = [" + type + "]");
        switch (type) {
            case HEADER_TYPE:
                return new HFHolder(header);
            case FOOTER_TYPE:
                return new HFHolder(footer);
            case MINI_STREAM_HOLDER_TYPE:
                return new StreamMiniInfoItemHolder(infoItemBuilder, parent);
            case STREAM_HOLDER_TYPE:
                return new SearchStreamInfoItemHolder(infoItemBuilder, parent);
            case MINI_CHANNEL_HOLDER_TYPE:
                return new ChannelMiniInfoItemHolder(infoItemBuilder, parent);
            case CHANNEL_HOLDER_TYPE:
                return new ChannelInfoItemHolder(infoItemBuilder, parent);
            case MINI_PLAYLIST_HOLDER_TYPE:
                return new PlaylistMiniInfoItemHolder(infoItemBuilder, parent);
            case PLAYLIST_HOLDER_TYPE:
                return new PlaylistInfoItemHolder(infoItemBuilder, parent);
            default:
                Log.e(TAG, "Trollolo");
                return new FallbackViewHolder(new View(parent.getContext()));
        }
    }

    @Override
    public void onBindViewHolder(HiveRecyclerView.ViewHolder holder, int position) {
        if (DEBUG) Log.d(TAG, "onBindViewHolder() called with: holder = [" + holder.getClass().getSimpleName() + "], position = [" + position + "]");
        if (holder instanceof InfoItemHolder) {
            // If header isn't null, offset the items by -1
            if (header != null) position--;

            ((InfoItemHolder) holder).updateFromItem(infoItemList.get(position));
        } else if (holder instanceof HFHolder && position == 0 && header != null) {
            ((HFHolder) holder).view = header;
        } else if (holder instanceof HFHolder && position == sizeConsideringHeaderOffset() && footer != null && showFooter) {
            ((HFHolder) holder).view = footer;
        }
        //点击焦点回落的处理
        int clickPos= SearchBaseListFragment.clickPosition;
        if(getItemCount()>0&&clickPos<getItemCount()&&clickPos>-1&&position==clickPos){
            holder.itemView.setFocusable(true);
            holder.itemView.requestFocus();
            clickPos=SearchBaseListFragment.clickPosition=-1;
        }else if(getItemCount()>0&&clickPos>getItemCount()-1){
            clickPos=getItemCount()-1;
            if(position==clickPos){
                holder.itemView.setFocusable(true);
                holder.itemView.requestFocus();
                clickPos=SearchBaseListFragment.clickPosition=-1;
            }

        }


    }
    public class SearchStreamInfoItemHolder extends SearchStreamMiniInfoItemHolder {

        public final TextView itemAdditionalDetails;

        public SearchStreamInfoItemHolder(InfoItemBuilder infoItemBuilder, ViewGroup parent) {
            super(infoItemBuilder, R.layout.list_stream_item_search, parent);
            itemAdditionalDetails = itemView.findViewById(R.id.itemAdditionalDetails);
        }

        @Override
        public void updateFromItem(final InfoItem infoItem) {
            super.updateFromItem(infoItem);

            if (!(infoItem instanceof StreamInfoItem)) return;
            final StreamInfoItem item = (StreamInfoItem) infoItem;

            itemAdditionalDetails.setText(getStreamInfoDetailLine(item));
        }

        private String getStreamInfoDetailLine(final StreamInfoItem infoItem) {
            String viewsAndDate = "";
            if (infoItem.getViewCount() >= 0) {
                viewsAndDate = Localization.shortViewCount(itemBuilder.getContext(), infoItem.getViewCount());
            }
            if (!TextUtils.isEmpty(infoItem.getUploadDate())) {
                if (viewsAndDate.isEmpty()) {
                    viewsAndDate = infoItem.getUploadDate();
                } else {
                    viewsAndDate += " • " + infoItem.getUploadDate();
                }
            }
            return viewsAndDate;
        }
    }
    public class SearchStreamMiniInfoItemHolder extends InfoItemHolder {

        public final ImageView itemThumbnailView;
        public final TextView itemVideoTitleView;
        public final TextView itemUploaderView;
        public final TextView itemDurationView;

        SearchStreamMiniInfoItemHolder(InfoItemBuilder infoItemBuilder, int layoutId, ViewGroup parent) {
            super(infoItemBuilder, layoutId, parent);

            itemThumbnailView = itemView.findViewById(R.id.itemThumbnailView);
            itemVideoTitleView = itemView.findViewById(R.id.itemVideoTitleView);
            itemUploaderView = itemView.findViewById(R.id.itemUploaderView);
            itemDurationView = itemView.findViewById(R.id.itemDurationView);
        }

        public SearchStreamMiniInfoItemHolder(InfoItemBuilder infoItemBuilder, ViewGroup parent) {
            this(infoItemBuilder, R.layout.list_stream_mini_item, parent);
        }

        @Override
        public void updateFromItem(final InfoItem infoItem) {
            if (!(infoItem instanceof StreamInfoItem)) return;
            final StreamInfoItem item = (StreamInfoItem) infoItem;

            itemVideoTitleView.setText(item.getName());
            itemUploaderView.setText(item.getUploaderName());

            if (item.getDuration() > 0) {
                itemDurationView.setText(Localization.getDurationString(item.getDuration()));
                itemDurationView.setBackgroundColor(ContextCompat.getColor(itemBuilder.getContext(),
                        R.color.duration_background_color));
                itemDurationView.setVisibility(View.VISIBLE);
            } else if (item.getStreamType() == StreamType.LIVE_STREAM) {
                itemDurationView.setText(R.string.duration_live);
                itemDurationView.setBackgroundColor(ContextCompat.getColor(itemBuilder.getContext(),
                        R.color.live_duration_background_color));
                itemDurationView.setVisibility(View.VISIBLE);
            } else {
                itemDurationView.setVisibility(View.GONE);
            }

            // Default thumbnail is shown on error, while loading and if the url is empty
            if(isLoadIconURL){
                itemBuilder.getImageLoader()
                        .displayImage(item.getThumbnailUrl(),
                                itemThumbnailView,
                                ImageDisplayConstants.DISPLAY_THUMBNAIL_OPTIONS);
            }else{
                    itemThumbnailView.setImageResource(R.drawable.dummy_thumbnail);
            }


            itemView.setOnClickListener(view -> {
                if (itemBuilder.getOnStreamSelectedListener() != null) {
                    itemBuilder.getOnStreamSelectedListener().selected(item);
                }
            });

            switch (item.getStreamType()) {
                case AUDIO_STREAM:
                case VIDEO_STREAM:
                case LIVE_STREAM:
                case AUDIO_LIVE_STREAM:
                    enableLongClick(item);
                    break;
                case FILE:
                case NONE:
                default:
                    disableLongClick();
                    break;
            }
        }

        private void enableLongClick(final StreamInfoItem item) {
            itemView.setLongClickable(true);
            itemView.setOnLongClickListener(view -> {
                if (itemBuilder.getOnStreamSelectedListener() != null) {
                    itemBuilder.getOnStreamSelectedListener().held(item);
                }
                return true;
            });
        }

        private void disableLongClick() {
            itemView.setLongClickable(false);
            itemView.setOnLongClickListener(null);
        }
    }
    public class ReceiverSlidefind extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean b=intent.getBooleanExtra("boolean",false);
            if(!isLoadIconURL && b){
                notifyItemRangeChanged(0,getItemCount());
            }
            isLoadIconURL=b;


        }

    }
    /**
     * 取消注册
     */
    public void unRegister(){
        try{
            if(receiverSlidefind!=null){
                activity.unregisterReceiver(receiverSlidefind);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


}

package org.schabi.newpipe.fragments.list.search;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.AttrRes;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.schabi.newpipe.R;
import org.schabi.newpipe.views.HiveRecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SuggestionListAdapter extends HiveRecyclerView.Adapter<SuggestionListAdapter.SuggestionItemHolder> {
    private final ArrayList<SuggestionItem> items = new ArrayList<>();
    private final Context context;
    private OnSuggestionItemSelected listener;
    private boolean showSuggestionHistory = true;

    public interface OnSuggestionItemSelected {
        void onSuggestionItemSelected(SuggestionItem item);
        void onSuggestionItemInserted(SuggestionItem item);
        void onSuggestionItemLongClick(SuggestionItem item);
    }

    public SuggestionListAdapter(Context context) {
        this.context = context;
    }

    public void setItems(List<SuggestionItem> items) {
        this.items.clear();
        this.items.addAll(items);
        /*if (showSuggestionHistory) {
            this.items.addAll(items);
        } else {
            // remove history items if history is disabled
            for (SuggestionItem item : items) {
                if (!item.fromHistory) {
                    this.items.add(item);
                }
            }
        }*/
        notifyDataSetChanged();
    }

    public void setListener(OnSuggestionItemSelected listener) {
        this.listener = listener;
    }

    public void setShowSuggestionHistory(boolean v) {
        showSuggestionHistory = v;
    }

    @Override
    public SuggestionItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new SuggestionItemHolder(LayoutInflater.from(context).inflate(R.layout.item_search_suggestion, parent, false));
    }

    @Override
    public void onBindViewHolder(SuggestionItemHolder holder, int position) {
        final SuggestionItem currentItem = getItem(position);
        holder.updateFrom(currentItem);
        holder.suggestion_search_layout.setOnClickListener(v -> {
            if (listener != null) listener.onSuggestionItemSelected(currentItem);
        });
       /* holder.suggestion_search_layout.setOnLongClickListener(v -> {
                if (listener != null) listener.onSuggestionItemLongClick(currentItem);
                return true;
        });*/
        holder.insertView.setOnClickListener(v -> {
            if (listener != null) listener.onSuggestionItemInserted(currentItem);
        });
        if(SearchFragment.downPosition==0&&position==0){
            SearchFragment.suggestionsRecyclerView.setFocusable(true);
            holder.itemView.setFocusable(true);
            holder.itemView.requestFocus();
            SearchFragment.downPosition=-1;
        }

    }

    private SuggestionItem getItem(int position) {
        return items.get(position);
    }

    @Override
    public int getItemCount() {
        return items.size()>4? 4 : items.size();
    }

    public boolean isEmpty() {
        return getItemCount() == 0;
    }

    public static class SuggestionItemHolder extends HiveRecyclerView.ViewHolder {
        private final TextView itemSuggestionQuery;
        private final ImageView suggestionIcon;
        private final RelativeLayout suggestion_search_layout;
        private final View queryView;
        private final View insertView;

        // Cache some ids, as they can potentially be constantly updated/recycled
        private final int historyResId;
        private final int searchResId;

        private SuggestionItemHolder(View rootView) {
            super(rootView);
            suggestionIcon = rootView.findViewById(R.id.item_suggestion_icon);
            itemSuggestionQuery = rootView.findViewById(R.id.item_suggestion_query);

            suggestion_search_layout=rootView.findViewById(R.id.suggestion_search_layout);
            queryView = rootView.findViewById(R.id.suggestion_search);
            insertView = rootView.findViewById(R.id.suggestion_insert);

            historyResId = resolveResourceIdFromAttr(rootView.getContext(), R.attr.history);
            searchResId = resolveResourceIdFromAttr(rootView.getContext(), R.attr.search);
        }

        private void updateFrom(SuggestionItem item) {
            suggestionIcon.setImageResource(item.fromHistory ? historyResId : searchResId);
            itemSuggestionQuery.setText(item.query);
        }

        private static int resolveResourceIdFromAttr(Context context, @AttrRes int attr) {
            TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{attr});
            int attributeResourceId = a.getResourceId(0, 0);
            a.recycle();
            return attributeResourceId;
        }
    }
}

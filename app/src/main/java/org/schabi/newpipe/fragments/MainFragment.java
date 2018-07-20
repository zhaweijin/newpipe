package org.schabi.newpipe.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.schabi.newpipe.BaseFragment;
import org.schabi.newpipe.R;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.kiosk.KioskList;
import org.schabi.newpipe.fragments.list.channel.ChannelFragment;
import org.schabi.newpipe.local.HistoryAdapter;
import org.schabi.newpipe.local.LocalItemListAdapterNew;
import org.schabi.newpipe.local.feed.FeedFragment;
import org.schabi.newpipe.fragments.list.kiosk.KioskFragment;
import org.schabi.newpipe.local.history.StatisticsPlaylistFragment;
import org.schabi.newpipe.local.playlist.LocalPlaylistFragment;
import org.schabi.newpipe.local.subscription.SubscriptionFragment;
import org.schabi.newpipe.report.ErrorActivity;
import org.schabi.newpipe.report.UserAction;
import org.schabi.newpipe.util.KioskTranslator;
import org.schabi.newpipe.util.NavigationHelper;
import org.schabi.newpipe.util.ServiceHelper;
import org.schabi.newpipe.util.ThemeHelper;
import org.schabi.newpipe.views.HiveRecyclerViewGrid;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainFragment extends BaseFragment{

    public int currentServiceId = -1;
    //private ViewPager viewPager;

    /*//////////////////////////////////////////////////////////////////////////
    // Constants
    //////////////////////////////////////////////////////////////////////////*/

    private static final int FALLBACK_SERVICE_ID = ServiceList.YouTube.getServiceId();
    private static final String FALLBACK_CHANNEL_URL = "https://www.youtube.com/channel/UC-9-kyTW8ZkZNDHQJ6FgpwQ";
    private static final String FALLBACK_CHANNEL_NAME = "Music";
    private static final String FALLBACK_KIOSK_ID = "Trending";
    private static final int KIOSK_MENU_OFFSET = 2000;
    private SimpleDateFormat sdfWeek;
    private SimpleDateFormat sdfDate;
    private SimpleDateFormat sdfTime;
    public static LinearLayout titlelayout;
    private TextView week;
    private TextView date;
    private TextView time;
    public static LinearLayout btn_find;
    public static LinearLayout layout_recommend;
    public static LinearLayout layout_subscribe;
    public static LinearLayout layout_collection;
    public static LinearLayout layout_history;
    private ImageView img_recommend;
    private ImageView img_subscribe;
    private ImageView img_collection;
    private ImageView img_history;
    private FragmentManager fragmentManager;
    public static FragmentManager manager;
    public static int currenFragment=-1;
    public static boolean isFocus=true;

    /*//////////////////////////////////////////////////////////////////////////
    // Fragment's LifeCycle
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        currentServiceId = ServiceHelper.getSelectedServiceId(activity);
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    protected void initViews(View rootView, Bundle savedInstanceState) {
        super.initViews(rootView, savedInstanceState);
        fragmentManager = getChildFragmentManager();
        titlelayout=(LinearLayout)rootView.findViewById(R.id.titlelayout);
        titlelayout.setVisibility(View.VISIBLE);
        //主页显示系统当前时间
        week=rootView.findViewById(R.id.week);
        date=rootView.findViewById(R.id.date);
        time=rootView.findViewById(R.id.time);
        sdfWeek = new SimpleDateFormat("EEEE");
        sdfDate = new SimpleDateFormat("yyyy.MM.dd");
        sdfTime = new SimpleDateFormat("HH:mm");
        handler.postDelayed(runnable, 0);
        manager=getFragmentManager();


        btn_find=rootView.findViewById(R.id.btn_find);
        layout_recommend=rootView.findViewById(R.id.layout_recommend);
        layout_subscribe=rootView.findViewById(R.id.layout_subscribe);
        layout_collection=rootView.findViewById(R.id.layout_collection);
        layout_history=rootView.findViewById(R.id.layout_history);
        img_recommend=rootView.findViewById(R.id.img_recommend);
        img_subscribe=rootView.findViewById(R.id.img_subscribe);
        img_collection=rootView.findViewById(R.id.img_collection);
        img_history=rootView.findViewById(R.id.img_history);
        btn_find.setFocusable(false);//onFocusChangeListener
        btn_find.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(b){
                    layout_recommend.setBackgroundResource(R.drawable.btn_focus_mask);
                    layout_subscribe.setBackgroundResource(R.drawable.btn_focus_mask);
                    layout_collection.setBackgroundResource(R.drawable.btn_focus_mask);
                    layout_history.setBackgroundResource(R.drawable.btn_focus_mask);
                }else{
                    if(currenFragment==1){//订阅页
                        layout_subscribe.setBackgroundResource(R.drawable.masklayer);
                    }else if(currenFragment==3){//历史页
                        layout_history.setBackgroundResource(R.drawable.masklayer);
                    }else if(currenFragment==2){//收藏页
                        layout_collection.setBackgroundResource(R.drawable.masklayer);
                    }
                }
            }
        });
        layout_recommend.setOnFocusChangeListener(onFocusChangeListener);
        layout_subscribe.setOnFocusChangeListener(onFocusChangeListener);
        layout_collection.setOnFocusChangeListener(onFocusChangeListener);
        layout_history.setOnFocusChangeListener(onFocusChangeListener);
        layout_recommend.setFocusable(true);
        layout_recommend.requestFocus();
        btn_find.setOnKeyListener(onKeyListener);
        layout_recommend.setOnKeyListener(onKeyListener);
        layout_subscribe.setOnKeyListener(onKeyListener);
        layout_collection.setOnKeyListener(onKeyListener);
        layout_history.setOnKeyListener(onKeyListener);

        btn_find.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavigationHelper.openSearchFragment(getFragmentManager(), ServiceHelper.getSelectedServiceId(activity), "");
            }
        });


    }


    private class PagerAdapter extends FragmentPagerAdapter {
        PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return isSubscriptionsPageOnlySelected() ? new SubscriptionFragment() : getMainPageFragment();
                case 1:
                    if(PreferenceManager.getDefaultSharedPreferences(getActivity())
                            .getString(getString(R.string.main_page_content_key), getString(R.string.blank_page_key))
                            .equals(getString(R.string.subscription_page_key))) {
                        return new LocalPlaylistFragment();
                    } else {
                        return new FeedFragment();
                    }
                case 2:
                    return new LocalPlaylistFragment();
                case 3:
                    //NavigationHelper.openStatisticFragment(activity.getSupportFragmentManager());
                    return new StatisticsPlaylistFragment();
                default:
                    return new BlankFragment();
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            //return getString(this.tabTitles[position]);
            return "";
        }

        @Override
        public int getCount() {
            return isSubscriptionsPageOnlySelected() ? 2 : 4;
        }
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Main page content
    //////////////////////////////////////////////////////////////////////////*/

    private boolean isSubscriptionsPageOnlySelected() {
        return PreferenceManager.getDefaultSharedPreferences(activity)
                .getString(getString(R.string.main_page_content_key), getString(R.string.blank_page_key))
                .equals(getString(R.string.subscription_page_key));
    }

    private Fragment getMainPageFragment() {
        if (getActivity() == null) {
            currenFragment=-1;
            return new BlankFragment();
        }


        try {
            SharedPreferences preferences =
                    PreferenceManager.getDefaultSharedPreferences(getActivity());
            final String setMainPage = preferences.getString(getString(R.string.main_page_content_key),
                    getString(R.string.main_page_selectd_kiosk_id));
            if (setMainPage.equals(getString(R.string.blank_page_key))) {
                currenFragment=-1;
                return new BlankFragment();
            } else if (setMainPage.equals(getString(R.string.kiosk_page_key))) {
                int serviceId = preferences.getInt(getString(R.string.main_page_selected_service),
                        FALLBACK_SERVICE_ID);
                String kioskId = preferences.getString(getString(R.string.main_page_selectd_kiosk_id),
                        FALLBACK_KIOSK_ID);
                KioskFragment fragment = KioskFragment.getInstance(serviceId, kioskId);
                fragment.useAsFrontPage(true);
                return fragment;
            } else if (setMainPage.equals(getString(R.string.feed_page_key))) {
                FeedFragment fragment = new FeedFragment();
                fragment.useAsFrontPage(true);
                return fragment;
            } else if (setMainPage.equals(getString(R.string.channel_page_key))) {
                int serviceId = preferences.getInt(getString(R.string.main_page_selected_service),
                        FALLBACK_SERVICE_ID);
                String url = preferences.getString(getString(R.string.main_page_selected_channel_url),
                        FALLBACK_CHANNEL_URL);
                String name = preferences.getString(getString(R.string.main_page_selected_channel_name),
                        FALLBACK_CHANNEL_NAME);
                ChannelFragment fragment = ChannelFragment.getInstance(serviceId, url, name);
                fragment.useAsFrontPage(true);
                return fragment;
            } else {
                currenFragment=-1;
                return new BlankFragment();
            }

        } catch (Exception e) {
            ErrorActivity.reportError(activity, e,
                    activity.getClass(),
                    null,
                    ErrorActivity.ErrorInfo.make(UserAction.UI_ERROR,
                            "none", "", R.string.app_ui_crash));
            currenFragment=-1;
            return new BlankFragment();
        }
    }

    Handler handler = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            Date dateSys=new Date(System.currentTimeMillis());
            week.setText(sdfWeek.format(dateSys));
            date.setText(sdfDate.format(dateSys));
            time.setText(sdfTime.format(dateSys));
            handler.postDelayed(this, 60000);
        }
    };
    View.OnFocusChangeListener onFocusChangeListener = new View.OnFocusChangeListener(){
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if(hasFocus){
                if(LocalItemListAdapterNew.mPopupWindow!=null&&LocalItemListAdapterNew.mPopupWindow.isShowing()){
                    LocalItemListAdapterNew.mPopupWindow.dismiss();
                }
                if(HistoryAdapter.mPopupWindow!=null&&HistoryAdapter.mPopupWindow.isShowing()){
                    HistoryAdapter.mPopupWindow.dismiss();
                }
                FragmentTransaction transaction=fragmentManager.beginTransaction();
                switch (v.getId()){
                    case R.id.layout_recommend:
                        if(HiveRecyclerViewGrid.mToast!=null){
                            HiveRecyclerViewGrid.mToast.cancel();
                        }
                        if(!isFocus&&currenFragment!=0){
                            layout_recommend.setBackgroundResource(R.drawable.shape1);
                            img_recommend.setVisibility(View.INVISIBLE);
                        }else{
                            layout_recommend.setBackgroundResource(R.drawable.btn_focus_mask);
                            img_recommend.setVisibility(View.VISIBLE);
                        }
                        layout_subscribe.setBackgroundResource(R.drawable.btn_focus_mask);
                        layout_collection.setBackgroundResource(R.drawable.btn_focus_mask);
                        layout_history.setBackgroundResource(R.drawable.btn_focus_mask);

                        if(currenFragment!=0&&isFocus){
                            Fragment fragment_recommend= isSubscriptionsPageOnlySelected() ? new SubscriptionFragment() : getMainPageFragment();
                            transaction.replace(R.id.container, fragment_recommend);
                            currenFragment=0;
                        }
                        if(!isFocus){
                            if(currenFragment==1){
                                layout_subscribe.setFocusable(true);
                                layout_subscribe.requestFocus();
                                layout_subscribe.setBackgroundResource(R.drawable.masklayer);
                            }else if(currenFragment==2){
                                layout_collection.setBackgroundResource(R.drawable.masklayer);
                            }else if(currenFragment==3){
                                layout_history.setBackgroundResource(R.drawable.masklayer);
                            }
                        }
                        break;
                    case R.id.layout_subscribe:
                        layout_recommend.setBackgroundResource(R.drawable.btn_focus_mask);
                        layout_subscribe.setBackgroundResource(R.drawable.btn_focus_mask);
                        layout_collection.setBackgroundResource(R.drawable.btn_focus_mask);
                        layout_history.setBackgroundResource(R.drawable.btn_focus_mask);
                        img_subscribe.setVisibility(View.VISIBLE);
                        if(currenFragment!=1||!isFocus){
                            Fragment fragment_subscribe=new FeedFragment();
                            transaction.replace(R.id.container, fragment_subscribe);
                            currenFragment=1;
                            isFocus=true;
                        }
                        break;
                    case R.id.layout_collection:
                        layout_recommend.setBackgroundResource(R.drawable.btn_focus_mask);
                        layout_subscribe.setBackgroundResource(R.drawable.btn_focus_mask);
                        layout_collection.setBackgroundResource(R.drawable.btn_focus_mask);
                        layout_history.setBackgroundResource(R.drawable.btn_focus_mask);
                        img_collection.setVisibility(View.VISIBLE);
                        if(currenFragment!=2){
                            Fragment fragment_collection=new LocalPlaylistFragment();
                            transaction.replace(R.id.container, fragment_collection);
                            currenFragment=2;
                        }
                        break;

                    case R.id.layout_history:
                        layout_recommend.setBackgroundResource(R.drawable.btn_focus_mask);
                        layout_subscribe.setBackgroundResource(R.drawable.btn_focus_mask);
                        layout_collection.setBackgroundResource(R.drawable.btn_focus_mask);
                        layout_history.setBackgroundResource(R.drawable.btn_focus_mask);
                        img_history.setVisibility(View.VISIBLE);
                        if(currenFragment!=3){
                            Fragment fragment_history=new StatisticsPlaylistFragment();
                            transaction.replace(R.id.container, fragment_history);
                            currenFragment=3;
                        }
                        break;
            }
                if (v.getId() == R.id.layout_recommend || v.getId() == R.id.layout_subscribe){
                    transaction.commitNowAllowingStateLoss();
                }else {
                    transaction.commit();
                }

            }else{
                switch (v.getId()){
                    case R.id.layout_recommend:
                        if(currenFragment==0){
                            layout_recommend.setBackgroundResource(R.drawable.masklayer);
                        }
                        img_recommend.setVisibility(View.INVISIBLE);
                        break;
                    case R.id.layout_subscribe:
                        img_subscribe.setVisibility(View.INVISIBLE);
                        break;
                    case R.id.layout_collection:
                        img_collection.setVisibility(View.INVISIBLE);
                        break;
                    case R.id.layout_history:
                        img_history.setVisibility(View.INVISIBLE);
                        break;
                }

            }
        }
    };
    View.OnKeyListener onKeyListener=new View.OnKeyListener() {
        @Override
        public boolean onKey(View view, int keyCode, KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if(view.getId()==R.id.layout_recommend && keyCode==KeyEvent.KEYCODE_DPAD_LEFT){
                    layout_recommend.setFocusable(true);
                    layout_subscribe.setFocusable(true);
                    layout_collection.setFocusable(true);
                    layout_history.setFocusable(true);
                    btn_find.setFocusable(true);
                    btn_find.requestFocus();
                }else if(view.getId()==R.id.layout_recommend && keyCode==KeyEvent.KEYCODE_DPAD_DOWN){
                    layout_recommend.setBackgroundResource(R.drawable.masklayer);
                    btn_find.setFocusable(true);
                    layout_recommend.setFocusable(true);
                    layout_subscribe.setFocusable(true);
                    layout_collection.setFocusable(true);
                    layout_history.setFocusable(true);
                }else if(view.getId()==R.id.layout_recommend && keyCode==KeyEvent.KEYCODE_DPAD_UP){
                    btn_find.setFocusable(true);
                    layout_recommend.setFocusable(true);
                    layout_subscribe.setFocusable(true);
                    layout_collection.setFocusable(true);
                    layout_history.setFocusable(true);
                }else if(view.getId()==R.id.layout_recommend && keyCode==KeyEvent.KEYCODE_DPAD_RIGHT){
                    btn_find.setFocusable(true);
                    layout_recommend.setFocusable(true);
                    layout_subscribe.setFocusable(true);
                    layout_collection.setFocusable(true);
                    layout_history.setFocusable(true);
                }else if(view.getId()==R.id.layout_subscribe && keyCode==KeyEvent.KEYCODE_DPAD_DOWN){
                    layout_subscribe.setBackgroundResource(R.drawable.masklayer);
                    btn_find.setFocusable(true);
                    layout_recommend.setFocusable(true);
                    layout_subscribe.setFocusable(true);
                    layout_collection.setFocusable(true);
                    layout_history.setFocusable(true);
                }else if(view.getId()==R.id.layout_subscribe && keyCode==KeyEvent.KEYCODE_DPAD_UP){
                    btn_find.setFocusable(true);
                    layout_recommend.setFocusable(true);
                    layout_subscribe.setFocusable(true);
                    layout_collection.setFocusable(true);
                    layout_history.setFocusable(true);
                }else if(view.getId()==R.id.layout_subscribe && keyCode==KeyEvent.KEYCODE_DPAD_LEFT){
                    isFocus=true;
                    btn_find.setFocusable(true);
                    layout_recommend.setFocusable(true);
                    layout_subscribe.setFocusable(true);
                    layout_collection.setFocusable(true);
                    layout_history.setFocusable(true);
                }else if(view.getId()==R.id.layout_subscribe && keyCode==KeyEvent.KEYCODE_DPAD_RIGHT){
                    btn_find.setFocusable(true);
                    layout_recommend.setFocusable(true);
                    layout_subscribe.setFocusable(true);
                    layout_collection.setFocusable(true);
                    layout_history.setFocusable(true);
                }else if(view.getId()==R.id.layout_collection && keyCode==KeyEvent.KEYCODE_DPAD_DOWN){
                    layout_collection.setBackgroundResource(R.drawable.masklayer);
                    btn_find.setFocusable(true);
                    layout_recommend.setFocusable(true);
                    layout_subscribe.setFocusable(true);
                    layout_collection.setFocusable(true);
                    layout_history.setFocusable(true);
                }else if(view.getId()==R.id.layout_collection && keyCode==KeyEvent.KEYCODE_DPAD_UP){
                    btn_find.setFocusable(true);
                    layout_recommend.setFocusable(true);
                    layout_subscribe.setFocusable(true);
                    layout_collection.setFocusable(true);
                    layout_history.setFocusable(true);
                }else if(view.getId()==R.id.layout_collection && keyCode==KeyEvent.KEYCODE_DPAD_LEFT){
                    btn_find.setFocusable(true);
                    layout_recommend.setFocusable(true);
                    layout_subscribe.setFocusable(true);
                    layout_collection.setFocusable(true);
                    layout_history.setFocusable(true);
                }else if(view.getId()==R.id.layout_collection && keyCode==KeyEvent.KEYCODE_DPAD_RIGHT){
                    btn_find.setFocusable(true);
                    layout_recommend.setFocusable(true);
                    layout_subscribe.setFocusable(true);
                    layout_collection.setFocusable(true);
                    layout_history.setFocusable(true);
                }else if(view.getId()==R.id.layout_history && keyCode==KeyEvent.KEYCODE_DPAD_DOWN){
                    layout_history.setBackgroundResource(R.drawable.masklayer);
                    btn_find.setFocusable(true);
                    layout_recommend.setFocusable(true);
                    layout_subscribe.setFocusable(true);
                    layout_collection.setFocusable(true);
                    layout_history.setFocusable(true);
                }else if(view.getId()==R.id.layout_history && keyCode==KeyEvent.KEYCODE_DPAD_RIGHT){
                    btn_find.setFocusable(true);
                    layout_recommend.setFocusable(true);
                    layout_subscribe.setFocusable(true);
                    layout_collection.setFocusable(true);
                    layout_history.setFocusable(true);
                    return true;
                }else if(view.getId()==R.id.layout_history && keyCode==KeyEvent.KEYCODE_DPAD_UP){
                    btn_find.setFocusable(true);
                    layout_recommend.setFocusable(true);
                    layout_subscribe.setFocusable(true);
                    layout_collection.setFocusable(true);
                    layout_history.setFocusable(true);
                }else if(view.getId()==R.id.layout_history && keyCode==KeyEvent.KEYCODE_DPAD_LEFT){
                    btn_find.setFocusable(true);
                    layout_recommend.setFocusable(true);
                    layout_subscribe.setFocusable(true);
                    layout_collection.setFocusable(true);
                    layout_history.setFocusable(true);
                }else if(view.getId()==R.id.btn_find && keyCode==KeyEvent.KEYCODE_DPAD_DOWN){
                    if(currenFragment==0){
                        layout_recommend.setBackgroundResource(R.drawable.masklayer);
                    }else if(currenFragment==1){
                        layout_subscribe.setBackgroundResource(R.drawable.masklayer);
                    }else if(currenFragment==2){
                        layout_collection.setBackgroundResource(R.drawable.masklayer);
                    }else if(currenFragment==3){
                        layout_history.setBackgroundResource(R.drawable.masklayer);
                    }
                }else if(view.getId()==R.id.btn_find && keyCode==KeyEvent.KEYCODE_DPAD_RIGHT){
                    isFocus=true;
                }
            }
            return false;
        }
    };
}

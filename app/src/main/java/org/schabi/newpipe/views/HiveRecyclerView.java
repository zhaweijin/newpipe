package org.schabi.newpipe.views;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;


/**
 * Created by ThinkPad on 2017/9/7.
 */

public class HiveRecyclerView extends RecyclerView {
    private int mSelectedPosition = 0;
    private long time1;


    private String tag = "HiveRecyclerView";

    public HiveRecyclerView(Context context) {
        super(context);
        init();
    }

    public HiveRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HiveRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        //启用子视图排序功能
        setChildrenDrawingOrderEnabled(true);
    }

    @Override
    public void onDraw(Canvas c) {
        //mSelectedPosition = getChildAdapterPosition(getFocusedChild());
        super.onDraw(c);
    }



    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        View view = getFocusedChild();
        if (null != view) {
            int position = getChildAdapterPosition(view) - getFirstVisiblePosition();
            if (position < 0) {
                return i;
            } else {
                if (i == childCount - 1) {//这是最后一个需要刷新的item
                    if (position > i) {
                        position = i;
                    }
                    return position;
                }
                if (i == position) {//这是原本要在最后一个刷新的item
                    return childCount - 1;
                }
            }
        }
        return i;
    }

    public int getFirstVisiblePosition() {
        if (getChildCount() == 0){
            return 0;
        }else{
            return getChildAdapterPosition(getChildAt(0));
        }
    }





    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

       // Utils.print(tag,"count="+event.getRepeatCount()+",action="+event.getAction());

        //1.0+的盒子快速移动卡屏幕，以及分页加载数据的时候，焦点无规律跳动，因此做了快速响应拦截
        if (event.getAction() == KeyEvent.ACTION_DOWN ) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP || event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN) {
                if ((System.currentTimeMillis() - time1) < 400) {
                    return true;
                }
                time1 = System.currentTimeMillis();
            }
        }
        /*}else {
            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP || event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN) {
                if(event.getRepeatCount()!=0){
                    //普通移动
                    RxBus.get().post(ConStant.obString_item_load_icon_url,false);
                }else if(event.getRepeatCount()==0 && event.getAction()==KeyEvent.ACTION_UP && (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP || event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN)){
                    //快速移动
                    RxBus.get().post(ConStant.obString_item_load_icon_url,true);
                }
            }
        }*/

        return super.dispatchKeyEvent(event);
    }



    private void smoothScrollView(int keycode) {
        int scrollDistance = 0;

        //Utils.print(tag,"yyy="+getScaleY());

        if(keycode==KeyEvent.KEYCODE_DPAD_DOWN){
            scrollDistance = 100;
        }else if(keycode==KeyEvent.KEYCODE_DPAD_UP){
            scrollDistance = -100;
        }

        Log.v(tag,"scrollDistance="+scrollDistance);
        if ((keycode == KeyEvent.KEYCODE_DPAD_UP || keycode == KeyEvent.KEYCODE_DPAD_DOWN)) {
            Log.v(tag,"smooth y");
            smoothScrollBy(0, scrollDistance);
        }
    }





}

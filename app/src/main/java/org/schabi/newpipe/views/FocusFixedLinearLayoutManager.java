package org.schabi.newpipe.views;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class FocusFixedLinearLayoutManager extends LinearLayoutManager {
    public FocusFixedLinearLayoutManager(Context context) {
        super (context);
    }
    public FocusFixedLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context,orientation,reverseLayout);
    }

    public FocusFixedLinearLayoutManager(Context context, AttributeSet attrs, int defStyleAttr,
                                         int defStyleRes) {
        super(context,attrs,defStyleAttr,defStyleRes);
    }

    @Override
    public View onInterceptFocusSearch(View focused, int direction) {

        int currentPosition= getPosition(getFocusedChild());//这里要用这个方法
        int count = getItemCount();
        Log.i("===recycler===","===count=="+count);
        int lastVisiblePosition=findLastVisibleItemPosition();
        Log.i("===recycler===","===lastVisiblePosition=="+lastVisiblePosition);
        switch(direction){
            case View.FOCUS_RIGHT:
                currentPosition++;
                break;
            case View.FOCUS_LEFT:
                currentPosition--;
                break;
        }
        if(currentPosition<0||currentPosition>count){
            return focused;
        }else{
            if(currentPosition>lastVisiblePosition){
                scrollToPosition(currentPosition);
            }
        }
        return super .onInterceptFocusSearch(focused, direction);
    }
}

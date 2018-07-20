package org.schabi.newpipe.views;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.KeyEvent;

public class HiveRecyclerViewNew extends HiveRecyclerView {
    private Intent intent;
    public HiveRecyclerViewNew(Context context) {
        super(context);
        intent = new Intent("org.schabi.newpipe.slidenew");
    }

    public HiveRecyclerViewNew(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public HiveRecyclerViewNew(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
          if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT || event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
                if(event.getRepeatCount()!=0){
                    //普通移动
                    if(intent==null){
                        intent = new Intent("org.schabi.newpipe.slidenew");
                    }
                    intent.putExtra("boolean",false);
                    getContext().sendBroadcast(intent);

                }else if(event.getRepeatCount()==0 && event.getAction()==KeyEvent.ACTION_UP && (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT || event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT)){
                    //快速移动
                    if(intent==null){
                        intent = new Intent("org.schabi.newpipe.slidenew");
                    }
                     intent.putExtra("boolean",true);
                     getContext().sendBroadcast(intent);
                }
            }

        return super.dispatchKeyEvent(event);
    }
}

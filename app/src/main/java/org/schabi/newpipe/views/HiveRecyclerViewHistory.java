package org.schabi.newpipe.views;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.KeyEvent;

public class HiveRecyclerViewHistory extends HiveRecyclerView {
    private Intent intent;
    public HiveRecyclerViewHistory(Context context) {
        super(context);
       // intent = new Intent("org.schabi.newpipe.slidehistory");
    }

    public HiveRecyclerViewHistory(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public HiveRecyclerViewHistory(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
       /* if (event.getAction() == KeyEvent.ACTION_DOWN ) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP || event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN) {
                if((System.currentTimeMillis() - time1)<400){
                    return true;
                }
                time1 = System.currentTimeMillis();
            }
        }*/
        /*if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP || event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN) {
            if(event.getRepeatCount()!=0){
                //普通移动
                if(intent==null){
                    intent = new Intent("org.schabi.newpipe.slidehistory");
                }
                intent.putExtra("boolean",false);
                getContext().sendBroadcast(intent);

            }else if(event.getRepeatCount()==0 && event.getAction()==KeyEvent.ACTION_UP && (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP || event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN)){
                //快速移动
                if(intent==null){
                    intent = new Intent("org.schabi.newpipe.slidehistory");
                }
                intent.putExtra("boolean",true);
                getContext().sendBroadcast(intent);
            }
        }*/

        return super.dispatchKeyEvent(event);
    }
}

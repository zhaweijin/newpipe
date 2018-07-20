package org.schabi.newpipe.views;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import org.schabi.newpipe.R;

public class HiveRecyclerViewGrid extends HiveRecyclerView {
    private Intent intent;
    public static Toast mToast;
    public HiveRecyclerViewGrid(Context context) {
        super(context);
        intent = new Intent("org.schabi.newpipe.slidegrid");
    }

    public HiveRecyclerViewGrid(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public HiveRecyclerViewGrid(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP || event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN) {
            if(event.getRepeatCount()!=0){
                //普通移动
              if(intent==null){
                    intent = new Intent("org.schabi.newpipe.slidegrid");
                }
                intent.putExtra("boolean",false);
                getContext().sendBroadcast(intent);

            }else if(event.getRepeatCount()==0 && event.getAction()==KeyEvent.ACTION_UP && (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP || event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN)){
                //快速移动
                if(intent==null){
                    intent = new Intent("org.schabi.newpipe.slidegrid");
                }
                intent.putExtra("boolean",true);
                getContext().sendBroadcast(intent);
            }
            if(event.getRepeatCount()!=0 && event.getAction()==KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP ){
                showToast(getContext().getResources().getString(R.string.backtop));
            }
        }

        return super.dispatchKeyEvent(event);
    }

    public void showToast(String text) {
        if(mToast == null) {
            mToast = Toast.makeText(getContext(), text, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(text);
            mToast.setDuration(Toast.LENGTH_SHORT);
        }
        mToast.show();
    }
    public void cancelToast() {
        if (mToast != null) {
            mToast.cancel();
        }
    }


}

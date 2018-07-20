package org.schabi.newpipe.views;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.View;

import org.schabi.newpipe.fragments.list.channel.ChannelFragment;
import org.schabi.newpipe.util.NavigationHelper;


/**
 * Created by ThinkPad on 2017/9/7.
 */

public class ChannelHiveRecyclerView extends HiveRecyclerView {
    private int mSelectedPosition = 0;
    private long time1;
    private ChannelFragment parent;

    private String tag = "HiveRecyclerView";

    public ChannelHiveRecyclerView(Context context) {
        super(context);
        init();

    }


    public ChannelHiveRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ChannelHiveRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
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

    public void setParent(ChannelFragment parent) {
        this.parent = parent;
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
        if (getChildCount() == 0) {
            return 0;
        } else {
            return getChildAdapterPosition(getChildAt(0));
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean result = super.dispatchKeyEvent(event);
        View focusView = this.getFocusedChild();
        if (focusView == null) {
            return result;
        } else {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                return true;
            } else {
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_DPAD_RIGHT:

                        View rightView = FocusFinder.getInstance().findNextFocus(this, focusView, View.FOCUS_RIGHT);
                        if (rightView != null) {
                            rightView.requestFocus();
                            return true;
                        } else {
                            if (event.getRepeatCount() > 0)
                                return true;
                            else
                                return false;
                        }
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        View leftView = FocusFinder.getInstance().findNextFocus(this, focusView, View.FOCUS_LEFT);
                        if (leftView != null) {
                            leftView.requestFocus();
                            return true;
                        } else {
                            if (event.getRepeatCount() > 0)
                                return true;
                            else
                                return false;
                        }
                    case KeyEvent.KEYCODE_DPAD_UP:
                        View upView = FocusFinder.getInstance().findNextFocus(this, focusView, View.FOCUS_UP);
                        if (upView != null) {
                            upView.requestFocus();
                            return true;
                        } else {
                            return false;
                        }
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                        View downView = FocusFinder.getInstance().findNextFocus(this, focusView, View.FOCUS_DOWN);
                        if (downView != null) {
                            downView.requestFocus();
                            return true;
                        } else {
                            return false;
                        }
                    case KeyEvent.KEYCODE_BACK:
                        if (parent != null) {
                            parent.getFragmentManager().popBackStackImmediate(null, 0);
                            return true;
                        }
                }
                return super.dispatchKeyEvent(event);
            }
        }
    }
}

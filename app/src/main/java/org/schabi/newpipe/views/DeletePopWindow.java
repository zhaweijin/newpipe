package org.schabi.newpipe.views;


import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;

import org.schabi.newpipe.R;

public class DeletePopWindow extends PopupWindow {
	private static final String TAG = "DeletePopWindow";
	//SetAddressWindowInterface
	private Context ctx;
	/*标题信息*/
	private String titleMessage;
	/*弹框布局*/
	private View container;
	
	/*回调接口*/
	private WindowInterface windowInterface;
	private Button confirm;
	private Button cancel;
	private TextView tvTitle;
	private Resources resources;

	public DeletePopWindow(Context ctx, String titleMessage,
						   WindowInterface windowInterface) {
		super();
		resources = ctx.getResources();
		this.ctx = ctx;
		this.titleMessage = titleMessage;
		this.windowInterface = windowInterface;
		init();
	}
	

	public void init(){
		LayoutInflater mLayoutInflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		container = mLayoutInflater.inflate(R.layout.delete_pop_layout, null);

		confirm=(Button)container.findViewById(R.id.confirm);
		cancel=(Button)container.findViewById(R.id.cancel);
		
		
		tvTitle =  (TextView) container.findViewById(R.id.title);
		tvTitle.setText(titleMessage);

		confirm.setText(ctx.getResources().getString( R.string.net_set));
		cancel.setText(ctx.getResources().getString( R.string.play_continue));

		confirm.setOnClickListener(onClick);
		cancel.setOnClickListener(onClick);
		
		this.setContentView(container);
		WindowManager wm = (WindowManager)ctx
                .getSystemService(Context.WINDOW_SERVICE);

        int width = wm.getDefaultDisplay().getWidth();
        int height = wm.getDefaultDisplay().getHeight();
		this.setWidth(width);
		this.setHeight(height);
		this.setFocusable(true);
		this.setAnimationStyle(R.style.delete_popu_in_out_style);
		ColorDrawable dw = new ColorDrawable(0x00000000);
		this.setBackgroundDrawable(dw);
		this.setOutsideTouchable(false);
		this.update();
		
	}
	
	/***
	 * 
	 * @Title: setAddressWindow
	 * @author:yuanhui
	 * @Description: TODO 弹框显示在界面中间
	 */
	public void showWindow() {
		this.showAtLocation(container, Gravity.CENTER, 0, 0);
	}
	
	/***
	 * 点击事件监听
	 */
	View.OnClickListener onClick = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.confirm:
				windowInterface.onConfirm();
				break;
			case R.id.cancel:
				windowInterface.onCancel();
				break;
			}
		}
	};
	

}

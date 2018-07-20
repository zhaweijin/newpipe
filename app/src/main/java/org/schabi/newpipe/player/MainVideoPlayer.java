/*
 * Copyright 2017 Mauricio Colli <mauriciocolli@outlook.com>
 * MainVideoPlayer.java is part of NewPipe
 *
 * License: GPL-3.0+
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.schabi.newpipe.player;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.text.CaptionStyleCompat;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.SubtitleView;
import com.nostra13.universalimageloader.utils.L;

import org.schabi.newpipe.App;
import org.schabi.newpipe.R;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.VideoStream;
import org.schabi.newpipe.fragments.OnScrollBelowItemsListener;
import org.schabi.newpipe.player.helper.PlaybackParameterDialog;
import org.schabi.newpipe.player.helper.PlayerHelper;
import org.schabi.newpipe.player.mediasource.ManagedMediaSourcePlaylist;
import org.schabi.newpipe.player.playqueue.PlayQueueItem;
import org.schabi.newpipe.player.playqueue.PlayQueueItemBuilder;
import org.schabi.newpipe.player.playqueue.PlayQueueItemHolder;
import org.schabi.newpipe.player.playqueue.PlayQueueItemTouchCallback;
import org.schabi.newpipe.util.AnimationUtils;
import org.schabi.newpipe.util.Constants;
import org.schabi.newpipe.util.ExtractorHelper;
import org.schabi.newpipe.util.ListHelper;
import org.schabi.newpipe.util.NavigationHelper;
import org.schabi.newpipe.util.PermissionHelper;
import org.schabi.newpipe.util.StateSaver;
import org.schabi.newpipe.util.ThemeHelper;

import java.io.IOException;
import java.security.Key;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

import icepick.State;

import static org.schabi.newpipe.player.BasePlayer.STATE_PLAYING;
import static org.schabi.newpipe.player.VideoPlayer.DEFAULT_CONTROLS_DURATION;
import static org.schabi.newpipe.player.VideoPlayer.DEFAULT_CONTROLS_HIDE_TIME;
import static org.schabi.newpipe.player.helper.PlayerHelper.getTimeString;
import static org.schabi.newpipe.util.AnimationUtils.Type.SLIDE_AND_ALPHA;
import static org.schabi.newpipe.util.AnimationUtils.animateView;
import static org.schabi.newpipe.util.StateSaver.KEY_SAVED_STATE;

/**
 * Activity Player implementing VideoPlayer
 *
 * @author mauriciocolli
 */
public final class MainVideoPlayer extends AppCompatActivity
        implements StateSaver.WriteRead, PlaybackParameterDialog.Callback, View.OnFocusChangeListener {
    private static final String TAG = "MainVideoPlayer";
    private static final int FIRST_SEEK_NUM = 1;
    private static final int SECONDE_SEEK_NUM = 2;
    private static final boolean DEBUG = BasePlayer.DEBUG;

    private GestureDetector gestureDetector;

    private VideoPlayerImpl playerImpl;

    private SharedPreferences defaultPreferences;

    @Nullable
    private PlayerState playerState;
    private boolean isInMultiWindow;
    private LinearLayout tips;
    private LinearLayout bottomControls;
    private LinearLayout player_quality;
    private String currentquality;
    private SharedPreferences preferences;
    private boolean hideSeekBar = false;
    private boolean isShow = false;
    public static int qualityChangedIndex = -1;
    private int seekCount = 0;
    @State
    protected int serviceId = Constants.NO_SERVICE_ID;
    /*  Dialog view   */
    private List<VideoStream> qualityStreams;
    /*//////////////////////////////////////////////////////////////////////////
    // Activity LifeCycle
    //////////////////////////////////////////////////////////////////////////*/
    public static MainVideoPlayer instance;
    public static boolean hasChanged = false;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG)
            Log.d(TAG, "onCreate() called with: savedInstanceState = [" + savedInstanceState + "]");
        defaultPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        ThemeHelper.setTheme(this);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            getWindow().setStatusBarColor(Color.BLACK);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = PlayerHelper.getScreenBrightness(getApplicationContext());
        getWindow().setAttributes(lp);
        setContentView(R.layout.activity_main_player);
        playerImpl = new VideoPlayerImpl(this);
        playerImpl.setup(findViewById(android.R.id.content));
        initQualityView();
        instance = this;
        if (savedInstanceState != null && savedInstanceState.get(KEY_SAVED_STATE) != null) {
            return; // We have saved states, stop here to restore it
        }
        preferences = getSharedPreferences("org.schabi.newpipe.debug_preferences", Context.MODE_PRIVATE);
        currentquality = preferences.getString("default_resolution", "");

        final Intent intent = getIntent();
        if (intent != null) {
            playerImpl.handleIntent(intent, currentquality);
        } else {
            Toast.makeText(this, R.string.general_error, Toast.LENGTH_SHORT).show();
            finish();
        }
        initResolution();
    }

    public void hideQualityView() {
        if (player_quality != null) {
            player_quality.setVisibility(View.GONE);
        }
    }

    private void initQualityView() {
        tips = findViewById(R.id.tip);
        bottomControls = findViewById(R.id.bottomControls);
        player_quality = findViewById(R.id.player_quality);
        player_quality.setVisibility(View.GONE);
        player_quality.setFocusable(false);

    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle bundle) {
        if (DEBUG) Log.d(TAG, "onRestoreInstanceState() called");
        super.onRestoreInstanceState(bundle);
        StateSaver.tryToRestore(bundle, this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (DEBUG) Log.d(TAG, "onNewIntent() called with: intent = [" + intent + "]");
        super.onNewIntent(intent);
        playerImpl.handleIntent(intent);
    }

    @Override
    protected void onResume() {
        if (DEBUG) Log.d(TAG, "onResume() called");
        super.onResume();
        if (isInMultiWindow) return;
        isInMultiWindow = isInMultiWindow();
        if (playerState != null) {
            playerImpl.setPlaybackQuality(playerState.getPlaybackQuality());
            playerImpl.initPlayback(playerState.getPlayQueue(), playerState.getRepeatMode(),
                    playerState.getPlaybackSpeed(), playerState.getPlaybackPitch(),
                    playerState.wasPlaying());
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (playerImpl.isSomePopupMenuVisible()) {
            playerImpl.getQualityPopupMenu().dismiss();
            playerImpl.getPlaybackSpeedPopupMenu().dismiss();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (DEBUG) Log.d(TAG, "onSaveInstanceState() called");
        super.onSaveInstanceState(outState);
        if (playerImpl == null) return;

        playerImpl.setRecovery();
        playerState = new PlayerState(playerImpl.getPlayQueue(), playerImpl.getRepeatMode(),
                playerImpl.getPlaybackSpeed(), playerImpl.getPlaybackPitch(),
                playerImpl.getPlaybackQuality(), playerImpl.isPlaying());
        StateSaver.tryToSave(isChangingConfigurations(), null, outState, this);
    }

    @Override
    protected void onStop() {
        if (DEBUG) Log.d(TAG, "onStop() called");
        super.onStop();
        playerImpl.destroy();
        PlayerHelper.setScreenBrightness(getApplicationContext(),
                getWindow().getAttributes().screenBrightness);
        App.isAutoPlay = false;
    }

    public static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }
    /*//////////////////////////////////////////////////////////////////////////
    // State Saving
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public String generateSuffix() {
        return "." + UUID.randomUUID().toString() + ".player";
    }

    @Override
    public void writeTo(Queue<Object> objectsToSave) {
        if (objectsToSave == null) return;
        objectsToSave.add(playerState);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void readFrom(@NonNull Queue<Object> savedObjects) {
        playerState = (PlayerState) savedObjects.poll();
    }

    /*//////////////////////////////////////////////////////////////////////////
    // View
    //////////////////////////////////////////////////////////////////////////*/

    private void showSystemUi() {
        if (DEBUG) Log.d(TAG, "showSystemUi() called");
        if (playerImpl != null && playerImpl.queueVisible) return;

        final int visibility;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            visibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        } else {
            visibility = View.STATUS_BAR_VISIBLE;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            @ColorInt final int systenUiColor =
                    ActivityCompat.getColor(getApplicationContext(), R.color.video_overlay_color);
            getWindow().setStatusBarColor(systenUiColor);
            getWindow().setNavigationBarColor(systenUiColor);
        }

        getWindow().getDecorView().setSystemUiVisibility(visibility);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private void hideSystemUi() {
        if (DEBUG) Log.d(TAG, "hideSystemUi() called");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            int visibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                visibility |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            }
            getWindow().getDecorView().setSystemUiVisibility(visibility);
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private void toggleOrientation() {
        setLandscape(!isLandscape());
        defaultPreferences.edit()
                .putBoolean(getString(R.string.last_orientation_landscape_key), !isLandscape())
                .apply();
    }

    private boolean isLandscape() {
        return getResources().getDisplayMetrics().heightPixels < getResources().getDisplayMetrics().widthPixels;
    }

    private void setLandscape(boolean v) {
        setRequestedOrientation(v
                ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                : ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
    }

    private boolean globalScreenOrientationLocked() {
        // 1: Screen orientation changes using acelerometer
        // 0: Screen orientatino is locked
        return !(android.provider.Settings.System.getInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0) == 1);
    }

    protected void setRepeatModeButton(final ImageButton imageButton, final int repeatMode) {
        switch (repeatMode) {
            case Player.REPEAT_MODE_OFF:
                imageButton.setImageResource(R.drawable.exo_controls_repeat_off);
                break;
            case Player.REPEAT_MODE_ONE:
                imageButton.setImageResource(R.drawable.exo_controls_repeat_one);
                break;
            case Player.REPEAT_MODE_ALL:
                imageButton.setImageResource(R.drawable.exo_controls_repeat_all);
                break;
        }
    }

    private boolean isInMultiWindow() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInMultiWindowMode();
    }

    ////////////////////////////////////////////////////////////////////////////
    // Playback Parameters Listener
    ////////////////////////////////////////////////////////////////////////////

    @Override
    public void onPlaybackParameterChanged(float playbackTempo, float playbackPitch) {
        if (playerImpl != null) playerImpl.setPlaybackParameters(playbackTempo, playbackPitch);
    }

    @SuppressLint("NewApi")
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        if (event.getAction() == KeyEvent.ACTION_DOWN) {

            if (player_quality.getVisibility() == View.VISIBLE) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN || event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP || event.getKeyCode() == KeyEvent.KEYCODE_MENU)
                    return true;
            }
            Log.i(TAG, "player_quality.getVisibility():" + player_quality.getVisibility() + "___" +
                    "hasChanged:" + hasChanged);
            if (player_quality.getVisibility() != View.VISIBLE && !hasChanged) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) {
                    playerImpl.isSeeking = true;
                    playerImpl.onStartTrackingTouch(playerImpl.playbackSeekBar);
                    if (event.getRepeatCount() > 60) {
                        seekCount = FIRST_SEEK_NUM;
                    } else if (event.getRepeatCount() > 120) {
                        seekCount = SECONDE_SEEK_NUM;
                    } else {
                        seekCount = 0;
                    }
                    isShow = true;
                    playerImpl.playPauseButton.setVisibility(View.GONE);
                    playerImpl.playPreviousButton.setVisibility(View.VISIBLE);
                    playerImpl.playNextButton.setVisibility(View.GONE);
                    if (playerImpl.playPreviousButton.getAlpha() == 0f) {
                        playerImpl.playPreviousButton.setAlpha(1f);
                        playerImpl.playPreviousButton.animate().setStartDelay(0).alpha(0).setDuration(1000).start();
                    }
                    bottomControls.setAlpha(1f);
                    bottomControls.setVisibility(View.VISIBLE);
                    bottomControls.animate().setStartDelay(0).alpha(0).setDuration(10000).start();
                    playerImpl.onplayReverse();
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER) {

                    Log.i(TAG, "pause");
                    bottomControls.setAlpha(1f);
                    playerImpl.onPlayPause();
                    playerImpl.showUI(playerImpl.isPlaying());

                } else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    if (playerImpl.simpleExoPlayer.getCurrentPosition() + 10000 < playerImpl.simpleExoPlayer.getDuration()) {
                        playerImpl.isSeeking = true;
                        playerImpl.onStartTrackingTouch(playerImpl.playbackSeekBar);
                        if (event.getRepeatCount() > 60) {
                            seekCount = FIRST_SEEK_NUM;
                        } else if (event.getRepeatCount() > 120) {
                            seekCount = SECONDE_SEEK_NUM;
                        } else {
                            seekCount = 0;
                        }
                        isShow = true;
                        Log.i(TAG, "next");
                        playerImpl.playPauseButton.setVisibility(View.GONE);
                        playerImpl.playNextButton.setVisibility(View.VISIBLE);
                        playerImpl.playPreviousButton.setVisibility(View.GONE);
                        if (playerImpl.playNextButton.getAlpha() == 0f) {
                            playerImpl.playNextButton.setAlpha(1f);
                            playerImpl.playNextButton.animate().setStartDelay(0).alpha(0).setDuration(1000).start();
                        }
                        bottomControls.setAlpha(1f);
                        bottomControls.setVisibility(View.VISIBLE);
                        bottomControls.animate().setStartDelay(0).alpha(0).setDuration(10000).start();
                        playerImpl.onPlaySpeed();
                    }
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP) {
                    Log.i(TAG, "up");
                    if (hideSeekBar && bottomControls.getAlpha() != 0f && tips.getAlpha() != 0f) {
                        Log.i("qiuxiaoyuan", "up ----  hideSeekBar");
                        bottomControls.setAlpha(0f);
                        bottomControls.setVisibility(View.GONE);
                        tips.setAlpha(0f);
                        tips.setVisibility(View.GONE);
                        hideSeekBar = false;
                    } else {
                        playerImpl.playPauseButton.setVisibility(View.GONE);
                        playerImpl.playNextButton.setVisibility(View.GONE);
                        playerImpl.playPreviousButton.setVisibility(View.GONE);
                        bottomControls.setAlpha(1f);
                        bottomControls.setVisibility(View.VISIBLE);
                        bottomControls.animate().setStartDelay(0).alpha(0).setDuration(10000).start();
                        if (playerImpl.playQueue.getItem() != playerImpl.playQueue.getItem(0)) {
                            Log.i(TAG, "------------fist video");
                            playerImpl.onPlayPrevious();
                            playerImpl.onStopBufferLoop(playerImpl.playbackSeekBar);
                        }
                    }
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN) {
                    seekCount = 0;
                    Log.i(TAG, "down");
                    playerImpl.playPauseButton.setVisibility(View.GONE);
                    playerImpl.playNextButton.setVisibility(View.GONE);
                    playerImpl.playPreviousButton.setVisibility(View.GONE);
                    bottomControls.setAlpha(1f);
                    bottomControls.setVisibility(View.VISIBLE);
                    bottomControls.animate().setStartDelay(0).alpha(0).setDuration(10000).start();
                    playerImpl.onPlayNext();
                    playerImpl.onStopBufferLoop(playerImpl.playbackSeekBar);
                }
            }

            if (event.getKeyCode() == KeyEvent.KEYCODE_MENU) {
                initResolution();
                //当当前播放流不为空时，执行显示分辨率弹窗
                if (qualityStreams != null && qualityStreams.size() > 0) {
                    seekCount = 0;
                    Log.i(TAG, "menu");
                    qualityChangedIndex = 0;
                    playerImpl.playPreviousButton.setVisibility(View.GONE);
                    playerImpl.playNextButton.setVisibility(View.GONE);
                    playerImpl.playPauseButton.setVisibility(View.GONE);
                    player_quality.setVisibility(View.VISIBLE);
                    player_quality.setFocusable(true);
                    player_quality.requestFocus();
                    bottomControls.setAlpha(1f);
                    bottomControls.setVisibility(View.VISIBLE);
                    bottomControls.animate().setStartDelay(0).alpha(0).setDuration(10000).start();
                    player_quality.removeAllViews();
                    int count = 0;
                    for (int i = qualityStreams.size() - 1; i >= 0; i--) {
                        Log.i(TAG, "qualityStreams.get( " + i + ").getResolution()  is  " + qualityStreams.get(i).getResolution());
                        Log.i(TAG, currentquality);
                        String resolution = qualityStreams.get(i).getResolution();
                        Button button = new Button(MainVideoPlayer.this);
                        button.setTextSize(px2dip(this, 30));
                        button.setTextColor(getResources().getColor(R.color.white));
                        button.setText(resolution);
                        button.setBackground(getResources().getDrawable(R.drawable.img_selector_btn_one));
                        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(dip2px(this, 130), ViewGroup.LayoutParams.WRAP_CONTENT);
                        layoutParams.setMargins(0, 0, 20, 0);
                        button.setLayoutParams(layoutParams);
                        button.setNextFocusDownId(button.getId());
                        button.setNextFocusUpId(button.getId());

                        if ((currentquality.equals(resolution))) {
                            Log.i("qiuxiaoyuan123", "========true");
                            button.requestFocus();
                            button.setFocusable(true);
                            button.setTextColor(getResources().getColor(R.color.color_red));
                        } else {
                            count++;
                        }
                        button.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                            @SuppressLint("NewApi")
                            @Override
                            public void onFocusChange(View v, boolean hasFocus) {
                                if (hasFocus) {
                                    button.setNextFocusDownId(button.getId());
                                    button.setNextFocusUpId(button.getId());
                                    button.setTextColor(getResources().getColor(R.color.color_red));
                                } else {
                                    button.setTextColor(getResources().getColor(R.color.white));
                                }
                            }
                        });
                        button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                SharedPreferences sharedPreferences = getSharedPreferences("org.schabi.newpipe.debug_preferences", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                hasChanged = true;
                                playerImpl.onVideoPlaying(resolution);
                                editor.putString("default_resolution", resolution);
                                editor.commit();
                                player_quality.setVisibility(View.GONE);
                                player_quality.setFocusable(false);
                                player_quality.clearFocus();
                            }
                        });

                        if (resolution == "1080p" || resolution == "720p" || resolution == "480p" || resolution == "360p" || resolution == "240p") {
                            player_quality.addView(button);

                        }
                    }
                }
                return true;

            } else if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                seekCount = 0;
                Log.i(TAG, "back");
                playerImpl.playPreviousButton.setVisibility(View.GONE);
                playerImpl.playNextButton.setVisibility(View.GONE);
                playerImpl.playPauseButton.setVisibility(View.GONE);
                bottomControls.setVisibility(View.GONE);
                if (player_quality.getVisibility() == View.VISIBLE) {
                    player_quality.setVisibility(View.GONE);
                    qualityChangedIndex = 1;
                    return true;
                }
                if ((isShow) && (bottomControls.getAlpha() != 0f || tips.getAlpha() != 0f)) {
                    bottomControls.setAlpha(0f);
                    bottomControls.setVisibility(View.GONE);
                    tips.setAlpha(0f);
                    tips.setVisibility(View.GONE);
                    isShow = false;
                    return true;
                }
                playerImpl.changeState(BasePlayer.STATE_COMPLETED);
                onPause();
                onStop();
                finish();
            }
        } else {
            if ((event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT || event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) && playerImpl.isSeeking) {
                playerImpl.mSeekBarHandler.sendEmptyMessageAtTime(VideoPlayerImpl.MSG_SEEK, 100);
            } else {
                playerImpl.seekTo = -1;
            }
            Log.i(TAG, "isSeeking:ACTION_UP");
        }
        return super.dispatchKeyEvent(event);
    }

    protected boolean onError(Throwable exception) {
        if (DEBUG) Log.d(TAG, "onError() called with: exception = [" + exception + "]");
        if (ExtractorHelper.isInterruptedCaused(exception)) {
            if (DEBUG) Log.w(TAG, "onError() isInterruptedCaused! = [" + exception + "]");
            return true;
        }
        if (exception instanceof IOException) {
            return true;
        }

        return false;
    }

    @SuppressLint("NewApi")
    @Override
    public void onFocusChange(View v, boolean hasFocus) {

        if (hasFocus) {
            switch (v.getId()) {
            }
        }
    }

    /**
     * 初始化分辨率,getAvailableStreams为获取当前播放流，初始化播放时，可能为空，需要多次请求
     */
    private void initResolution() {
        String currentResolution = null;
        SharedPreferences.Editor editor = preferences.edit();
        if (qualityStreams == null) {
            qualityStreams = playerImpl.getAvailableStreams();
        }
        if (qualityStreams != null && qualityStreams.size() > 0) {
            Boolean hasExit = false;
            currentquality = preferences.getString("default_resolution", "");
            for (int i = qualityStreams.size() - 1; i >= 0; i--) {
                if (!currentquality.isEmpty()) {
                    if (qualityStreams.get(i).getResolution().equals(currentquality)) {
                        currentResolution = qualityStreams.get(i).getResolution();
                        hasExit = true;
                        break;
                    }
                } else {
                    if (qualityStreams.get(i).getResolution().equals("480p")) {
                        hasExit = true;
                        editor.putString("default_resolution", "480p");
                        currentResolution = "480p";
                        break;
                    }
                }
            }
            if (!hasExit) {
                if (qualityStreams.size() > 1 && qualityStreams.get(qualityStreams.size() - 1).getResolution().equals("144p"))
                    currentResolution = qualityStreams.get(qualityStreams.size() - 2).getResolution();
                else
                    currentResolution = qualityStreams.get(qualityStreams.size() - 1).getResolution();
                editor.putString("default_resolution", currentResolution);
            }
        }
        currentquality = currentResolution;
    }

    ///////////////////////////////////////////////////////////////////////////

    @SuppressWarnings({"unused", "WeakerAccess"})
    private class VideoPlayerImpl extends VideoPlayer {
        private String TAG = "MainVideoPlayer";
        private TextView titleTextView;
        private TextView channelTextView;
        private TextView volumeTextView;
        private TextView brightnessTextView;
        //        private ImageButton queueButton;
//        private ImageButton repeatButton;//02
        private ImageButton shuffleButton;//01

        private ImageButton playPauseButton;
        private ImageButton playPreviousButton;
        private ImageButton playNextButton;

        private RelativeLayout queueLayout;
        private ImageButton itemsListCloseButton;
        private RecyclerView itemsList;
        private ItemTouchHelper itemTouchHelper;
        private ObjectAnimator pauseButtonAnimator;

        private boolean queueVisible;

        //        private ImageButton moreOptionsButton;
        private ImageButton toggleOrientationButton;
        private ImageButton switchPopupButton;
        private ImageButton switchBackgroundButton;
        private SeekBar playbackSeekBar;

        private RelativeLayout windowRootLayout;
        private View secondaryControls;
        private SurfaceView surfaceView;

        protected int seekTo = -1;

        protected int mutliSeekNum = 0;

        protected static final int UPDATE_PROGRESS = 1;


        protected static final int MSG_SEEK = 2;

        protected static final int MSG_VOLUME = 3;

        protected static final int HIDDEN_PROGRESS = 4;

        protected static final int HIDDEN_LAST_TIME = 5;

        protected static final int UPDATE_PROGRESS_TIME = 1000;

        protected static final int UPDATE_SHOW_TIME = 5000;
        protected static final int SEEK_STEP_MIN = 5 * 1000;
        protected static final int SEEK_STEP_MAX = 3 * 60 * 1000;
        protected static final int SEEK_DELAY_TIME = 400;
        private boolean isInitposition = false;
        private int seekbackWidth;

        protected final SeekBarHandler mSeekBarHandler = new SeekBarHandler();
        private int durationPosition;
        private ManagedMediaSourcePlaylist playlist = new ManagedMediaSourcePlaylist();

        protected class SeekBarHandler extends Handler {

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case UPDATE_PROGRESS:
                        playbackSeekBar.setProgress(seekTo);
                        playbackCurrentTime.setText(getTimeString(playbackSeekBar.getProgress()));
                        removeMessages(UPDATE_PROGRESS);
                        break;
                    case MSG_SEEK:
                        exoseekPlayer();
                        break;

                }
            }
        }


        protected void exoseekPlayer() {
            isSeeking = false;
            Log.i(TAG, "isSeeking:" + isSeeking);
            int durationTime = (int) simpleExoPlayer.getDuration();
            if (seekTo != 0 && durationTime != 0) {
                if (seekTo < durationTime && seekTo > 0) {
                    seekTo = (seekTo >= durationTime) ? durationTime : seekTo;
                    simpleExoPlayer.seekTo(seekTo);
                    if (!isPlaying()) {
                        simpleExoPlayer.setPlayWhenReady(true);
                        onPlay();
                        animateView(currentDisplaySeek, AnimationUtils.Type.SCALE_AND_ALPHA, false, 200);
                    }
                    Log.e(TAG, "last 2 : " + seekTo);
                    seekTo = -1;
                    mSeekBarHandler.removeMessages(MSG_SEEK);
                }
            }
        }

        VideoPlayerImpl(final Context context) {
            super("VideoPlayerImpl" + MainVideoPlayer.TAG, context);
        }

        @Override
        public void initViews(View rootView) {
            super.initViews(rootView);
            this.titleTextView = rootView.findViewById(R.id.titleTextView);
            this.channelTextView = rootView.findViewById(R.id.channelTextView);
            this.volumeTextView = rootView.findViewById(R.id.volumeTextView);
            this.brightnessTextView = rootView.findViewById(R.id.brightnessTextView);
            this.playPauseButton = rootView.findViewById(R.id.playPauseButton);
            this.playPreviousButton = rootView.findViewById(R.id.playPreviousButton);
            this.playNextButton = rootView.findViewById(R.id.playNextButton);
            this.playbackSeekBar = rootView.findViewById(R.id.playbackSeekBar);
            this.playPreviousButton.setAlpha(0f);
            this.playNextButton.setAlpha(0f);
            this.playPauseButton.setVisibility(View.GONE);
            this.playPreviousButton.setVisibility(View.GONE);
            this.playNextButton.setVisibility(View.GONE);
            this.secondaryControls = rootView.findViewById(R.id.secondaryControls);
            this.toggleOrientationButton = rootView.findViewById(R.id.toggleOrientation);
            this.switchBackgroundButton = rootView.findViewById(R.id.switchBackground);
            this.switchPopupButton = rootView.findViewById(R.id.switchPopup);
            this.queueLayout = findViewById(R.id.playQueuePanel);
            this.itemsListCloseButton = findViewById(R.id.playQueueClose);
            this.itemsList = findViewById(R.id.playQueue);
            this.surfaceView = findViewById(R.id.surfaceView);

            titleTextView.setSelected(true);
            channelTextView.setSelected(true);
            getRootView().setKeepScreenOn(true);
            pauseButtonAnimator = ObjectAnimator.ofFloat(playPauseButton, "alpha", 1f, 0f);
            pauseButtonAnimator.setDuration(1000).setInterpolator(new LinearInterpolator());
            pauseButtonAnimator.setRepeatMode(ObjectAnimator.REVERSE);
            pauseButtonAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        }

        @Override
        protected void setupSubtitleView(@NonNull SubtitleView view,
                                         final float captionScale,
                                         @NonNull final CaptionStyleCompat captionStyle) {
            final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            final int minimumLength = Math.min(metrics.heightPixels, metrics.widthPixels);
            final float captionRatioInverse = 20f + 4f * (1f - captionScale);
            view.setFixedTextSize(TypedValue.COMPLEX_UNIT_PX,
                    (float) minimumLength / captionRatioInverse);
            view.setApplyEmbeddedStyles(captionStyle.equals(CaptionStyleCompat.DEFAULT));
            view.setStyle(captionStyle);
        }

        @Override
        public void initListeners() {
            super.initListeners();

            MySimpleOnGestureListener listener = new MySimpleOnGestureListener();
            gestureDetector = new GestureDetector(context, listener);
            gestureDetector.setIsLongpressEnabled(false);
            getRootView().setOnTouchListener(listener);
            playPauseButton.setOnClickListener(this);
            playPreviousButton.setOnClickListener(this);
            playNextButton.setOnClickListener(this);
            toggleOrientationButton.setOnClickListener(this);
            switchBackgroundButton.setOnClickListener(this);
            switchPopupButton.setOnClickListener(this);
        }

        /*//////////////////////////////////////////////////////////////////////////
        // ExoPlayer Video Listener
        //////////////////////////////////////////////////////////////////////////*/

        @Override
        public void onRepeatModeChanged(int i) {
            super.onRepeatModeChanged(i);
            updatePlaybackButtons();
        }

        @Override
        public void onShuffleClicked() {
            super.onShuffleClicked();
            updatePlaybackButtons();
        }

        /*//////////////////////////////////////////////////////////////////////////
        // Playback Listener
        //////////////////////////////////////////////////////////////////////////*/

        protected void onMetadataChanged(@NonNull final PlayQueueItem item,
                                         @Nullable final StreamInfo info,
                                         final int newPlayQueueIndex,
                                         final boolean hasPlayQueueItemChanged) {
            super.onMetadataChanged(item, info, newPlayQueueIndex, false);

            titleTextView.setText(getVideoTitle());
            channelTextView.setText(getUploaderName());
        }

        @Override
        public void onPlaybackShutdown() {
            super.onPlaybackShutdown();
            finish();
        }

        /*//////////////////////////////////////////////////////////////////////////
        // Player Overrides
        //////////////////////////////////////////////////////////////////////////*/

        @Override
        public void onFullScreenButtonClicked() {
            super.onFullScreenButtonClicked();

            if (DEBUG) Log.d(TAG, "onFullScreenButtonClicked() called");
            if (simpleExoPlayer == null) return;

            if (!PermissionHelper.isPopupEnabled(context)) {
                PermissionHelper.showPopupEnablementToast(context);
                return;
            }

            setRecovery();
            final Intent intent = NavigationHelper.getPlayerIntent(
                    context,
                    PopupVideoPlayer.class,
                    this.getPlayQueue(),
                    this.getRepeatMode(),
                    this.getPlaybackSpeed(),
                    this.getPlaybackPitch(),
                    this.getPlaybackQuality()
            );
            context.startService(intent);

            ((View) getControlAnimationView().getParent()).setVisibility(View.GONE);
            destroy();
            finish();
        }

        public void onPlayBackgroundButtonClicked() {
            if (DEBUG) Log.d(TAG, "onPlayBackgroundButtonClicked() called");
            if (playerImpl.getPlayer() == null) return;

            setRecovery();
            final Intent intent = NavigationHelper.getPlayerIntent(
                    context,
                    BackgroundPlayer.class,
                    this.getPlayQueue(),
                    this.getRepeatMode(),
                    this.getPlaybackSpeed(),
                    this.getPlaybackPitch(),
                    this.getPlaybackQuality()
            );
            context.startService(intent);

            ((View) getControlAnimationView().getParent()).setVisibility(View.GONE);
            destroy();
            finish();
        }


        @Override
        public void onClick(View v) {
            super.onClick(v);

            if (v.getId() == playPauseButton.getId()) {
                playPauseButton.setVisibility(View.VISIBLE);
            } else if (v.getId() == playPreviousButton.getId()) {
                playPreviousButton.setVisibility(View.VISIBLE);

            } else if (v.getId() == playNextButton.getId()) {
                playNextButton.setVisibility(View.VISIBLE);
            } else if (v.getId() == toggleOrientationButton.getId()) {
                onScreenRotationClicked();

            } else if (v.getId() == switchPopupButton.getId()) {
                onFullScreenButtonClicked();

            } else if (v.getId() == switchBackgroundButton.getId()) {
                onPlayBackgroundButtonClicked();

            }

            if (getCurrentState() != STATE_COMPLETED) {
                getControlsVisibilityHandler().removeCallbacksAndMessages(null);
                animateView(getControlsRoot(), true, DEFAULT_CONTROLS_DURATION, 0, () -> {
                    if (getCurrentState() == STATE_PLAYING && !isSomePopupMenuVisible()) {
                        hideControls(DEFAULT_CONTROLS_DURATION, DEFAULT_CONTROLS_HIDE_TIME);
                    }
                });
            }
        }

        public void onplayReverse() {
            int last = (seekTo == -1) ? (int) simpleExoPlayer.getCurrentPosition() : seekTo;
            last = last - 10000 * (seekCount + 1);
            if (last <= 0) {
                last = 1000;
            }
            delaySeekTo(last);
        }

        public void onPlaySpeed() {
            int last = (seekTo == -1) && simpleExoPlayer != null ? (int) simpleExoPlayer.getCurrentPosition() : seekTo;
            last = last + 10000 * (seekCount + 1);
            if (simpleExoPlayer != null && last > simpleExoPlayer.getDuration()) {
                return;
            }
            delaySeekTo(last);
        }

        private int mCurrentPosition;


        protected void delaySeekTo(int seekTo) {
            this.seekTo = seekTo;
            mSeekBarHandler.sendEmptyMessage(UPDATE_PROGRESS);
        }

        private void onQueueClicked() {
            queueVisible = true;
            buildQueue();
            updatePlaybackButtons();

            getControlsRoot().setVisibility(View.INVISIBLE);
            animateView(queueLayout, SLIDE_AND_ALPHA, /*visible=*/true,
                    DEFAULT_CONTROLS_DURATION);

            itemsList.scrollToPosition(playQueue.getIndex());
        }

        private void onQueueClosed() {
            animateView(queueLayout, SLIDE_AND_ALPHA, /*visible=*/false,
                    DEFAULT_CONTROLS_DURATION);
            queueVisible = false;
        }

        private void onMoreOptionsClicked() {
            if (DEBUG) Log.d(TAG, "onMoreOptionsClicked() called");

            final boolean isMoreControlsVisible = secondaryControls.getVisibility() == View.VISIBLE;
            animateView(secondaryControls, SLIDE_AND_ALPHA, !isMoreControlsVisible,
                    DEFAULT_CONTROLS_DURATION);
            showControls(DEFAULT_CONTROLS_DURATION);
        }

        private void onScreenRotationClicked() {
            if (DEBUG) Log.d(TAG, "onScreenRotationClicked() called");
            toggleOrientation();
            showControlsThenHide();
        }

        @Override
        public void onPlaybackSpeedClicked() {
            PlaybackParameterDialog.newInstance(getPlaybackSpeed(), getPlaybackPitch())
                    .show(getSupportFragmentManager(), TAG);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            super.onStopTrackingTouch(seekBar);
            if (wasPlaying()) showControlsThenHide();
        }

        @Override
        public void onDismiss(PopupMenu menu) {
            super.onDismiss(menu);
            if (isPlaying()) hideControls(DEFAULT_CONTROLS_DURATION, 0);
        }

        @Override
        protected int nextResizeMode(int currentResizeMode) {
            switch (currentResizeMode) {
                case AspectRatioFrameLayout.RESIZE_MODE_FIT:
                    return AspectRatioFrameLayout.RESIZE_MODE_FILL;
                case AspectRatioFrameLayout.RESIZE_MODE_FILL:
                    return AspectRatioFrameLayout.RESIZE_MODE_ZOOM;
                default:
                    return AspectRatioFrameLayout.RESIZE_MODE_FIT;
            }
        }

        @Override
        protected int getDefaultResolutionIndex(final List<VideoStream> sortedVideos) {
            return ListHelper.getDefaultResolutionIndex(context, sortedVideos);
        }

        @Override
        protected int getOverrideResolutionIndex(final List<VideoStream> sortedVideos,
                                                 final String playbackQuality) {
            return ListHelper.getResolutionIndex(context, sortedVideos, playbackQuality);
        }

        /*//////////////////////////////////////////////////////////////////////////
        // States
        //////////////////////////////////////////////////////////////////////////*/

        private void animatePlayButtons(final boolean show, final int duration) {
        }

        @Override
        public void onBlocked() {
            Log.i(TAG, "=======mainvideoplayer==onblocked()");
            super.onBlocked();
            playPauseButton.setImageResource(R.drawable.player_pause);
            animatePlayButtons(false, 100);
            getRootView().setKeepScreenOn(true);
        }

        @Override
        public void onBuffering() {
            super.onBuffering();
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) resetSurfaceview();
            animatePlayButtons(false, 100);
            getRootView().setKeepScreenOn(true);
        }

        private void resetSurfaceview() {
            if (getSurfaceView() != null) {
                getSurfaceView().setVisibility(View.GONE);
                getSurfaceView().setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onPlaying() {
            super.onPlaying();
            animateView(playPauseButton, AnimationUtils.Type.SCALE_AND_ALPHA, false, 80, 0, () -> {
                playPauseButton.setImageResource(R.drawable.player_pause);
                animatePlayButtons(true, 200);
            });

            getRootView().setKeepScreenOn(true);
        }

        @Override
        public void onPaused() {
            super.onPaused();
            animateView(playPauseButton, AnimationUtils.Type.SCALE_AND_ALPHA, false, 80, 0, () -> {
                playPauseButton.setImageResource(R.drawable.player_pause);
                /////////////
                playPreviousButton.setVisibility(View.GONE);
                playNextButton.setVisibility(View.GONE);
            });

            getRootView().setKeepScreenOn(false);
        }

        @Override
        public void onPausedSeek() {
            super.onPausedSeek();
            animatePlayButtons(false, 100);
            getRootView().setKeepScreenOn(true);
        }

        @Override
        public void onPlay() {
            Log.i("QXM", "============onPlay ");
            super.onPlay();
            playPauseButton.setVisibility(View.GONE);
            tips.setVisibility(View.VISIBLE);
            bottomControls.setVisibility(View.VISIBLE);
            bottomControls.setAlpha(1f);
            tips.animate().setStartDelay(0).alpha(0).setDuration(10000).start();
            bottomControls.animate().setStartDelay(0).alpha(0).setDuration(10000).start();
            hideSeekBar = true;
        }

        @Override
        public void onPrepared(boolean playWhenReady) {
            super.onPrepared(playWhenReady);
            Log.i(TAG, "onPrepared:");
            //切换视频的时候，会重新走onPrepared，这时候才应该将切换标志恢复成原本的状态
            if (hasChanged) {
                Toast.makeText(context, R.string.quality_change_success, Toast.LENGTH_SHORT).show();
                hasChanged = false;
            }
            //应当将进度条隐藏动画放置到该位置执行，当走完onPrepared时，视频即将播放
            tips.setVisibility(View.VISIBLE);
            bottomControls.setVisibility(View.VISIBLE);
            bottomControls.setAlpha(1f);
            tips.animate().setStartDelay(0).alpha(0).setDuration(10000).start();
            bottomControls.animate().setStartDelay(0).alpha(0).setDuration(10000).start();
            hideSeekBar = true;
        }

        //全部播放时，是将所有视频整合成一个流进行播放，只有在全部播放结束的时候，才会执行ENDED状态
        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            super.onPlayerStateChanged(playWhenReady, playbackState);
            Log.i(TAG, "onPlayerStateChanged:" + playbackState + "___playWhenReady:" + playWhenReady);
            switch (playbackState) {
                case Player.STATE_IDLE: // 1
                    break;
                case Player.STATE_BUFFERING: // 2
                    break;
                case Player.STATE_READY: //3
                    break;
                case Player.STATE_ENDED: // 4
                    hasChanged = false;
                    if (playWhenReady) {
                        Toast.makeText(context, "播放结束，即将返回上一页", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    break;
            }
        }

        @Override
        public void onCompleted() {
            super.onCompleted();
            Log.i(TAG, "onCompleted");
            if (playQueue.getStreams().size() > 1) {
//                Toast.makeText(context, getResources().getString(R.string.main_player_next), Toast.LENGTH_SHORT).show();
                playerImpl.playNextButton.setVisibility(View.GONE);
                playerImpl.playPreviousButton.setVisibility(View.GONE);
            } else {
                animateView(playPauseButton, AnimationUtils.Type.SCALE_AND_ALPHA, false, 0, 0, () -> {
                    playPauseButton.setImageResource(R.drawable.ic_replay_white);
                    playPauseButton.setAlpha(1f);
                    playPauseButton.setVisibility(View.VISIBLE);
                    animatePlayButtons(true, DEFAULT_CONTROLS_DURATION);
                });
            }

            getRootView().setKeepScreenOn(false);
        }

        /*//////////////////////////////////////////////////////////////////////////
        // Utils
        //////////////////////////////////////////////////////////////////////////*/

        @Override
        public void showControlsThenHide() {
            if (queueVisible) return;

            super.showControlsThenHide();
        }

        @Override
        public void showControls(long duration) {
            if (queueVisible) return;

            super.showControls(duration);
        }

        @Override
        public void hideControls(final long duration, long delay) {
            if (DEBUG) Log.d(TAG, "hideControls() called with: delay = [" + delay + "]");
            getControlsVisibilityHandler().removeCallbacksAndMessages(null);
            getControlsVisibilityHandler().postDelayed(() ->
                    animateView(getControlsRoot(), false, duration, 0),/*delayMillis=*/delay);
        }

        private void updatePlaybackButtons() {
            if (simpleExoPlayer == null || playQueue == null) return;
        }

        private void buildQueue() {
            itemsList.setAdapter(playQueueAdapter);
            itemsList.setClickable(true);
            itemsList.setLongClickable(true);

            itemsList.clearOnScrollListeners();
            itemsList.addOnScrollListener(getQueueScrollListener());

            itemTouchHelper = new ItemTouchHelper(getItemTouchCallback());
            itemTouchHelper.attachToRecyclerView(itemsList);

            playQueueAdapter.setSelectedListener(getOnSelectedListener());

            itemsListCloseButton.setOnClickListener(view -> onQueueClosed());
        }

        private OnScrollBelowItemsListener getQueueScrollListener() {
            return new OnScrollBelowItemsListener() {
                @Override
                public void onScrolledDown(RecyclerView recyclerView) {
                    if (playQueue != null && !playQueue.isComplete()) {
                        playQueue.fetch();
                    } else if (itemsList != null) {
                        itemsList.clearOnScrollListeners();
                    }
                }
            };
        }

        private ItemTouchHelper.SimpleCallback getItemTouchCallback() {
            return new PlayQueueItemTouchCallback() {
                @Override
                public void onMove(int sourceIndex, int targetIndex) {
                    if (playQueue != null) playQueue.move(sourceIndex, targetIndex);
                }
            };
        }

        private PlayQueueItemBuilder.OnSelectedListener getOnSelectedListener() {
            return new PlayQueueItemBuilder.OnSelectedListener() {
                @Override
                public void selected(PlayQueueItem item, View view) {
                    onSelected(item);
                }

                @Override
                public void held(PlayQueueItem item, View view) {
                    final int index = playQueue.indexOf(item);
                    if (index != -1) playQueue.remove(index);
                }

                @Override
                public void onStartDrag(PlayQueueItemHolder viewHolder) {
                    if (itemTouchHelper != null) itemTouchHelper.startDrag(viewHolder);
                }
            };
        }

        ///////////////////////////////////////////////////////////////////////////
        // Getters
        ///////////////////////////////////////////////////////////////////////////

        public TextView getTitleTextView() {
            return titleTextView;
        }

        public TextView getChannelTextView() {
            return channelTextView;
        }

        public TextView getVolumeTextView() {
            return volumeTextView;
        }

        public TextView getBrightnessTextView() {
            return brightnessTextView;
        }

        public ImageButton getPlayPauseButton() {
            return playPauseButton;
        }

        public void showUI(boolean isShow) {

            playNextButton.setVisibility(View.GONE);
            playPreviousButton.setVisibility(View.GONE);
            bottomControls.setVisibility(View.VISIBLE);
            bottomControls.setAlpha(1f);
            tips.setVisibility(View.VISIBLE);
            tips.setAlpha(1f);
            if (isShow) {
                playPauseButton.setVisibility(View.INVISIBLE);
                tips.animate().setStartDelay(0).alpha(0).setDuration(17000).start();
                bottomControls.animate().setStartDelay(0).alpha(0).setDuration(17000).start();
                pauseButtonAnimator.end();
            } else {
                playPauseButton.setVisibility(View.VISIBLE);
                tips.animate().setStartDelay(0).alpha(1).setDuration(1000).start();
                bottomControls.setAlpha(1f);
                bottomControls.setVisibility(View.VISIBLE);
                pauseButtonAnimator.start();
            }

        }
    }

    private class MySimpleOnGestureListener extends GestureDetector.SimpleOnGestureListener implements View.OnTouchListener {
        private boolean isMoving;

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (DEBUG)
                Log.d(TAG, "onDoubleTap() called with: e = [" + e + "]" + "rawXy = " + e.getRawX() + ", " + e.getRawY() + ", xy = " + e.getX() + ", " + e.getY());
            if (!playerImpl.isPlaying()) return false;

//            if (e.getX() > playerImpl.getRootView().getWidth() * 2 / 3) {
//                playerImpl.onFastForward();
//            } else if (e.getX() < playerImpl.getRootView().getWidth() / 3) {
//                playerImpl.onFastRewind();
//            } else {
//                playerImpl.getPlayPauseButton().performClick();
//            }

            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (DEBUG) Log.d(TAG, "onSingleTapConfirmed() called with: e = [" + e + "]");
            if (playerImpl.getCurrentState() == BasePlayer.STATE_BLOCKED) return true;

            if (playerImpl.isControlsVisible()) {
                playerImpl.hideControls(150, 0);
            } else {
                playerImpl.showControlsThenHide();
                showSystemUi();
            }
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            if (DEBUG) Log.d(TAG, "onDown() called with: e = [" + e + "]");

            return super.onDown(e);
        }

        private final boolean isPlayerGestureEnabled = PlayerHelper.isPlayerGestureEnabled(getApplicationContext());

        private final float stepsBrightness = 15, stepBrightness = (1f / stepsBrightness), minBrightness = .01f;
        private float currentBrightness = getWindow().getAttributes().screenBrightness > 0
                ? getWindow().getAttributes().screenBrightness
                : 0.5f;

        private int currentVolume, maxVolume = playerImpl.getAudioReactor().getMaxVolume();
        private final float stepsVolume = 15, stepVolume = (float) Math.ceil(maxVolume / stepsVolume), minVolume = 0;

        private final String brightnessUnicode = new String(Character.toChars(0x2600));
        private final String volumeUnicode = new String(Character.toChars(0x1F508));

        private final int MOVEMENT_THRESHOLD = 40;
        private final int eventsThreshold = 8;
        private boolean triggered = false;
        private int eventsNum;

        // TODO: Improve video gesture controls
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (!isPlayerGestureEnabled) return false;

            //noinspection PointlessBooleanExpression
            if (DEBUG && false) Log.d(TAG, "MainVideoPlayer.onScroll = " +
                    ", e1.getRaw = [" + e1.getRawX() + ", " + e1.getRawY() + "]" +
                    ", e2.getRaw = [" + e2.getRawX() + ", " + e2.getRawY() + "]" +
                    ", distanceXy = [" + distanceX + ", " + distanceY + "]");
            float abs = Math.abs(e2.getY() - e1.getY());
            if (!triggered) {
                triggered = abs > MOVEMENT_THRESHOLD;
                return false;
            }

            if (eventsNum++ % eventsThreshold != 0 || playerImpl.getCurrentState() == BasePlayer.STATE_COMPLETED)
                return false;
            isMoving = true;
            boolean up = distanceY > 0;


            if (e1.getX() > playerImpl.getRootView().getWidth() / 2) {
                double floor = Math.floor(up ? stepVolume : -stepVolume);
                currentVolume = (int) (playerImpl.getAudioReactor().getVolume() + floor);
                if (currentVolume >= maxVolume) currentVolume = maxVolume;
                if (currentVolume <= minVolume) currentVolume = (int) minVolume;
                playerImpl.getAudioReactor().setVolume(currentVolume);

                currentVolume = playerImpl.getAudioReactor().getVolume();
                if (DEBUG) Log.d(TAG, "onScroll().volumeControl, currentVolume = " + currentVolume);
                final String volumeText = volumeUnicode + " " + Math.round((((float) currentVolume) / maxVolume) * 100) + "%";
                playerImpl.getVolumeTextView().setText(volumeText);

                if (playerImpl.getVolumeTextView().getVisibility() != View.VISIBLE)
                    animateView(playerImpl.getVolumeTextView(), true, 200);
                if (playerImpl.getBrightnessTextView().getVisibility() == View.VISIBLE)
                    playerImpl.getBrightnessTextView().setVisibility(View.GONE);
            } else {
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                currentBrightness += up ? stepBrightness : -stepBrightness;
                if (currentBrightness >= 1f) currentBrightness = 1f;
                if (currentBrightness <= minBrightness) currentBrightness = minBrightness;

                lp.screenBrightness = currentBrightness;
                getWindow().setAttributes(lp);
                if (DEBUG)
                    Log.d(TAG, "onScroll().brightnessControl, currentBrightness = " + currentBrightness);
                int brightnessNormalized = Math.round(currentBrightness * 100);

                final String brightnessText = brightnessUnicode + " " + (brightnessNormalized == 1 ? 0 : brightnessNormalized) + "%";
                playerImpl.getBrightnessTextView().setText(brightnessText);

                if (playerImpl.getBrightnessTextView().getVisibility() != View.VISIBLE)
                    animateView(playerImpl.getBrightnessTextView(), true, 200);
                if (playerImpl.getVolumeTextView().getVisibility() == View.VISIBLE)
                    playerImpl.getVolumeTextView().setVisibility(View.GONE);
            }
            return true;
        }

        private void onScrollEnd() {
            if (DEBUG) Log.d(TAG, "onScrollEnd() called");
            triggered = false;
            eventsNum = 0;
            if (playerImpl.getVolumeTextView().getVisibility() == View.VISIBLE) {
                animateView(playerImpl.getVolumeTextView(), false, 200, 200);
            }
            if (playerImpl.getBrightnessTextView().getVisibility() == View.VISIBLE) {
                animateView(playerImpl.getBrightnessTextView(), false, 200, 200);
            }

            if (playerImpl.isControlsVisible() && playerImpl.getCurrentState() == STATE_PLAYING) {
                playerImpl.hideControls(DEFAULT_CONTROLS_DURATION, DEFAULT_CONTROLS_HIDE_TIME);
            }
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (DEBUG && false)
                Log.d(TAG, "onTouch() called with: v = [" + v + "], event = [" + event + "]");
            gestureDetector.onTouchEvent(event);
            if (event.getAction() == MotionEvent.ACTION_UP && isMoving) {
                isMoving = false;
                onScrollEnd();
            }
            return true;
        }
    }
}

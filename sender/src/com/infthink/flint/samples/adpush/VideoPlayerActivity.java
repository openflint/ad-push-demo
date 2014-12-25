package com.infthink.flint.samples.adpush;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import com.infthink.flint.samples.adpush.AdpushChannel.AdData;
import com.infthink.flint.samples.adpush.R;

import tv.matchstick.flint.Flint;
import tv.matchstick.flint.MediaStatus;
import tv.matchstick.flint.RemoteMediaPlayer;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.SeekBar;
import android.widget.TextView;

public class VideoPlayerActivity extends ActionBarActivity implements
        FlintStatusChangeListener, AdpushChannel.AdChangeListener {

    private static final int PLAYER_STATE_NONE = 0;
    private static final int PLAYER_STATE_PLAYING = 1;
    private static final int PLAYER_STATE_PAUSED = 2;
    private static final int PLAYER_STATE_BUFFERING = 3;

    private static final int REFRESH_INTERVAL_MS = (int) TimeUnit.SECONDS
            .toMillis(1);

    private ImageView mAdImageView;

    private TextView mCurrentDeviceTextView;
    private TextView mStreamPositionTextView;
    private TextView mStreamDurationTextView;

    private Button mPlayPauseButton;
    private Button mStopMediaButton;

    private SeekBar mSeekBar;

    private GifView mGifView;

    private boolean mSeeking;
    private boolean mIsUserSeeking;

    private int mPlayerState;

    protected Handler mHandler;
    private Runnable mRefreshRunnable;

    private FlintVideoManager mFlintVideoManager;

    private AdData mCurrentAdData;
    
    private Vibrator mVibrator;
    private long[] mVibratorPattern = {100,400,100,400};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fling_player_activity);

        mAdImageView = (ImageView) findViewById(R.id.media_img);
        mAdImageView.setScaleType(ScaleType.FIT_CENTER);
        mAdImageView.setVisibility(View.INVISIBLE);

        mCurrentDeviceTextView = (TextView) findViewById(R.id.connected_device);
        mStreamPositionTextView = (TextView) findViewById(R.id.stream_position);
        mStreamDurationTextView = (TextView) findViewById(R.id.stream_duration);

        mPlayPauseButton = (Button) findViewById(R.id.pause_play);
        mStopMediaButton = (Button) findViewById(R.id.stop);

        mGifView = (GifView) findViewById(R.id.media_gif);
        mGifView.setVisibility(View.INVISIBLE);

        mSeekBar = (SeekBar) findViewById(R.id.seek_bar);

        mHandler = new Handler();

        String applicationId = "~adpush";
        Flint.FlintApi.setApplicationId(applicationId);
        mFlintVideoManager = new FlintVideoManager(this, applicationId, this,
                this);

        setUpControls();

        mRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (!mSeeking) {
                    refreshPlaybackPosition(
                            mFlintVideoManager.getMediaCurrentTime(),
                            mFlintVideoManager.getMediaDuration());
                }
                updateButtonStates();
                startRefreshTimer();
            }
        };
        
        mVibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE); 
    }

    private void setUpControls() {
        mAdImageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (mCurrentAdData != null) {
                    Intent intent = new Intent();
                    intent.setAction("android.intent.action.VIEW");
                    Uri content_url = Uri.parse(mCurrentAdData.click_link);
                    intent.setData(content_url);
                    startActivity(intent);
                }
            }
        });

        mPlayPauseButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayerState == PLAYER_STATE_PAUSED) {
                    mFlintVideoManager.playMedia();
                } else {
                    mFlintVideoManager.pauseMedia();
                }
            }
        });

        mStopMediaButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mFlintVideoManager.stopApplication();
            }
        });

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mIsUserSeeking = false;
                mSeekBar.setSecondaryProgress(0);
                onSeekBarMoved(TimeUnit.SECONDS.toMillis(seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mIsUserSeeking = true;
                mSeekBar.setSecondaryProgress(seekBar.getProgress());
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                    boolean fromUser) {
            }
        });

        mIsUserSeeking = false;
    }

    private void onSeekBarMoved(long position) {
        if (!mFlintVideoManager.isMediaConnectioned())
            return;

        refreshPlaybackPosition(position, -1);

        int resumeState = RemoteMediaPlayer.RESUME_STATE_PLAY;
        mSeeking = true;
        mFlintVideoManager.seekMedia(position, resumeState);
    }

    private void setCurrentDeviceName(String name) {
        mCurrentDeviceTextView.setText(name);
    }

    private void setApplicationStatus(String statusText) {
    }

    private void setCurrentMediaMetadata(String title, String subtitle,
            Uri imageUrl) {
    }

    private void refreshPlaybackPosition(long position, long duration) {
        if (!mIsUserSeeking) {
            if (position == 0) {
                mStreamPositionTextView.setText(R.string.no_time);
                mSeekBar.setProgress(0);
            } else if (position > 0) {
                mSeekBar.setProgress((int) TimeUnit.MILLISECONDS
                        .toSeconds(position));
            }
            mStreamPositionTextView.setText(formatTime(position));
        }

        if (duration == 0) {
            mStreamDurationTextView.setText(R.string.no_time);
            mSeekBar.setMax(0);
        } else if (duration > 0) {
            mStreamDurationTextView.setText(formatTime(duration));
            if (!mIsUserSeeking) {
                mSeekBar.setMax((int) TimeUnit.MILLISECONDS.toSeconds(duration));
            }
        }
    }

    private String formatTime(long millisec) {
        int seconds = (int) (millisec / 1000);
        int hours = seconds / (60 * 60);
        seconds %= (60 * 60);
        int minutes = seconds / 60;
        seconds %= 60;

        String time;
        if (hours > 0) {
            time = String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            time = String.format("%d:%02d", minutes, seconds);
        }
        return time;
    }

    private void updateButtonStates() {
        boolean hasMediaConnection = mFlintVideoManager.isMediaConnectioned();
        boolean hasMedia = false;

        if (hasMediaConnection) {
            MediaStatus mediaStatus = mFlintVideoManager.getMediaStatus();
            if (mediaStatus != null) {
                int mediaPlayerState = mediaStatus.getPlayerState();
                int playerState = PLAYER_STATE_NONE;
                if (mediaPlayerState == MediaStatus.PLAYER_STATE_PAUSED) {
                    playerState = PLAYER_STATE_PAUSED;
                } else if (mediaPlayerState == MediaStatus.PLAYER_STATE_PLAYING) {
                    playerState = PLAYER_STATE_PLAYING;
                } else if (mediaPlayerState == MediaStatus.PLAYER_STATE_BUFFERING) {
                    playerState = PLAYER_STATE_BUFFERING;
                }
                setPlayerState(playerState);

                hasMedia = mediaStatus.getPlayerState() != MediaStatus.PLAYER_STATE_IDLE;
            }
        } else {
            setPlayerState(PLAYER_STATE_NONE);
        }
        mStopMediaButton.setEnabled(hasMediaConnection && hasMedia);
        setSeekBarEnabled(hasMediaConnection && hasMedia);
    }

    private void setSeekBarEnabled(boolean enabled) {
        mSeekBar.setEnabled(enabled);
    }

    private void setPlayerState(int playerState) {
        mPlayerState = playerState;
        if (mPlayerState == PLAYER_STATE_PAUSED) {
            mPlayPauseButton.setText(R.string.play);
        } else if (mPlayerState == PLAYER_STATE_PLAYING) {
            mPlayPauseButton.setText(R.string.pause);
        }

        mPlayPauseButton.setEnabled((mPlayerState == PLAYER_STATE_PAUSED)
                || (mPlayerState == PLAYER_STATE_PLAYING));
    }

    private void clearMediaState() {
        setCurrentMediaMetadata(null, null, null);
        refreshPlaybackPosition(0, 0);
    }

    protected final void startRefreshTimer() {
        mHandler.postDelayed(mRefreshRunnable, REFRESH_INTERVAL_MS);
    }

    protected final void cancelRefreshTimer() {
        mHandler.removeCallbacks(mRefreshRunnable);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mFlintVideoManager.destroy();
        mVibrator.cancel();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        mFlintVideoManager.addMediaRouterButton(menu,
                R.id.media_route_menu_item);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onDeviceSelected(String name) {
        setCurrentDeviceName(name);
    }

    @Override
    public void onDeviceUnselected() {
        setCurrentDeviceName(getString(R.string.no_device));
    }

    @Override
    public void onVolumeChanged(double percent, boolean muted) {
    }

    @Override
    public void onApplicationStatusChanged(String status) {
        setApplicationStatus(status);
    }

    @Override
    public void onApplicationDisconnected() {
        clearMediaState();
        updateButtonStates();
    }

    @Override
    public void onConnectionFailed() {
        updateButtonStates();
        clearMediaState();
        cancelRefreshTimer();
    }

    @Override
    public void onConnected() {
    }

    @Override
    public void onNoLongerRunning(boolean isRunning) {
        if (isRunning) {
            startRefreshTimer();
        } else {
            clearMediaState();
            updateButtonStates();
        }
    }

    @Override
    public void onConnectionSuspended() {
        cancelRefreshTimer();
        updateButtonStates();
    }

    @Override
    public void onMediaStatusUpdated() {
        MediaStatus mediaStatus = this.mFlintVideoManager.getMediaStatus();
        if ((mediaStatus != null)
                && (mediaStatus.getPlayerState() == MediaStatus.PLAYER_STATE_IDLE)) {
            clearMediaState();
        }

        refreshPlaybackPosition(mFlintVideoManager.getMediaCurrentTime(),
                mFlintVideoManager.getMediaDuration());
        updateButtonStates();
    }

    @Override
    public void onMediaMetadataUpdated(String title, String artist, Uri imageUrl) {
        setCurrentMediaMetadata(title, artist, imageUrl);
    }

    @Override
    public void onApplicationConnectionResult(String applicationStatus) {
        setApplicationStatus(applicationStatus);
        startRefreshTimer();
        updateButtonStates();
    }

    @Override
    public void onLeaveApplication() {
        updateButtonStates();
    }

    @Override
    public void onStopApplication() {
        updateButtonStates();
    }

    @Override
    public void onMediaSeekEnd() {
        mSeeking = false;
    }

    @Override
    public void onMediaVolumeEnd() {
    }

    @Override
    public void onAdChange(AdData data) {
        mVibrator.vibrate(mVibratorPattern, -1);
        if ("ad_image".equals(data.type)) {
            mAdImageView.setVisibility(View.VISIBLE);
            mGifView.setVisibility(View.INVISIBLE);
            mCurrentAdData = data;
            new DownloadImageTask(mAdImageView).execute(data.image_url);
        } else if ("ad_gif".equals(data.type)) {
            mAdImageView.setVisibility(View.INVISIBLE);
            mGifView.setVisibility(View.VISIBLE);
            mGifView.setResource(data.image_url);
        }
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }
}

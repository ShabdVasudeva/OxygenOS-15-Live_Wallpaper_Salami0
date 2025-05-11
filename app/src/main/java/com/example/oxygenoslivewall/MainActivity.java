package com.example.oxygenoslivewall;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.PowerManager;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import com.example.oxygenoslivewall.R;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;

public class MainActivity extends WallpaperService {
    @Override
    public Engine onCreateEngine() {
        return new CustomWallpaperEngine();
    }

    private class CustomWallpaperEngine extends Engine {
        private ExoPlayer exoPlayer;
        private boolean isLocked = false;
        private final Handler handler = new Handler();
        private LockStatusTask lockStatusTask;
        private PowerManager.WakeLock wakeLock;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            initializeExoPlayer(holder);
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LiveApp::WallpaperWakeLock");
                wakeLock.acquire();
            }
            startLockStatusTask();
        }

        private void initializeExoPlayer(SurfaceHolder holder) {
            // Use a DefaultTrackSelector with an increased maximum video size to handle higher resolutions.
            DefaultTrackSelector trackSelector = new DefaultTrackSelector(getApplicationContext());
            trackSelector.setParameters(trackSelector.buildUponParameters().setMaxVideoSizeSd());
            exoPlayer = new ExoPlayer.Builder(getApplicationContext()).setTrackSelector(trackSelector).build();
            exoPlayer.setVideoSurface(holder.getSurface());
            MediaItem mediaItem =
                    MediaItem.fromUri("android.resource://" + getPackageName() + "/" + R.raw.wall);
            exoPlayer.setMediaItem(mediaItem);
            exoPlayer.setRepeatMode(Player.REPEAT_MODE_OFF);
            exoPlayer.prepare();
            exoPlayer.setPlayWhenReady(true);
            exoPlayer.addListener(
                    new Player.Listener() {
                        @Override
                        public void onPlaybackStateChanged(int playbackState) {
                            if (playbackState == Player.STATE_ENDED) {
                                exoPlayer.pause();
                            }
                        }
                    });
        }

        private void startLockStatusTask() {
            lockStatusTask = new LockStatusTask(getApplicationContext());
            lockStatusTask.execute();
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            if (exoPlayer != null) {
                exoPlayer.release();
                exoPlayer = null;
            }
            if (lockStatusTask != null) {
                lockStatusTask.cancel(true);
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            if (exoPlayer != null) {
                if (visible && !isLocked) {
                    exoPlayer.play();
                } else {
                    exoPlayer.pause();
                }
            }
        }

        @Override
        public void onDestroy() {
            if (exoPlayer != null) {
                exoPlayer.release();
            }
            if (lockStatusTask != null) {
                lockStatusTask.cancel(true);
            }
            super.onDestroy();
        }

        private class LockStatusTask extends AsyncTask<Void, Void, Void> {
            private final Context context;
            private final BroadcastReceiver screenReceiver =
                    new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                                isLocked = true;
                                if (exoPlayer != null) {
                                    exoPlayer.pause();
                                    exoPlayer.seekTo(0);
                                }
                            } else if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {
                                isLocked = false;
                                if (exoPlayer != null) {
                                    exoPlayer.seekTo(0);
                                    exoPlayer.play();
                                }
                            }
                        }
                    };

            LockStatusTask(Context context) {
                this.context = context;
            }

            @Override
            protected Void doInBackground(Void... voids) {
                IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
                filter.addAction(Intent.ACTION_USER_PRESENT);
                context.registerReceiver(screenReceiver, filter);
                return null;
            }

            @Override
            protected void onCancelled() {
                context.unregisterReceiver(screenReceiver);
            }
        }
    }
}

package com.mt.record;

import android.app.Service;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.text.TextUtils;

import java.io.IOException;

public class MediaManager {

    //声音管理器
    static AudioManager audioManager;
    private static MediaPlayer player;
    private static boolean isPause;

    public static void playSound(String filePathString, MediaPlayer.OnPreparedListener onPreparedListener,
                                 MediaPlayer.OnCompletionListener onCompletionListener, Context context) {
        audioManager = (AudioManager) context.getSystemService(Service.AUDIO_SERVICE);

        if (TextUtils.isEmpty(filePathString)) {
            return;
        }

        if (player == null) {
            player = new MediaPlayer();
            player.setOnErrorListener(new MediaPlayer.OnErrorListener() {

                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    player.reset();
                    return false;
                }
            });
        } else {
            player.reset();
        }

        try {
            setAudioStreamType(player);
            player.setOnPreparedListener(onPreparedListener);
            player.setOnCompletionListener(onCompletionListener);
            player.setDataSource(filePathString);
            player.prepareAsync();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void setAudioStreamType(MediaPlayer player) {
        if (Build.VERSION.SDK_INT >= 21) {
            AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
            //设置音频流的合适属性
            attrBuilder.setLegacyStreamType(AudioManager.STREAM_MUSIC);
            player.setAudioAttributes(attrBuilder.build());
        } else {
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        }
    }

    public static boolean isPlaying() {
        if (player != null) {
            return player.isPlaying();
        }
        return false;
    }

    //停止函数
    public static void pause() {
        if (player != null && player.isPlaying()) {
            player.pause();
            isPause = true;
        }
    }

    //继续
    public static void resume() {
        if (player != null && isPause) {
            player.start();
            isPause = false;
        }
    }

    public static void release() {
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
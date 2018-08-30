package com.mt.record;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.SeekBar;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int TOTAL_TIME = 2 * 60; // 单位：秒
    SeekBar seekBar;
    Button playRecord;
    Button record;
    Button repeatRecord;
    Button saveRecord;
    Chronometer recordTimer;
    private RecorderManager recordManager; // 录制音频
    private PermissionHelper permissionHelper; // 申请权限
    private boolean isRecording = false; // 录音中的标志位
    private long recordTime; // 用于记录计时暂停时的 SystemClock.elapsedRealTime();
    private int audioDuration;   // 录音时长
    private String audioDir;
    private String audioPath;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.record_play:
                playRecord();
                break;

            case R.id.record_goon:
                record();
                break;

            case R.id.record_reset:
                resetRecord();
                break;

            case R.id.record_save:
                saveRecord();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionHelper.handleRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * 保存并上传录音文件到七牛
     * 上传之后返回前页面
     */
    private void saveRecord() {

        if (MediaManager.isPlaying()) {
            MediaManager.release();
        }

        if (isRecording) {
            pauseRecord();
        }

        recordManager.release();
        record.setText(getString(R.string.start_record));
    }

    /**
     * 重新录制
     */
    private void resetRecord() {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.alert_reset_record))
                .setPositiveButton(getString(R.string.alert_positive_reset), (dialog, which) -> {
                    recordManager.cancel();
                    record.setText(getString(R.string.start_record));
                    resetTimer();
                    enableButton(false, playRecord, saveRecord);
                    enableButton(true, record);
                    seekBar.setProgress(0);
                })
                .setNegativeButton(getString(R.string.cancel_text), null)
                .create()
                .show();
    }

    /**
     * 录制
     * 开始、暂停、继续
     */
    private void record() {
        if (recordManager.isStarted()) {
            if (isRecording) {
                // 录音暂停
                pauseRecord();
            } else {
                // 录音继续
                recordManager.resume();
                isRecording = true;
                setRecordBtn(true);
                goonTimer();
                enableButton(false, repeatRecord, saveRecord, playRecord);
            }
        } else {
            // 录音开始
            recordManager.startRecorder(true);
            startTimer();
            setRecordBtn(true);
            isRecording = true;
            enableButton(false, repeatRecord, saveRecord, saveRecord);
        }
    }

    /**
     * 录音暂停
     */
    private void pauseRecord() {
        pauseTimer();
        recordManager.pause();
        audioDuration = timerDuration();
        setRecordBtn(false);
        isRecording = false;
        enableButton(true, repeatRecord, saveRecord, playRecord);
    }

    /**
     * 播放录音文件
     */
    private void playRecord() {
        resetAudio();

        MediaManager.playSound(recordManager.getAudioPath(), mp -> {
            mp.start();

            startTimer();
            enableButton(false, record, repeatRecord, playRecord);
            enableButton(true, saveRecord);
        }, mp -> {
            MediaManager.release();

            seekBar.setProgress(100);
            pauseTimer();
            enableButton(true, record, repeatRecord, saveRecord, playRecord);

            if (timerDuration() >= TOTAL_TIME) {
                enableButton(false, record);
            }
        }, this);

    }

    private void startTimer() {
        recordTimer.setBase(SystemClock.elapsedRealtime());
        recordTimer.start();
    }

    private void pauseTimer() {
        recordTime = SystemClock.elapsedRealtime();
        recordTimer.stop();
    }

    private void resetTimer() {
        recordTimer.setBase(SystemClock.elapsedRealtime());
        recordTimer.stop();
    }

    private void goonTimer() {
        if (recordTime != 0) {
            recordTimer.setBase(recordTimer.getBase() + (SystemClock.elapsedRealtime() - recordTime));
        } else {
            recordTimer.setBase(SystemClock.elapsedRealtime());
        }
        recordTimer.start();
    }

    private void resetAudio() {
        if (MediaManager.isPlaying()) {
            MediaManager.release();
        }
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermission();
        } else {
            enableButton(true, record);
        }
    }

    private void requestPermission() {
        permissionHelper.requestPermissions(getString(R.string.request_audio_permission),
                new PermissionHelper.PermissionListener() {
                    @Override
                    public void doAfterGrand(String... permission) {
                        enableButton(true, record);
                    }

                    @Override
                    public void doAfterDenied(String... permission) {
                        userRefusePermissionsDialog();
                    }
                }, Manifest.permission.RECORD_AUDIO);
    }

    private void userRefusePermissionsDialog() {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.request_audio_permission))
                .setPositiveButton(getString(R.string.setting), (dialog, which) -> {
                    //引导用户到设置中去进行设置
                    Intent intent = new Intent();
                    intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
                    intent.setData(Uri.fromParts("package", getPackageName(), null));
                    startActivity(intent);
                })
                .setNegativeButton(getString(R.string.cancel_text), null)
                .create().show();
    }

    private void enableButton(boolean enabled, View... views) {
        for (View view : views) {
            view.setEnabled(enabled);
        }
    }

    private int timerDuration() {
        return (int) ((SystemClock.elapsedRealtime() - recordTimer.getBase()) / 1000);
    }

    private void setRecordBtn(boolean isRecording) {
        Drawable topDrawable;

        if (isRecording) {
            record.setText("");
            topDrawable = getResources().getDrawable(R.mipmap.mic_pause);
            record.setCompoundDrawables(null, topDrawable, null, null);
        } else {
            record.setText(getString(R.string.goon_record));
            topDrawable = getResources().getDrawable(R.mipmap.mic);
        }

        topDrawable.setBounds(0, 0, topDrawable.getMinimumWidth(), topDrawable.getMinimumHeight());
        record.setCompoundDrawables(null, topDrawable, null, null);
    }

    private void setSeekProgress(int totalDuration) {
        int progress = (int) (((float) timerDuration() / totalDuration) * 100);

        seekBar.setProgress(progress);

        if (progress >= 100) {
            recordTimer.stop();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        permissionHelper = new PermissionHelper(this);

        seekBar = findViewById(R.id.record_sb);
        playRecord = findViewById(R.id.record_play);
        record = findViewById(R.id.record_goon);
        repeatRecord = findViewById(R.id.record_reset);
        saveRecord = findViewById(R.id.record_save);
        recordTimer = findViewById(R.id.timer);

        playRecord.setOnClickListener(this);
        record.setOnClickListener(this);
        repeatRecord.setOnClickListener(this);
        saveRecord.setOnClickListener(this);

        enableButton(false, playRecord, seekBar, repeatRecord, saveRecord, record);

        audioDir = FileUtils.getAppRecordDir(this).getAbsolutePath();
        recordManager = RecorderManager.getInstance(audioDir);

        recordTimer.setOnChronometerTickListener(chronometer -> {

            // 超时
            if (timerDuration() >= TOTAL_TIME) {
                // 录音中
                if (recordManager.isStarted()) {
                    if (isRecording) {
                        // 录音暂停
                        pauseRecord();
                    }
                }

                recordTimer.stop();
                seekBar.setProgress(100);
                enableButton(false, record);

                return;
            }

            if (!MediaManager.isPlaying()) {
                setSeekProgress(TOTAL_TIME);
            } else {
                // 播放中
                setSeekProgress(audioDuration);
            }
        });

        checkPermission();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        MediaManager.release();

        if (recordManager != null) {
            recordManager.cancel();
            recordManager = null;
        }
    }

    public void convertAudio(String audioPath, String audioDir) {
                    AmrFileDecoder amrFileDecoder = new AmrFileDecoder();
        try {
            String wavFilePath = amrFileDecoder.amrToWav(new FileInputStream(new File(audioPath)), audioDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

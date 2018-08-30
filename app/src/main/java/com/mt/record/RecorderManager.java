package com.mt.record;

import android.media.MediaRecorder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

/**
 * 录音可以暂停。每次暂停都产生一个文件，最后保存的时候合并为一个文件
 */
public class RecorderManager {
    private static RecorderManager instance;

    private ArrayList<File> recList = new ArrayList<>();
    private MediaRecorder mediaRecorder = null;
    private String audioDir;// 录音所存放的文件夹位置
    private String audioPath; // 录音文件路径
    private boolean isStarted;

    private RecorderManager(String audioDir) {
        this.audioDir = audioDir;
    }

    public static RecorderManager getInstance(String dir) {
        if (instance == null) {
            synchronized (RecorderManager.class) {
                if (instance == null) {
                    instance = new RecorderManager(dir);
                }
            }
        }
        return instance;
    }

    /**
     * 开始录音
     *
     * @param init 录第一个文件 true， 暂停之后继续 false
     */
    public void startRecorder(boolean init) {

        if (init) {
            cleanFieArrayList(recList);
            isStarted = false;
        }

        stopRecorder();
        mediaRecorder = null;

        mediaRecorder = new MediaRecorder();
        File file = prepareRecorder();
        if (file != null) {
            recList.add(file);
        }
    }

    /**
     * 完成录音
     */
    public void release() {

        stopRecorder();
        mediaRecorder = null;
        isStarted = false;

        File file = getOutputVoiceFile(recList);
        if (file != null && file.length() > 0) {
            cleanFieArrayList(recList);
            audioPath = file.getAbsolutePath();
        }
    }

    /**
     * 重录
     * 删掉目录下所有文件
     */
    public void cancel() {

        stopRecorder();
        mediaRecorder = null;
        isStarted = false;
        cleanDirFie();

        if (audioPath != null) {
            File file = new File(audioPath);
            file.delete();
            audioPath = null;
        }
    }

    /**
     * 暂停
     */
    public void pause() {

        stopRecorder();
        mediaRecorder = null;
    }

    /**
     * 继续
     */
    public void resume() {

        startRecorder(false);
    }

    public boolean isStarted() {
        return isStarted;
    }

    /**
     * 点击播放的时候把之前录的文件合并，返回新的文件，从而实现录音暂停时播放
     *
     * @return 合并之后的录音文件
     */
    public String getAudioPath() {
        if (recList.isEmpty()) {
            return audioPath;
        }

        File file = getOutputVoiceFile(recList);
        if (file != null && file.length() > 0) {
            cleanFieArrayList(recList);
            recList.add(file);

            audioPath = file.getAbsolutePath();
        }

        return audioPath;
    }

    private void cleanDirFie() {
        File dirFiles = new File(audioDir);
        for (File file : dirFiles.listFiles()) {
            file.delete();
        }
    }

    /**
     * 合并录音
     */
    private File getOutputVoiceFile(ArrayList<File> list) {

        File recDirFile = new File(audioDir);

        // 创建音频文件,合并的文件放这里
        File resFile = new File(recDirFile, generalFileName());
        FileOutputStream fileOutputStream;

        if (!resFile.exists()) {
            try {
                resFile.createNewFile();
            } catch (IOException e) {
                return null;
            }
        }
        try {
            fileOutputStream = new FileOutputStream(resFile);
        } catch (IOException e) {
            return null;
        }
        // list里面为暂停录音 所产生的 几段录音文件的名字，中间几段文件的减去前面的6个字节头文件
        for (int i = 0; i < list.size(); i++) {
            File file = list.get(i);
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                byte[] myByte = new byte[fileInputStream.available()];
                // 文件长度
                int length = myByte.length;
                // 头文件
                if (i == 0) {
                    while (fileInputStream.read(myByte) != -1) {
                        fileOutputStream.write(myByte, 0, length);
                    }
                }
                // 之后的文件，去掉头文件就可以了
                else {
                    while (fileInputStream.read(myByte) != -1) {
                        fileOutputStream.write(myByte, 6, length - 6);
                    }
                }
                fileOutputStream.flush();
                fileInputStream.close();
            } catch (Exception e) {
                return null;
            }
        }
        // 结束后关闭流
        try {
            fileOutputStream.close();
        } catch (IOException e) {
        }

        return resFile;
    }

    private void cleanFieArrayList(ArrayList<File> list) {
        for (File file : list) {
            file.delete();
        }
        list.clear();
    }

    /**
     * 停止录音
     */
    private void stopRecorder() {
        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.release();
            }
        } catch (Exception e) {
        }
    }

    /**
     * 录音准备工作 ，开始录音
     */
    @SuppressWarnings("deprecation")
    private File prepareRecorder() {
        File recFile = null;
        if (mediaRecorder == null) return null;

        try {
            recFile = new File(audioDir, generalFileName());

            audioPath = recFile.getAbsolutePath();

            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(recFile.getAbsolutePath());
            mediaRecorder.setAudioSamplingRate(8000);
            mediaRecorder.prepare();
            mediaRecorder.start();

            isStarted = true;
        } catch (Exception e) {
        }
        return recFile;
    }

    /**
     * 随机生成文件的名称并用 MD5 处理
     */
    private String generalFileName() {
        return (UUID.randomUUID().toString()) + ".amr";
    }
}

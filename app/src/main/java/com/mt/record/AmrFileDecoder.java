package com.mt.record;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import io.kvh.media.amr.AmrDecoder;

/**
 * https://github.com/kevinho/opencore-amr-android
 */
public class AmrFileDecoder {
    private static final String TAG = "AmrFileDecoder";

    private static final String FILE_SUFFIX_NAME = ".pcm";

    // 20 ms second
    // 0.02 x 8000 x 2 = 320;160 short
    private static final int PCM_FRAME_SIZE = 160;
    private static final int AMR_FRAME_SIZE = 32;

    /**
     * amr 文件转码成 wav
     * @param inputStream amr 文件流
     * @param audioDir amr 文件目录
     * @return wav 文件路径
     * @throws IOException
     */
    public String amrToWav(InputStream inputStream, String audioDir) throws IOException {

        File resFile = new File(new File(audioDir), generalFileName());
        FileOutputStream fileOutputStream;
        if (!resFile.exists()) {
            resFile.createNewFile();
        }

        fileOutputStream = new FileOutputStream(resFile);

        long mDecoderState = AmrDecoder.init();

        byte[] readBuffer = new byte[AMR_FRAME_SIZE];

        //amr file has 6 bytes header: "23 21 41 4D 52 0A" => "#!amr.", so skip here
        try {
            inputStream.skip(6);
        } catch (IOException e) {

            e.printStackTrace();
        }

        while (inputStream.read(readBuffer) != -1) {
            // amr frame 32 bytes
            byte[] amrFrame = readBuffer.clone();
            // pcm frame 160 shorts
            short[] pcmFrame = new short[PCM_FRAME_SIZE];
            AmrDecoder.decode(mDecoderState, amrFrame, pcmFrame);

            byte[] pcmByte = new byte[pcmFrame.length * 2];
            ByteBuffer.wrap(pcmByte).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(pcmFrame);

            try {
                fileOutputStream.write(pcmByte);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        fileOutputStream.flush();
        fileOutputStream.close();

        AmrDecoder.exit(mDecoderState);

        String path = (resFile.getAbsolutePath());

        PcmToWavUtil pcmToWavUtil = new PcmToWavUtil();
        return pcmToWavUtil.pcmToWav(path);
    }

    /**
     * 随机生成文件的名称并用 MD5 处理
     */
    private String generalFileName() {
        return (UUID.randomUUID().toString()) + FILE_SUFFIX_NAME;
    }
}
package com.mt.record;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;

import androidx.core.content.ContextCompat;

public class StorageUtil {

    private final static String TAG = "StorageUtil";

    /**
     * @return 得到sd卡的存储路径
     */
    public static String getSDCardPath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    /**
     * 根据上下文(即：context)，得到其应用的存储路径
     *
     * @param context 上下文
     * @return 应用的存储路径
     */
    public static String getApplicationPath(Context context) {
        return context.getApplicationContext().getFilesDir().getAbsolutePath();
    }

    /**
     * 判断是否存在sd卡
     *
     * @return 有SD卡返回true，否则false
     */
    public static boolean hasSDCard() {
        return Environment.MEDIA_MOUNTED.equals(Environment
                .getExternalStorageState());
    }

    /**
     * 判断文件夹是否存在， 如果不存在会去创建，创建失败返回false
     *
     * @param filePath 文件路径
     * @return true or false
     */
    public static boolean isDirExist(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            if (file.mkdirs()) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }


    public static final void closeSilently(Closeable close) {
        if (close == null) {
            return;
        }

        try {
            close.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isHasSDCardPermission(Context context) {
        int checkResult = ContextCompat.checkSelfPermission(context, "android.permission.WRITE_EXTERNAL_STORAGE");
        if (checkResult != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return true;
    }

    public static String getPackName(Context context) {
        if (null == context) {
            return "logger";
        }
        String strPackName = context.getPackageName();
        if (null == strPackName || strPackName.length() <= 0) {
            return "logger";
        }
        return strPackName;
    }

    public static String getLogBasePath(Context context) {
        String basePath = null;
        if (hasSDCard() && isHasSDCardPermission(context)) {
            basePath = getSDCardPath();
        } else {
            basePath = getApplicationPath(context);
        }
        if (!basePath.endsWith("/")) {
            basePath += File.separator;
        }
        String appPackName = context.getPackageName();
        if (appPackName != null) {
            basePath += appPackName;
        } else {
            basePath += "logger";
        }
        basePath += File.separator;
		/*
		long timeValue = System.currentTimeMillis();
		CharSequence timeStr = DateFormat.format("yyyyMMdd",
				timeValue);
		basePath += timeStr + File.separator;
		*/
        return basePath;
    }

    public static String getLogSDCardPath(Context context) {
        String basePath = null;
        if (hasSDCard() && isHasSDCardPermission(context)) {
            basePath = getSDCardPath();
        } else {
            return null;
        }
        if (!basePath.endsWith("/")) {
            basePath += File.separator;
        }
        String appPackName = context.getPackageName();
        if (appPackName != null) {
            basePath += appPackName;
        } else {
            basePath += "logger";
        }
        basePath += File.separator;
		/*
		long timeValue = System.currentTimeMillis();
		CharSequence timeStr = DateFormat.format("yyyyMMdd",
				timeValue);
		basePath += timeStr + File.separator;
		*/
        return basePath;
    }

    public static boolean deleteFile(String delpath, List<String> excludeNames) {
        try {
            File file = new File(delpath);
            // 当且仅当此抽象路径名表示的文件存在且 是一个目录时，返回 true
            if (!file.isDirectory()) {
                file.delete();
            } else if (file.isDirectory()) {
                String[] filelist = file.list();
                for (int i = 0; i < filelist.length; i++) {
                    if (null != excludeNames && excludeNames.contains(filelist[i])) {
                        continue;
                    }
                    File delfile = new File(delpath + "/" + filelist[i]);
                    if (!delfile.isDirectory()) {
                        delfile.delete();
                    } else if (delfile.isDirectory()) {
                        deleteFile(delpath + "/" + filelist[i], null);
                    }
                }
                if (!file.getAbsolutePath().equals(delpath)) {
                    //选择不删除自身文件夹
                    file.delete();
                }
            }

        } catch (NullPointerException e) {
            System.out.println("deletefile() Exception:" + e.getMessage());
        }
        return true;
    }

    public static String getAgoraLogPath(Activity activity) {
        String basePath = getLogBasePath(activity);
        String agoraLogPath = basePath + "agora" + File.separator + "log.txt";
        createFile(agoraLogPath);
        return agoraLogPath;
    }

    /**
     * 创建 单个 文件
     *
     * @param filePath 待创建的文件路径
     */
    private static void createFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            return;
        }
        if (filePath.endsWith(File.separator)) {// 以 路径分隔符 结束，说明是文件夹
            return;
        }
        //判断父目录是否存在
        if (!file.getParentFile().exists() && !file.getParentFile().isDirectory()) {
            //父目录不存在 创建父目录
            if (!file.getParentFile().mkdirs()) {
                return;
            }
        }

        //创建目标文件
        try {
            if (file.createNewFile()) {//创建文件成功
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

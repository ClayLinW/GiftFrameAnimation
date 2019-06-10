package com.lin.clay.giftframeanimation.build;

import android.os.Environment;
import android.text.TextUtils;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class GainGiftUtils {

    public static GiftModel getGiftModel(String giftId) {
        GiftModel giftModel;
        File fileJson = new File(getFilePath(giftId, giftId + ".json"));
        if (fileJson.exists()) {
            String fileFromSD = getFileFromSD(fileJson.getAbsolutePath());
            Gson gson = new Gson();
            giftModel = gson.fromJson(fileFromSD, GiftModel.class);
            if (giftModel == null) {
                return null;
            }
            String backgroundMusic = giftModel.backgroundMusic;
            if (backgroundMusic != null) {
                File musicFile = new File(getFilePath(giftId, backgroundMusic));
                if (!musicFile.exists()) {
                    return null;
                } else {
                    giftModel.backgroundMusic = musicFile.getAbsolutePath();
                }
            }
            String backgroundColor = giftModel.backgroundColor;
            if (TextUtils.isEmpty(backgroundColor)) {
                giftModel.backgroundColor = "#f2ffffff";
            }
            List<String> imageArray = giftModel.imageArray;
            if (imageArray == null || imageArray.size() == 0) {
                return null;
            }
            List<String> imagePathList = new ArrayList<>();
            for (int i = 0; i < imageArray.size(); i++) {
                String imgName = imageArray.get(i);
                File fileImg = new File(getFilePath(giftId, imgName));
                if (!fileImg.exists()) {
                    return null;
                }
                imagePathList.add(fileImg.getAbsolutePath());
            }
            giftModel.imageArray = imagePathList;
        } else {
            return null;
        }
        return giftModel;
    }

    private static String getFileFromSD(String path) {
        String result = "";

        try {
            FileInputStream f = new FileInputStream(path);
            BufferedReader bis = new BufferedReader(new InputStreamReader(f));
            String line = "";
            while ((line = bis.readLine()) != null) {
                result += line;
            }
            f.close();
            bis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private static String getFilePath(String markGiftId, String fileName) {
        return getPathGifts() + markGiftId + File.separator + fileName;
    }

    private static String getPathGifts() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() +
                File.separator + "frameAnimation" + File.separator;
    }
}

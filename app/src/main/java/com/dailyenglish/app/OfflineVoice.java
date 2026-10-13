package com.dailyenglish.app;

import android.content.Context;
import android.content.res.AssetManager;
import com.k2fsa.sherpa.onnx.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class OfflineVoice {
    public static final String MODEL="voice/en_US-ryan-high.onnx";
    public static synchronized OfflineTts create(Context context) throws IOException {
        File directory=new File(context.getNoBackupFilesDir(),"voice-ryan-v1/espeak-ng-data");
        File marker=new File(directory.getParentFile(),"ready");
        if(!marker.exists()) {
            copy(context.getAssets(),"voice/espeak-ng-data",directory);
            if(!marker.createNewFile()) throw new IOException("语音数据初始化失败");
        }
        OfflineTtsVitsModelConfig vits=new OfflineTtsVitsModelConfig();
        vits.setModel(MODEL);vits.setTokens("voice/tokens.txt");vits.setDataDir(directory.getAbsolutePath());
        OfflineTtsModelConfig model=new OfflineTtsModelConfig();model.setVits(vits);model.setNumThreads(2);model.setDebug(false);
        OfflineTtsConfig config=new OfflineTtsConfig();config.setModel(model);
        return new OfflineTts(context.getAssets(),config);
    }
    private static void copy(AssetManager assets,String source,File destination) throws IOException {
        String[] children=assets.list(source);
        if(children!=null&&children.length>0) {
            if(!destination.isDirectory()&&!destination.mkdirs()) throw new IOException("无法创建语音目录");
            for(String child:children) copy(assets,source+"/"+child,new File(destination,child));
        } else {
            try(InputStream input=assets.open(source)) { Files.copy(input,destination.toPath(),StandardCopyOption.REPLACE_EXISTING); }
        }
    }
}

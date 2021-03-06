package cn.protruly.spiderman.action;

import android.content.Context;

import java.io.File;

/**
 * Created by lijia on 17-6-9.
 */

public class LowmemAction extends Action {

    public LowmemAction(Context context, String tag, String packageName, String erroReason, String filePath) {
        super(context);
        collectLogFile(tag, packageName, erroReason, filePath);
    }

    public void collectLogFile(String tag, String pg, String es, String filePath) {

        copyTraceAndTombstoneFile(ANRFilePath, filePath, mSpiderManLogStoragePath);
        copyLogdFile(LogdFilePath);
        collectErrorInfo(targetPath, tag, pg, es);
        zipFileDirectory(new File(targetPath));

    }

}

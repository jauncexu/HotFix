package com.example.hotfix;

import android.app.Application;
import android.content.Context;

import com.enjoy.patch.EnjoyFix;

import java.io.File;

public class MyApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        // 调用热修复修复Utils的错误代码
        EnjoyFix.installPatch(this, new File("/sdcard/patch.jar"));
    }
}

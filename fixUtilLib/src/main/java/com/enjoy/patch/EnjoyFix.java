package com.enjoy.patch;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import dalvik.system.DexClassLoader;


public class EnjoyFix {
    private static final String TAG = "EnjoyFix";

    /**
     * 1、获取程序的PathClassLoader对象
     * 2、反射获得PathClassLoader父类BaseDexClassLoader的pathList对象
     * 3、反射获取pathList的dexElements对象 （oldElement）
     * 4、把补丁包变成Element数组：patchElement（反射执行makePathElements）
     * 5、合并patchElement+oldElement = newElement （Array.newInstance）
     * 6、反射把oldElement赋值成newElement
     *
     * @param application
     * @param patch
     */
    public static void installPatch(Application application, File patch) {
        if (!patch.exists()) {
            return;
        }

        //1、获取程序的PathClassLoader对象
        ClassLoader classLoader = application.getClassLoader();
        //2、反射获得PathClassLoader父类BaseDexClassLoader的pathList对象
        try {
            Field pathListField = ShareReflectUtil.findField(classLoader, "pathList");
            Object pathList = pathListField.get(classLoader);
            //3、反射获取pathList的dexElements对象 （oldElement）
            Field dexElementsField = ShareReflectUtil.findField(pathList, "dexElements");
            Object[] oldElements = (Object[]) dexElementsField.get(pathList);
            //4、把补丁包变成Element数组：patchElement（反射执行makePathElements）
            Object[] patchElements = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Method makePathElements = ShareReflectUtil.findMethod(pathList, "makePathElements", List.class, File.class, List.class);
                List<File> patchs = new ArrayList<>();
                patchs.add(patch);
                ArrayList<IOException> ioExceptions = new ArrayList<>();
                patchElements = (Object[]) makePathElements.invoke(pathList, patchs, application.getCacheDir(), ioExceptions);
                //5、合并patchElement+oldElement = newElement （Array.newInstance）
                //创建一个新数组，大小 oldElements+patchElements
                // int[].class.getComponentType() == int.class
                Object[] newElements = (Object[]) Array.newInstance(oldElements.getClass().getComponentType(), oldElements.length + patchElements.length);

                System.arraycopy(patchElements, 0, newElements, 0, patchElements.length);
                System.arraycopy(oldElements, 0, newElements, patchElements.length, oldElements.length);
                //6、反射把oldElement赋值成newElement
                dexElementsField.set(pathList, newElements);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
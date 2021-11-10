package com.ulink;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.umeng.commonsdk.UMConfigure;
import com.umeng.umlink.MobclickLink;
import com.umeng.umlink.UMLinkListener;
import java.util.HashMap;

public class UlinkModule extends ReactContextBaseJavaModule {
  private static ReactApplicationContext mContext;
  private HashMap<String, String> mInstall_params;

  public UlinkModule(ReactApplicationContext reactContext) {
    super(reactContext);
    mContext = reactContext;
  }

  @Override
  public String getName() {
    return "Ulink";
  }

  @ReactMethod
  public void init(String appKey, Boolean LogEnabled) {
    UMConfigure.setLogEnabled(LogEnabled);
    UMConfigure.init(
      mContext,
      appKey,
      "Umeng",
      UMConfigure.DEVICE_TYPE_PHONE,
      null
    );
    UMConfigure.setProcessEvent(true); //支持多进程打点.默认不支持
  }

  @ReactMethod
  public void handlerLink() {
    try {
      Activity currentActivity = getCurrentActivity();
      String initialURL = null;
      if (currentActivity != null) {
        Uri data = currentActivity.getIntent().getData();
        if (data != null) {
          MobclickLink.handleUMLinkURI(mContext, data, umlinkAdapter);
        }

        SharedPreferences sp = mContext.getSharedPreferences(
          "MY_PREFERENCE",
          Context.MODE_PRIVATE
        );
        boolean hasGetInstallParams = sp.getBoolean(
          "key_Has_Get_InstallParams",
          false
        );
        if (hasGetInstallParams == false) {
          //从来没调用过getInstallParam方法，适当延时调用getInstallParam方法
          new Handler()
          .postDelayed(
              new Runnable() {

                @Override
                public void run() {
                  MobclickLink.getInstallParams(mContext, umlinkAdapter);
                }
              },
              2000
            ); //2秒后执行
          //MobclickLink.getInstallParams(mContext, umlinkAdapter);

          //在9.3.3版本中，由于要检查SDK是否初始化成功，所以可能需要3秒乃至更长的延迟才能调用getInstallParams
          //在9.3.6以后版本中，不再检查SDK是否初始化成功，可以不用延迟

        } else {
          //已经调用过getInstallParam方法，没必要在下次启动时再调用
          //但后续仍可在需要时调用，比如demo中的按钮点击
        }
      }
    } catch (Exception e) {
      //   promise.reject(
      //     new JSApplicationIllegalArgumentException(
      //       "Could not get the initial URL : " + e.getMessage()
      //     )
      //   );
    }
  }

  UMLinkListener umlinkAdapter = new UMLinkListener() {

    @Override
    public void onLink(String path, HashMap<String, String> query_params) {
      android.util.Log.i("mob", "-----onLink-----" + path);
      WritableMap params = Arguments.createMap();
      if (!path.isEmpty()) {
        params.putString("path", path);
      }
      if (!query_params.isEmpty()) {
        for (String key : query_params.keySet()) {
          android.util.Log.i(
            "mob",
            "-----query_params_bundle-----" + key + query_params.get(key)
          );
          params.putString(key, query_params.get(key));
        }

        sendEvent("onLink", params);
      }
      if (mInstall_params != null && !mInstall_params.isEmpty()) {
        for (String key : mInstall_params.keySet()) {
          android.util.Log.i(
            "mob",
            "-----mInstall_params-----" + key + mInstall_params.get(key)
          );
          params.putString(key, mInstall_params.get(key));
        }

        sendEvent("onLink", params);
      }
    }

    @Override
    public void onInstall(HashMap<String, String> install_params, Uri uri) {
      android.util.Log.i("mob", "-----onInstall-----" + uri.toString());
      if (install_params.isEmpty() && uri.toString().isEmpty()) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage("没有匹配到安装参数");

        builder.show();
      } else {
        if (!install_params.isEmpty()) {
          mInstall_params = install_params;
        }
        if (!uri.toString().isEmpty()) {
          MobclickLink.handleUMLinkURI(mContext, uri, umlinkAdapter);
        }
      }
      SharedPreferences.Editor sp = mContext
        .getSharedPreferences("MY_PREFERENCE", Context.MODE_PRIVATE)
        .edit();
      sp.putBoolean("key_Has_Get_InstallParams", true);
      sp.commit();
    }

    @Override
    public void onError(String error) {
      android.util.Log.i("mob", error);
      AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
      builder.setMessage(error);

      builder.show();
    }
  };

  // 注册监听方法
  private static void sendEvent(String eventName, WritableMap params) {
    mContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
      .emit(eventName, params);
  }
}

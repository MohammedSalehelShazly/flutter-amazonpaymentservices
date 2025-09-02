package com.amazon.flutter_amazonpaymentservices;

import android.app.Activity;
import android.content.Context;
import android.provider.Settings;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/**
 * FlutterAmazonpaymentservicesPlugin
 *
 * V2 embedding (no Registrar).
 * - Channel name MUST match what you use in Dart: "flutter_amazonpaymentservices"
 */
public class FlutterAmazonpaymentservicesPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {

  private MethodChannel channel;
  private Context applicationContext;
  private Activity activity;

  // === FlutterPlugin ===
  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
    applicationContext = binding.getApplicationContext();
    channel = new MethodChannel(binding.getBinaryMessenger(), "flutter_amazonpaymentservices");
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    if (channel != null) {
      channel.setMethodCallHandler(null);
      channel = null;
    }
    applicationContext = null;
  }

  // === ActivityAware ===
  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    activity = binding.getActivity();
    // NOTE: لو محتاج ActivityResult لاحقًا:
    // binding.addActivityResultListener(...);
  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    activity = binding.getActivity();
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    activity = null;
  }

  @Override
  public void onDetachedFromActivity() {
    activity = null;
  }

  // === Method channel ===
  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    switch (call.method) {

      case "getUDID": {
        // بديل سريع شغال لرقم جهاز أندرويد (لو عندك طريقة APS مختلفة استبدلها)
        try {
          String udid = Settings.Secure.getString(
              applicationContext.getContentResolver(),
              Settings.Secure.ANDROID_ID
          );
          result.success(udid);
        } catch (Exception e) {
          result.error("udid_error", e.getMessage(), null);
        }
        break;
      }

      case "validateApi": {
        // TODO: انقل هنا منطقك الحالي للتعامل مع APS SDK
        // result.success(<map/response>);
        result.notImplemented();
        break;
      }

      default:
        result.notImplemented();
    }
  }
}

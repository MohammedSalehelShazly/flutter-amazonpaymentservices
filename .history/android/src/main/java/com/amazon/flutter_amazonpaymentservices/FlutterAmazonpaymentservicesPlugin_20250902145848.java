package com.amazon.flutter_amazonpaymentservices;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.payfort.fortpaymentsdk.FortSdk;
import com.payfort.fortpaymentsdk.callbacks.FortCallBackManager;
import com.payfort.fortpaymentsdk.callbacks.FortCallback;
import com.payfort.fortpaymentsdk.callbacks.FortInterfaces;
import com.payfort.fortpaymentsdk.callbacks.PayFortCallback;
import com.payfort.fortpaymentsdk.domain.model.FortRequest;

import java.util.HashMap;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

/**
 * V2 embedding implementation (no Registrar).
 */
public class FlutterAmazonpaymentservicesPlugin implements FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {

  private static final String METHOD_CHANNEL_KEY = "flutter_amazonpaymentservices";
  private static final int PAYFORT_REQUEST_CODE = 1166;

  private MethodChannel methodChannel;
  private static Activity activity;
  private static FortCallBackManager fortCallback;

  // لو عندك enum Constants.ENVIRONMENTS_VALUES في مشروعك، هنستخدمه زي ما عندك
  private Constants.ENVIRONMENTS_VALUES mEnvironment = Constants.ENVIRONMENTS_VALUES.SANDBOX;

  // ===== FlutterPlugin =====
  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
    methodChannel = new MethodChannel(binding.getBinaryMessenger(), METHOD_CHANNEL_KEY);
    methodChannel.setMethodCallHandler(this);
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    if (methodChannel != null) {
      methodChannel.setMethodCallHandler(null);
      methodChannel = null;
    }
  }

  // ===== ActivityAware =====
  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    activity = binding.getActivity();
    binding.addActivityResultListener((requestCode, resultCode, data) -> {
      if (requestCode == PAYFORT_REQUEST_CODE) {
        if (data != null && (resultCode == RESULT_OK || resultCode == RESULT_CANCELED)) {
          if (fortCallback != null) fortCallback.onActivityResult(requestCode, resultCode, data);
        } else {
          Intent intent = new Intent();
          if (fortCallback != null) fortCallback.onActivityResult(requestCode, resultCode, intent);
        }
        return true;
      }
      return false;
    });
  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    onAttachedToActivity(binding);
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    activity = null;
  }

  @Override
  public void onDetachedFromActivity() {
    activity = null;
  }

  // ===== Channel methods =====
  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
    switch (call.method) {
      case "getUDID": {
        try {
          String udid = FortSdk.getDeviceId(activity);
          result.success(udid);
        } catch (Exception e) {
          result.error("udid_error", e.getMessage(), null);
        }
        break;
      }

      case "validateApi": {
        handleValidateAPI(call, result);
        break;
      }

      case "normalPay": {
        handleOpenFullScreenPayfort(call, result);
        break;
      }

      default:
        result.notImplemented();
    }
  }

  private void handleValidateAPI(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
    try {
      // استقبل البراميترز بنفس الشكل القديم:
      // environmentType ("production" | "sandbox")
      // requestParam (Map) — هو نفسه اللي فيه sdk_token وغيره
      String env = "sandbox";
      if (call.hasArgument("environmentType") && call.argument("environmentType") != null) {
        env = String.valueOf(call.argument("environmentType"));
      }

      if ("production".equalsIgnoreCase(env)) {
        mEnvironment = Constants.ENVIRONMENTS_VALUES.PRODUCTION;
      } else {
        mEnvironment = Constants.ENVIRONMENTS_VALUES.SANDBOX;
      }

      @SuppressWarnings("unchecked")
      HashMap<String, Object> requestParamMap = call.hasArgument("requestParam")
          ? (HashMap<String, Object>) call.argument("requestParam")
          : (HashMap<String, Object>) call.arguments; // fallback لو الـ Dart بيبعت الخريطة مباشرةً

      FortRequest fortRequest = new FortRequest();
      fortRequest.setRequestMap(requestParamMap);

      // ✅ استخدم البيئة المختارة (بدل TEST الثابتة)
      FortSdk.getInstance().validate(
          activity,
          mEnvironment.getSdkEnvironemt(),
          fortRequest,
          new PayFortCallback() {
            @Override public void startLoading() { }
            @Override public void onSuccess(@NonNull Map<String, ?> fortResponseMap, @NonNull Map<String, ?> map1) {
              result.success(fortResponseMap);
            }
            @Override public void onFailure(@NonNull Map<String, ?> fortResponseMap, @NonNull Map<String, ?> map1) {
              result.error("onFailure", "onFailure", fortResponseMap);
            }
          });

    } catch (Exception e) {
      HashMap<Object, Object> errorDetails = new HashMap<>();
      errorDetails.put("response_message", e.getMessage());
      result.error("validate_exception", "Exception in validateApi", errorDetails);
    }
  }

  private void handleOpenFullScreenPayfort(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
    try {
      String env = "sandbox";
      if (call.hasArgument("environmentType") && call.argument("environmentType") != null) {
        env = String.valueOf(call.argument("environmentType"));
      }
      if ("production".equalsIgnoreCase(env)) {
        mEnvironment = Constants.ENVIRONMENTS_VALUES.PRODUCTION;
      } else {
        mEnvironment = Constants.ENVIRONMENTS_VALUES.SANDBOX;
      }

      boolean isShowResponsePage = false;
      if (call.hasArgument("isShowResponsePage") && call.argument("isShowResponsePage") != null) {
        Boolean b = call.argument("isShowResponsePage");
        isShowResponsePage = b != null && b;
      }

      @SuppressWarnings("unchecked")
      HashMap<String, Object> requestParamMap = call.hasArgument("requestParam")
          ? (HashMap<String, Object>) call.argument("requestParam")
          : (HashMap<String, Object>) call.arguments;

      FortRequest fortRequest = new FortRequest();
      fortRequest.setShowResponsePage(isShowResponsePage);
      fortRequest.setRequestMap(requestParamMap);

      if (fortCallback == null) fortCallback = FortCallback.Factory.create();

      FortSdk.getInstance().registerCallback(
          activity,
          fortRequest,
          mEnvironment.getSdkEnvironemt(), // زي ما كان عندك
          PAYFORT_REQUEST_CODE,
          fortCallback,
          true,
          new FortInterfaces.OnTnxProcessed() {
            @Override public void onCancel(Map<String, Object> requestParamsMap, Map<String, Object> responseMap) {
              result.error("onCancel", "onCancel", responseMap);
            }
            @Override public void onSuccess(Map<String, Object> requestParamsMap, Map<String, Object> fortResponseMap) {
              result.success(fortResponseMap);
            }
            @Override public void onFailure(Map<String, Object> requestParamsMap, Map<String, Object> fortResponseMap) {
              result.error("onFailure", "onFailure", fortResponseMap);
            }
          });

    } catch (Exception e) {
      HashMap<Object, Object> errorDetails = new HashMap<>();
      errorDetails.put("response_message", e.getMessage());
      result.error("onFailure", "onFailure", errorDetails);
    }
  }
}

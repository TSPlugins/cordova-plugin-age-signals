package com.example.agesignals;

import android.app.Activity;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.play.agesignals.AgeSignalsAccessRequest;
import com.google.android.play.agesignals.AgeSignalsManager;
import com.google.android.play.agesignals.AgeSignalsManagerFactory;
import com.google.android.play.agesignals.AgeSignalsRequest;
import com.google.android.play.agesignals.AgeSignalsResult;
import com.google.android.play.agesignals.model.AgeSignalsStatus;

/**
 * Android side: Play Age Signals API 0.0.4 (two-call flow, as in Google's docs):
 *   1. requestAgeSignalsAccess(AgeSignalsAccessRequest) -> SHARED / NOT_SHARED / VERIFICATION_REQUIRED
 *   2. checkAgeSignals(AgeSignalsRequest)               -> ageLower / ageUpper / ageRangeSource / installId
 *
 * If the compiler says it cannot find AgeSignalsStatus or AgeSignalsAccessRequest, fix the two
 * import lines above (the exact package is not shown in Google's code sample).
 */
public class AgeSignalsPlugin extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext cb) throws JSONException {
        if ("isAvailable".equals(action)) {
            JSONObject o = new JSONObject();
            o.put("available", true); // real availability is reported by the API errors
            cb.success(o);
            return true;
        }
        if ("requestAgeRange".equals(action)) {
            // The Play prompt needs a foreground Activity; start from the UI thread.
            cordova.getActivity().runOnUiThread(() -> requestAgeRange(cb));
            return true;
        }
        return false;
    }

    private void requestAgeRange(final CallbackContext cb) {
        try {
            final Activity activity = cordova.getActivity();
            final AgeSignalsManager manager =
                AgeSignalsManagerFactory.create(activity.getApplicationContext());

            // Step 1: ask for access (Play may show its age-sharing prompt)
            AgeSignalsAccessRequest accessRequest =
                AgeSignalsAccessRequest.builder().setActivity(activity).build();

            manager.requestAgeSignalsAccess(accessRequest)
                .addOnSuccessListener(accessResult -> {
                    Integer status = accessResult.ageSignalsStatus();
                    if (status != null && status == AgeSignalsStatus.SHARED) {
                        readSignals(manager, cb);              // Step 2
                    } else if (status != null && status == AgeSignalsStatus.VERIFICATION_REQUIRED) {
                        sendStatusOnly(cb, "VERIFICATION_REQUIRED");
                    } else {
                        sendStatusOnly(cb, "NOT_SHARED");
                    }
                })
                .addOnFailureListener(e -> fail(cb, "ACCESS_ERROR", e));

        } catch (Exception e) {
            fail(cb, "AGE_SIGNALS_EXCEPTION", e);
        }
    }

    // Step 2: read the actual signals once sharing is active
    private void readSignals(AgeSignalsManager manager, final CallbackContext cb) {
        manager.checkAgeSignals(AgeSignalsRequest.builder().build())
            .addOnSuccessListener(result -> sendResult(cb, result))
            .addOnFailureListener(e -> fail(cb, "CHECK_ERROR", e));
    }

    private void sendResult(CallbackContext cb, AgeSignalsResult result) {
        try {
            Integer lower = result.ageLower();
            Integer upper = result.ageUpper();
            Integer source = result.ageRangeSource();
            String installId = result.installId();

            JSONObject o = new JSONObject();
            o.put("status", "SHARED");
            o.put("lower", lower == null ? JSONObject.NULL : lower);   // null = not sharing / verified adult
            o.put("upper", upper == null ? JSONObject.NULL : upper);   // null = open-ended top band (e.g. 18+)
            o.put("source", source == null ? JSONObject.NULL : String.valueOf(source));
            o.put("installId", installId == null ? JSONObject.NULL : installId);
            cb.success(o);
        } catch (JSONException e) {
            fail(cb, "JSON_ERROR", e);
        }
    }

    private void sendStatusOnly(CallbackContext cb, String status) {
        try {
            JSONObject o = new JSONObject();
            o.put("status", status);
            o.put("lower", JSONObject.NULL);
            o.put("upper", JSONObject.NULL);
            o.put("source", JSONObject.NULL);
            o.put("installId", JSONObject.NULL);
            cb.success(o);
        } catch (JSONException e) {
            fail(cb, "JSON_ERROR", e);
        }
    }

    private void fail(CallbackContext cb, String code, Exception e) {
        try {
            JSONObject o = new JSONObject();
            o.put("code", code);
            o.put("message", e == null || e.getMessage() == null ? "" : e.getMessage());
            cb.error(o);
        } catch (JSONException ignored) {
            cb.error(code);
        }
    }
}

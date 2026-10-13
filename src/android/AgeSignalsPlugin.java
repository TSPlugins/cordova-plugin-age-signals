package com.example.agesignals;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.play.agesignals.AgeSignalsManager;
import com.google.android.play.agesignals.AgeSignalsManagerFactory;
import com.google.android.play.agesignals.AgeSignalsRequest;
import com.google.android.play.agesignals.AgeSignalsResult;

/**
 * Android side: Play Age Signals API.
 *
 * !!! TWO THINGS YOU MUST VERIFY AGAINST GOOGLE'S DOCS (SDK is beta and changing) !!!
 *  1. Step 1 below: since age-signals 0.0.4 Google splits the flow in two calls:
 *       requestAgeSignalsAccess(Activity)  -> SHARED / NOT_SHARED / VERIFICATION_REQUIRED
 *       checkAgeSignals(...)               -> actual signals
 *     Copy the exact call + result type from
 *     https://developer.android.com/google/play/age-signals/request-age-signals
 *     and wire it into requestAccess(). Until you do, this plugin goes straight to
 *     checkAgeSignals(), so Play will never show its sharing prompt and you will never
 *     see VERIFICATION_REQUIRED.
 *  2. Accessor names on AgeSignalsResult for your library version.
 */
public class AgeSignalsPlugin extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext cb) throws JSONException {
        if ("isAvailable".equals(action)) {
            JSONObject o = new JSONObject();
            o.put("available", true); // real availability is reported by the API errors
            cb.success(o);
            return true;
        }
        if ("requestAgeRange".equals(action)) {
            cordova.getThreadPool().execute(() -> requestAgeRange(cb));
            return true;
        }
        return false;
    }

    private void requestAgeRange(CallbackContext cb) {
        try {
            AgeSignalsManager manager = AgeSignalsManagerFactory.create(cordova.getContext());

            // ---- STEP 1 (TODO, see class comment): ask for access --------------------
            // String access = requestAccess(manager);   // SHARED | NOT_SHARED | VERIFICATION_REQUIRED
            // if ("NOT_SHARED".equals(access) || "VERIFICATION_REQUIRED".equals(access)) {
            //     JSONObject o = new JSONObject();
            //     o.put("status", access);
            //     cb.success(o);
            //     return;
            // }

            // ---- STEP 2: read the signals -------------------------------------------
            manager.checkAgeSignals(AgeSignalsRequest.builder().build())
                .addOnSuccessListener(result -> sendResult(cb, result))
                .addOnFailureListener(e -> fail(cb, "AGE_SIGNALS_ERROR", e.getMessage()));

        } catch (Exception e) {
            fail(cb, "AGE_SIGNALS_EXCEPTION", e.getMessage());
        }
    }

    private void sendResult(CallbackContext cb, AgeSignalsResult result) {
        try {
            JSONObject o = new JSONObject();
            Integer lower = result.ageLower();
            Integer upper = result.ageUpper();
            Object userStatus = result.userStatus();

            o.put("status", "SHARED");
            o.put("lower", lower == null ? JSONObject.NULL : lower);
            o.put("upper", upper == null ? JSONObject.NULL : upper);
            o.put("source", userStatus == null ? JSONObject.NULL : String.valueOf(userStatus));
            o.put("installId", result.installId() == null ? JSONObject.NULL : result.installId());
            cb.success(o);
        } catch (JSONException e) {
            fail(cb, "JSON_ERROR", e.getMessage());
        }
    }

    private void fail(CallbackContext cb, String code, String message) {
        try {
            JSONObject o = new JSONObject();
            o.put("code", code);
            o.put("message", message == null ? "" : message);
            cb.error(o);
        } catch (JSONException ignored) {
            cb.error(code);
        }
    }
}

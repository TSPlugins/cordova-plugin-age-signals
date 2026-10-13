# cordova-plugin-age-signals (scaffold, untested)

One Cordova plugin, one JS API, for:
- Android: Google Play Age Signals API (beta)
- iOS 26+: Apple Declared Age Range API

```js
cordova.plugins.AgeSignals.requestAgeRange([13, 16, 18], onResult, onError);
```

## Before you build
1. Android: wire Step 1 (`requestAgeSignalsAccess`) in `AgeSignalsPlugin.java` from Google's docs.
2. iOS: compile with Xcode 26+ SDK (check your MABS version's release notes). Fix any API-label drift.
3. Enable the "Declared Age Range" capability on your App ID and regenerate the provisioning profile.
4. Android minSdk must be >= 23; app must be installed from Google Play to get real signals.

## Use in OutSystems O11
Extensibility Configurations of your plugin module:
```json
{ "plugin": { "url": "https://github.com/YOUR_ORG/cordova-plugin-age-signals.git#v0.1.0" } }
```

JavaScript node (async) inside a Client Action `RequestAgeRange`
(outputs: Status Text, Lower Integer, Upper Integer, Source Text, ErrorCode Text, ErrorMessage Text):
```js
var p = window.cordova && cordova.plugins && cordova.plugins.AgeSignals;
if (!p) {
  $parameters.ErrorCode = "PLUGIN_MISSING"; $parameters.Status = "";
  $parameters.Lower = -1; $parameters.Upper = -1;
  $resolve();
} else {
  p.requestAgeRange([13, 16, 18], function (r) {
    $parameters.Status = r.status || "";
    $parameters.Lower  = (r.lower === null || r.lower === undefined) ? -1 : r.lower;
    $parameters.Upper  = (r.upper === null || r.upper === undefined) ? -1 : r.upper;
    $parameters.Source = r.source || "";
    $parameters.ErrorCode = ""; $parameters.ErrorMessage = "";
    $resolve();
  }, function (e) {
    $parameters.Status = ""; $parameters.Lower = -1; $parameters.Upper = -1;
    $parameters.ErrorCode = (e && e.code) || "UNKNOWN";
    $parameters.ErrorMessage = (e && e.message) || "";
    $resolve();
  });
}
```

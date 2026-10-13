var exec = require('cordova/exec');
var SERVICE = 'AgeSignals';

/**
 * Result shape (both platforms):
 *  { status: 'SHARED' | 'NOT_SHARED' | 'VERIFICATION_REQUIRED' | 'NOT_SUPPORTED',
 *    lower: number|null, upper: number|null,
 *    source: string|null,        // Android: userStatus / iOS: age range declaration
 *    installId: string|null }    // Android supervised users only
 * Errors: { code: string, message: string }
 */
module.exports = {
  isAvailable: function (success, error) {
    exec(success, error, SERVICE, 'isAvailable', []);
  },
  // ageGates is used on iOS only (1-3 ascending ages, >= 2 years apart)
  requestAgeRange: function (ageGates, success, error) {
    exec(success, error, SERVICE, 'requestAgeRange', [ageGates || [13, 16, 18]]);
  }
};

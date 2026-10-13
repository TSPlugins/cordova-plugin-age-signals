import Foundation
import UIKit
#if canImport(DeclaredAgeRange)
import DeclaredAgeRange
#endif

/// iOS side: Apple Declared Age Range API (iOS 26+).
/// Compile-guarded so it still builds with older SDKs, and runtime-guarded so the app
/// still runs on iOS < 26 (returns NOT_SUPPORTED there).
@objc(AgeSignalsPlugin)
class AgeSignalsPlugin: CDVPlugin {

    @objc(isAvailable:)
    func isAvailable(_ command: CDVInvokedUrlCommand) {
        var available = false
        #if canImport(DeclaredAgeRange)
        if #available(iOS 26.0, *) { available = true }
        #endif
        ok(command, ["available": available])
    }

    @objc(requestAgeRange:)
    func requestAgeRange(_ command: CDVInvokedUrlCommand) {
        let gates = (command.argument(at: 0) as? [Int]) ?? [13, 16, 18]

        #if canImport(DeclaredAgeRange)
        if #available(iOS 26.0, *) {
            Task { @MainActor in
                guard let vc = self.viewController, !gates.isEmpty else {
                    self.fail(command, "PRESENTATION_CONTEXT_UNAVAILABLE", "No view controller or age gates")
                    return
                }
                do {
                    let response = try await AgeRangeService.shared.requestAgeRange(
                        ageGates: gates[0],
                        gates.count > 1 ? gates[1] : nil,
                        gates.count > 2 ? gates[2] : nil,
                        in: vc
                    )
                    switch response {
                    case .sharing(let range):
                        // lowerBound/upperBound may be nil (below lowest gate / at or above highest gate)
                        let lower: Any = range.lowerBound.map { $0 as Any } ?? NSNull()
                        let upper: Any = range.upperBound.map { $0 as Any } ?? NSNull()
                        let source: Any = range.ageRangeDeclaration.map { String(describing: $0) as Any } ?? NSNull()
                        self.ok(command, ["status": "SHARED", "lower": lower, "upper": upper,
                                          "source": source, "installId": NSNull()])
                    case .declinedSharing:
                        self.ok(command, ["status": "NOT_SHARED", "lower": NSNull(), "upper": NSNull(),
                                          "source": NSNull(), "installId": NSNull()])
                    @unknown default:
                        self.ok(command, ["status": "NOT_SHARED"])
                    }
                } catch {
                    self.fail(command, "AGE_RANGE_ERROR", String(describing: error))
                }
            }
            return
        }
        #endif

        ok(command, ["status": "NOT_SUPPORTED", "lower": NSNull(), "upper": NSNull(),
                     "source": NSNull(), "installId": NSNull()])
    }

    // MARK: helpers
    private func ok(_ command: CDVInvokedUrlCommand, _ dict: [String: Any]) {
        let r = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: dict)
        self.commandDelegate.send(r, callbackId: command.callbackId)
    }

    private func fail(_ command: CDVInvokedUrlCommand, _ code: String, _ message: String) {
        let r = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: ["code": code, "message": message])
        self.commandDelegate.send(r, callbackId: command.callbackId)
    }
}

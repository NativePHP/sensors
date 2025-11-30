import Foundation
import MLKitFaceDetection
import MLKitVision
import UIKit

enum FaceDetectionFunctions {

    class Detect: BridgeFunction {
        func execute(parameters: [String: Any]) throws -> [String: Any] {
            let id = parameters["id"] as? String
            var uiImage: UIImage?

            // Try to load from file path first (preserves EXIF orientation)
            if let path = parameters["path"] as? String {
                uiImage = UIImage(contentsOfFile: path)
            }
            // Fall back to base64
            else if var base64Image = parameters["image"] as? String {
                // Strip data URL prefix if present
                if let range = base64Image.range(of: ";base64,") {
                    base64Image = String(base64Image[range.upperBound...])
                }
                if let imageData = Data(base64Encoded: base64Image) {
                    uiImage = UIImage(data: imageData)
                }
            }

            guard let image = uiImage else {
                return BridgeResponse.error(code: "INVALID_IMAGE", message: "Could not load image")
            }

            let visionImage = VisionImage(image: image)
            visionImage.orientation = image.imageOrientation

            let options = FaceDetectorOptions()
            options.performanceMode = .accurate
            options.landmarkMode = .all
            options.classificationMode = .all

            let faceDetector = FaceDetector.faceDetector(options: options)
            let eventClass = "Nativephp\\FaceDetection\\Events\\FaceDetectionCompleted"

            do {
                let faces = try faceDetector.results(in: visionImage)

                let faceData = faces.map { face -> [String: Any] in
                    // Convert probabilities to whole percentages
                    // Note: -1 means "not computed", so we pass nil in that case
                    func formatProbability(_ value: CGFloat) -> Any {
                        if value < 0 { return NSNull() }
                        return Int(round(value * 100))  // e.g., 0.9929 -> 99
                    }

                    return [
                        "bounds": [
                            "x": round(face.frame.origin.x),
                            "y": round(face.frame.origin.y),
                            "width": round(face.frame.size.width),
                            "height": round(face.frame.size.height)
                        ],
                        "smilingProbability": formatProbability(face.smilingProbability),
                        "leftEyeOpenProbability": formatProbability(face.leftEyeOpenProbability),
                        "rightEyeOpenProbability": formatProbability(face.rightEyeOpenProbability),
                        "headEulerAngleY": round(face.headEulerAngleY * 10) / 10,
                        "headEulerAngleZ": round(face.headEulerAngleZ * 10) / 10
                    ]
                }

                var payload: [String: Any] = [
                    "success": true,
                    "faces": faceData,
                    "count": faces.count
                ]
                if let id = id { payload["id"] = id }

                DispatchQueue.main.async {
                    LaravelBridge.shared.send?(eventClass, payload)
                }
            } catch {
                var payload: [String: Any] = [
                    "success": false,
                    "error": error.localizedDescription
                ]
                if let id = id { payload["id"] = id }

                DispatchQueue.main.async {
                    LaravelBridge.shared.send?(eventClass, payload)
                }
            }

            return [:]
        }
    }

    class IsAvailable: BridgeFunction {
        func execute(parameters: [String: Any]) throws -> [String: Any] {
            return BridgeResponse.success(data: ["available": true])
        }
    }
}

package com.example.androidphp.bridge.plugins.FaceDetection

import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.example.androidphp.bridge.BridgeFunction
import com.example.androidphp.bridge.BridgeResponse
import com.example.androidphp.utils.NativeActionCoordinator
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import org.json.JSONArray
import org.json.JSONObject

/**
 * Face detection functions using Google ML Kit
 * Namespace: "FaceDetection.*"
 */
object FaceDetectionFunctions {

    private const val TAG = "FaceDetectionFunctions"
    private const val EVENT_CLASS = "Nativephp\\FaceDetection\\Events\\FaceDetectionCompleted"

    /**
     * Detect faces in an image
     * Parameters:
     *   - path: (preferred) string - File path to the image
     *   - image: (fallback) string - Base64 encoded image data
     *   - id: (optional) string - Optional ID to track this detection request
     * Returns:
     *   - (empty map - results are returned via FaceDetectionCompleted event)
     */
    class Detect(private val activity: FragmentActivity) : BridgeFunction {
        override fun execute(parameters: Map<String, Any>): Map<String, Any> {
            val id = parameters["id"] as? String
            val path = parameters["path"] as? String
            val base64Image = parameters["image"] as? String

            Log.d(TAG, "Detecting faces with path=$path, hasBase64=${base64Image != null}, id=$id")

            // Load bitmap from path (preferred) or base64 fallback
            val bitmap = when {
                path != null -> {
                    try {
                        BitmapFactory.decodeFile(path)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load image from path: ${e.message}")
                        null
                    }
                }
                base64Image != null -> {
                    try {
                        // Strip data URL prefix if present (e.g., "data:image/jpeg;base64,")
                        val cleanBase64 = if (base64Image.contains(";base64,")) {
                            base64Image.substringAfter(";base64,")
                        } else {
                            base64Image
                        }
                        val decoded = Base64.decode(cleanBase64, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to decode base64 image: ${e.message}")
                        null
                    }
                }
                else -> null
            }

            if (bitmap == null) {
                dispatchError(id, "Could not load image")
                return emptyMap()
            }

            // Create InputImage for ML Kit
            val inputImage = InputImage.fromBitmap(bitmap, 0)

            // Configure face detector with accurate mode and all classifications
            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()

            val detector = FaceDetection.getClient(options)

            // Process synchronously using Tasks.await()
            try {
                val faces = Tasks.await(detector.process(inputImage))

                val faceArray = JSONArray()
                for (face in faces) {
                    val faceObj = JSONObject()

                    // Bounding box
                    val bounds = JSONObject()
                    bounds.put("x", face.boundingBox.left)
                    bounds.put("y", face.boundingBox.top)
                    bounds.put("width", face.boundingBox.width())
                    bounds.put("height", face.boundingBox.height())
                    faceObj.put("bounds", bounds)

                    // Probabilities as whole percentages (99 not 0.99)
                    // ML Kit returns null for uncomputed values
                    faceObj.put("smilingProbability", formatProbability(face.smilingProbability))
                    faceObj.put("leftEyeOpenProbability", formatProbability(face.leftEyeOpenProbability))
                    faceObj.put("rightEyeOpenProbability", formatProbability(face.rightEyeOpenProbability))

                    // Euler angles (rounded to 1 decimal place)
                    faceObj.put("headEulerAngleY", Math.round(face.headEulerAngleY * 10) / 10.0)
                    faceObj.put("headEulerAngleZ", Math.round(face.headEulerAngleZ * 10) / 10.0)

                    faceArray.put(faceObj)
                }

                val payload = JSONObject()
                payload.put("success", true)
                payload.put("faces", faceArray)
                payload.put("count", faces.size)
                if (id != null) payload.put("id", id)

                Log.d(TAG, "Detected ${faces.size} faces")

                // Dispatch event on main thread
                Handler(Looper.getMainLooper()).post {
                    NativeActionCoordinator.dispatchEvent(activity, EVENT_CLASS, payload.toString())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Face detection failed: ${e.message}", e)
                dispatchError(id, e.message ?: "Face detection failed")
            }

            return emptyMap()
        }

        /**
         * Format probability as whole percentage (0-100) or null
         */
        private fun formatProbability(value: Float?): Any {
            if (value == null || value < 0) return JSONObject.NULL
            return Math.round(value * 100)
        }

        /**
         * Dispatch error event on main thread
         */
        private fun dispatchError(id: String?, message: String) {
            val payload = JSONObject()
            payload.put("success", false)
            payload.put("error", message)
            if (id != null) payload.put("id", id)

            Handler(Looper.getMainLooper()).post {
                NativeActionCoordinator.dispatchEvent(activity, EVENT_CLASS, payload.toString())
            }
        }
    }

    /**
     * Check if face detection is available on this device
     * Returns:
     *   - available: boolean - Whether ML Kit face detection is available
     */
    class IsAvailable(private val activity: FragmentActivity) : BridgeFunction {
        override fun execute(parameters: Map<String, Any>): Map<String, Any> {
            // ML Kit face detection is available on all Android devices with Google Play Services
            return BridgeResponse.success(mapOf("available" to true))
        }
    }
}
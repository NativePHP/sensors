# Face Detection Plugin - Android Implementation Guide

## Overview
This plugin uses ML Kit Face Detection. iOS is complete. Android needs implementation.

## Working iOS Implementation

### Key Files
- `resources/ios/Sources/FaceDetectionFunctions.swift` - Bridge functions
- `resources/js/face-detection.js` - JS library (shared iOS/Android)
- `nativephp.json` - Plugin manifest
- `src/Events/FaceDetectionCompleted.php` - Event dispatched with results

### How It Works
1. JS calls `faceDetection.detect({ path: '/path/to/image.jpg' })`
2. Native code loads image, runs ML Kit face detection
3. Native dispatches event `Nativephp\FaceDetection\Events\FaceDetectionCompleted` via `LaravelBridge`
4. Vue listens for event and displays results

### Critical Learnings from iOS
- **Use file path, not base64** - Pass `path` parameter for better EXIF orientation handling
- **Use synchronous `results(in:)` not async `process()`** - Async had issues
- **Dispatch event, don't return data** - Follow camera/video pattern with events
- **Format probabilities as whole percentages** - `Int(round(value * 100))` gives 99 not 0.99

## Android Implementation Needed

### File to Create
`resources/android/src/com/nativephp/plugin/face_detection/FaceDetectionFunctions.kt`

### Manifest Already Configured
```json
{
  "bridge_functions": [
    {
      "name": "FaceDetection.Detect",
      "android": "com.nativephp.plugin.face_detection.FaceDetectionFunctions.Detect"
    },
    {
      "name": "FaceDetection.IsAvailable",
      "android": "com.nativephp.plugin.face_detection.FaceDetectionFunctions.IsAvailable"
    }
  ],
  "dependencies": {
    "android": {
      "implementation": ["com.google.mlkit:face-detection:16.1.7"]
    }
  }
}
```

### Required Kotlin Implementation Pattern

```kotlin
package com.nativephp.plugin.face_detection

import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import org.json.JSONArray
import org.json.JSONObject

class FaceDetectionFunctions {

    class Detect : BridgeFunction {
        override fun execute(params: JSONObject, context: Context, activity: FragmentActivity): JSONObject {
            val id = params.optString("id", null)

            // Load image from path (preferred) or base64
            val path = params.optString("path", null)
            val base64 = params.optString("image", null)

            val bitmap = when {
                path != null -> BitmapFactory.decodeFile(path)
                base64 != null -> {
                    val decoded = Base64.decode(base64, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                }
                else -> null
            }

            if (bitmap == null) {
                return BridgeResponse.error("INVALID_IMAGE", "Could not load image")
            }

            val image = InputImage.fromBitmap(bitmap, 0)

            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()

            val detector = FaceDetection.getClient(options)

            // Use Tasks.await() for synchronous execution
            try {
                val faces = Tasks.await(detector.process(image))

                val faceArray = JSONArray()
                for (face in faces) {
                    val faceObj = JSONObject()

                    val bounds = JSONObject()
                    bounds.put("x", face.boundingBox.left)
                    bounds.put("y", face.boundingBox.top)
                    bounds.put("width", face.boundingBox.width())
                    bounds.put("height", face.boundingBox.height())
                    faceObj.put("bounds", bounds)

                    // Format as whole percentages, -1 means not computed
                    fun formatProb(value: Float?): Any {
                        if (value == null || value < 0) return JSONObject.NULL
                        return Math.round(value * 100)
                    }

                    faceObj.put("smilingProbability", formatProb(face.smilingProbability))
                    faceObj.put("leftEyeOpenProbability", formatProb(face.leftEyeOpenProbability))
                    faceObj.put("rightEyeOpenProbability", formatProb(face.rightEyeOpenProbability))
                    faceObj.put("headEulerAngleY", Math.round(face.headEulerAngleY * 10) / 10.0)
                    faceObj.put("headEulerAngleZ", Math.round(face.headEulerAngleZ * 10) / 10.0)

                    faceArray.put(faceObj)
                }

                val payload = JSONObject()
                payload.put("success", true)
                payload.put("faces", faceArray)
                payload.put("count", faces.size)
                if (id != null) payload.put("id", id)

                // Dispatch event on main thread
                Handler(Looper.getMainLooper()).post {
                    NativeActionCoordinator.dispatchEvent(
                        activity,
                        "Nativephp\\FaceDetection\\Events\\FaceDetectionCompleted",
                        payload.toString()
                    )
                }
            } catch (e: Exception) {
                val payload = JSONObject()
                payload.put("success", false)
                payload.put("error", e.message)
                if (id != null) payload.put("id", id)

                Handler(Looper.getMainLooper()).post {
                    NativeActionCoordinator.dispatchEvent(
                        activity,
                        "Nativephp\\FaceDetection\\Events\\FaceDetectionCompleted",
                        payload.toString()
                    )
                }
            }

            return JSONObject() // Return empty, results via event
        }
    }

    class IsAvailable : BridgeFunction {
        override fun execute(params: JSONObject, context: Context, activity: FragmentActivity): JSONObject {
            return BridgeResponse.success(JSONObject().put("available", true))
        }
    }
}
```

### Key Android Imports
```kotlin
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Base64
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import org.json.JSONArray
import org.json.JSONObject
```

### Event Payload Format (must match iOS)
```json
{
  "success": true,
  "count": 1,
  "faces": [
    {
      "bounds": { "x": 100, "y": 200, "width": 300, "height": 300 },
      "smilingProbability": 95,
      "leftEyeOpenProbability": 99,
      "rightEyeOpenProbability": 98,
      "headEulerAngleY": 5.2,
      "headEulerAngleZ": -2.1
    }
  ],
  "id": "optional-tracking-id"
}
```

### Testing
1. Build Android app
2. Use FaceDetection.vue page
3. Take photo, verify face detection results appear
4. Check percentages are whole numbers (99% not 99.29%)

### Reference Files
- Check existing bridge functions in NativePHP mobile package for patterns
- Look at `BridgeFunctionRegistry` for registration
- Look at `NativeActionCoordinator.dispatchEvent` for event dispatch

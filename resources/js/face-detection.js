/**
 * NativePHP Face Detection Plugin - JavaScript Library
 * Provides face detection capabilities using ML Kit
 *
 * @example
 * import { faceDetection } from '@nativephp/face-detection';
 *
 * // Check if available
 * const available = await faceDetection.isAvailable();
 *
 * // Detect faces in a base64 image
 * const result = await faceDetection.detect({ image: base64String });
 * console.log(result);
 * // {
 * //   count: 2,
 * //   faces: [
 * //     {
 * //       bounds: { x: 100, y: 50, width: 200, height: 200 },
 * //       smilingProbability: 0.95,
 * //       leftEyeOpenProbability: 0.99,
 * //       rightEyeOpenProbability: 0.98,
 * //       headEulerAngleY: 5.2,
 * //       headEulerAngleZ: -2.1
 * //     },
 * //     ...
 * //   ]
 * // }
 */

const baseUrl = '/_native/api/call';

/**
 * Internal bridge call function
 * @private
 */
async function bridgeCall(method, params = {}) {
    const response = await fetch(baseUrl, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN':
                document.querySelector('meta[name="csrf-token"]')?.content ||
                '',
        },
        body: JSON.stringify({ method, params }),
    });

    const result = await response.json();

    if (result.status === 'error') {
        throw new Error(result.message || 'Native call failed');
    }

    const nativeResponse = result.data;
    if (nativeResponse && nativeResponse.data !== undefined) {
        return nativeResponse.data;
    }

    return nativeResponse;
}

/**
 * Detect faces in an image
 * @param {Object} options - Detection options
 * @param {string} [options.image] - Base64 encoded image string
 * @param {string} [options.path] - Absolute file path to image (preferred, preserves EXIF)
 * @returns {Promise<{count: number, faces: Array<Face>}>}
 */
export async function detect(options = {}) {
    if (!options.image && !options.path) {
        throw new Error('Either image (base64) or path (file path) is required');
    }
    return bridgeCall('FaceDetection.Detect', options);
}

/**
 * Check if face detection is available on this device
 * @returns {Promise<{available: boolean}>}
 */
export async function isAvailable() {
    return bridgeCall('FaceDetection.IsAvailable');
}

/**
 * Helper: Convert a File/Blob to base64
 * @param {File|Blob} file - Image file to convert
 * @returns {Promise<string>} Base64 encoded string (without data URL prefix)
 */
export function fileToBase64(file) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => {
            const base64 = reader.result.split(',')[1];
            resolve(base64);
        };
        reader.onerror = reject;
        reader.readAsDataURL(file);
    });
}

/**
 * Helper: Convert an HTMLImageElement to base64
 * @param {HTMLImageElement} img - Image element to convert
 * @param {string} [format='image/jpeg'] - Output format
 * @param {number} [quality=0.9] - JPEG quality (0-1)
 * @returns {string} Base64 encoded string (without data URL prefix)
 */
export function imageToBase64(img, format = 'image/jpeg', quality = 0.9) {
    const canvas = document.createElement('canvas');
    canvas.width = img.naturalWidth;
    canvas.height = img.naturalHeight;
    const ctx = canvas.getContext('2d');
    ctx.drawImage(img, 0, 0);
    return canvas.toDataURL(format, quality).split(',')[1];
}

/**
 * Helper: Capture frame from video element as base64
 * @param {HTMLVideoElement} video - Video element to capture from
 * @param {string} [format='image/jpeg'] - Output format
 * @param {number} [quality=0.9] - JPEG quality (0-1)
 * @returns {string} Base64 encoded string (without data URL prefix)
 */
export function captureVideoFrame(video, format = 'image/jpeg', quality = 0.9) {
    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    const ctx = canvas.getContext('2d');
    ctx.drawImage(video, 0, 0);
    return canvas.toDataURL(format, quality).split(',')[1];
}

/**
 * Face Detection namespace object
 */
export const faceDetection = {
    detect,
    isAvailable,
    // Helpers
    fileToBase64,
    imageToBase64,
    captureVideoFrame,
};

export default faceDetection;

// Example
// From file input
// const input = document.querySelector('input[type="file"]');
// input.addEventListener('change', async (e) => {
//     const base64 = await faceDetection.fileToBase64(e.target.files[0]);
//     const result = await faceDetection.detect({ image: base64 });
//     console.log(`Found ${result.count} faces`);
// });
//
// // From camera stream
// const video = document.querySelector('video');
// const base64 = faceDetection.captureVideoFrame(video);
// const result = await faceDetection.detect({ image: base64 });
//
// // Real-time detection loop
// setInterval(async () => {
//     const base64 = faceDetection.captureVideoFrame(video);
//     const { faces } = await faceDetection.detect({ image: base64 });
//     faces.forEach((face) => {
//         if (face.smilingProbability > 0.8) {
//             console.log('Smile detected!');
//         }
//     });
// }, 500);

package com.example.sencsu.utils

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmenter
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import java.nio.ByteBuffer

class SegmentHelper {
    private val options = SelfieSegmenterOptions.Builder()
        .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
        .build()
    private val segmenter = Segmentation.getClient(options)

    fun processImage(image: Bitmap, onResult: (Bitmap?) -> Unit) {
        val inputImage = InputImage.fromBitmap(image, 0)
        segmenter.process(inputImage)
            .addOnSuccessListener { segmentationMask ->
                val result = generateMaskedImage(image, segmentationMask.buffer, segmentationMask.width, segmentationMask.height)
                onResult(result)
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    private fun generateMaskedImage(original: Bitmap, maskBuffer: ByteBuffer, width: Int, height: Int): Bitmap {
        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Redimensionner l'original pour qu'il corresponde exactement au masque si nécessaire
        val scaledBitmap = if (original.width != width || original.height != height) {
            Bitmap.createScaledBitmap(original, width, height, true)
        } else {
            original
        }

        val pixels = IntArray(width * height)
        scaledBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        maskBuffer.rewind()

        // Optimisation : boucle unique sur tous les pixels
        for (i in 0 until width * height) {
            val confidence = maskBuffer.float
            // Seuil de confiance (0.7f donne un contour plus net que 0.5f)
            if (confidence < 0.7f) {
                pixels[i] = Color.TRANSPARENT
            }
            // Si confidence > 0.7, on garde le pixel original intact (déjà dans le tableau)
        }

        resultBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return resultBitmap
    }
}
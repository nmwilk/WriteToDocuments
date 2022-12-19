package com.nmwilkinson.documentshack

import android.content.ContentUris
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.icu.text.SimpleDateFormat
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.TextView
import java.util.*

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val fillPaint = Paint()
    private val iso8601DateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private val random = Random(1000)

    init {
        fillPaint.style = Paint.Style.FILL
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.createDrawing).setOnClickListener(this)
    }

    override fun onClick(p0: View?) {
        val collection = MediaStore.Files.getContentUri("external")
        val relativePath = "Documents"

        val contentValues = ContentValues()

        val color = random.nextLong().and(0x00FFFFFF)
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "color_${Integer.toHexString(color.toInt())}.png")
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
        contentValues.put(MediaStore.MediaColumns.SIZE, 100000)
        contentValues.put(
            MediaStore.MediaColumns.DATE_MODIFIED,
            iso8601DateFormatter.format(Date())
        )
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)

        fillPaint.color = 0xFF000000.or(color).toInt()

        contentResolver.insert(collection, contentValues)?.let {
            contentResolver.openOutputStream(it).use { outputStream ->
                val bmp = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                canvas.drawRect(0f, 0f, 400f, 400f, fillPaint)
                bmp.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
        }

        val found = refresh()
        findViewById<TextView>(R.id.found).text = "Found ${found.size}"
    }

    private fun refresh(): MutableList<MediaStoreImage> {
        val images = mutableListOf<MediaStoreImage>()

            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.RELATIVE_PATH
            )

            val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"

            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->

                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val mimeTypeColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

                while (cursor.moveToNext()) {
                    if (!supportedMimeType(cursor.getString(mimeTypeColumn))) continue
                    val id = cursor.getLong(idColumn)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val image = MediaStoreImage(id, contentUri)
                    images += image
                }
        }

        return images
    }


    private val supportedMimeTypes = listOf(
        "image/jpeg",
        "image/png",
        "image/gif",
        "image/bmp",
        "image/tiff",
        "image/heic",
    )


    private fun supportedMimeType(mimeType: String?): Boolean {
        return supportedMimeTypes.contains(mimeType)
    }

}

data class MediaStoreImage(val id: Long, val contentUri: Uri)

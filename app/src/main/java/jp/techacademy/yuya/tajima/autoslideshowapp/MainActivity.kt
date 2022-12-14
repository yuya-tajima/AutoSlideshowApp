package jp.techacademy.yuya.tajima.autoslideshowapp

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import androidx.appcompat.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.provider.MediaStore
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private var mTimer: Timer? = null

    private var mHandler = Handler(Looper.getMainLooper())

    val imageUrlList = mutableListOf<Uri>()

    var currentImageIndex = 0

    private val PERMISSIONS_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()

        Log.d("DEBUG_PRINT", "onStart")

        disableAllOperation()

        if (!checkPermission()) {
            return
        }

        setUp()

        nextButton.setOnClickListener{
            Log.d("DEBUG_PRINT", "clicked next button")
            setNextImageIndex()
            setImageToView()
        }

        prevButton.setOnClickListener {
            Log.d("DEBUG_PRINT", "clicked prev button")
            setPrevImageIndex()
            setImageToView()
        }

        controlButton.setOnClickListener {
            Log.d("DEBUG_PRINT", "clicked control button")
            if (!isSlideShowActive()){
                startAutoSlideshowState()
            } else {
                stopAutoSlideshowState()
            }
        }

        Log.d("DEBUG_PRINT", "size of ImageUrlList : " + imageUrlList.size)
    }

    private fun isSlideShowActive (): Boolean {
        return mTimer != null
    }

    override fun onPause() {
        super.onPause()
        stopAutoSlideshowState()
    }

    private fun startAutoSlideshowState() {
        disableManualOperation()
        startAutoSlideshow()
        controlButton.text = "??????"
    }

    private fun stopAutoSlideshowState() {
        enableManualOperation()
        stopAutoSlideshow()
        controlButton.text = "??????"
    }

    private fun checkPermission(): Boolean {

        // Android 6.0???????????????
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // ???????????????????????????????????????????????????
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                // ?????????????????????
                return true
            } else {
                // ??????????????????????????????????????????????????????????????????
                requestPermissions(
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSIONS_REQUEST_CODE
                )
            }
            // Android 5??????????????????
        } else {
            return true
        }

        return false
    }

    private fun setUp () {
        getContentsInfo()
        if (imageExists()) {
            currentImageIndex = 0
            enableAllOperation()
            setImageToView()
        } else {
            showAlertDialog("???????????????????????????", "???????????????????????????Photos?????????????????????????????????")
        }
    }

    private fun setNextImageIndex () {
        currentImageIndex += 1
        if ( currentImageIndex > imageUrlList.size - 1 ) {
            currentImageIndex = 0
        }
    }

    private fun setPrevImageIndex () {
        currentImageIndex -= 1
        if ( currentImageIndex < 0 ) {
            currentImageIndex = imageUrlList.size - 1
        }
    }

    private fun imageExists(): Boolean  {
        return imageUrlList.size != 0
    }

    private fun setImageToView () {
        Log.d("DEBUG_PRINT", "URI : " + imageUrlList[currentImageIndex].toString())
        imageView.setImageURI(imageUrlList[currentImageIndex])
    }

    private fun startAutoSlideshow () {
        mTimer = Timer()
        mTimer?.schedule(object : TimerTask() {
            override fun run() {
                mHandler.post {
                    setNextImageIndex()
                    setImageToView()
                }
            }
        }, 2000, 2000)
    }

    private fun stopAutoSlideshow() {
        mTimer?.cancel()
        mTimer = null
    }

    private fun disableManualOperation () {
        prevButton.isEnabled = false
        prevButton.isClickable = false
        nextButton.isEnabled = false
        nextButton.isClickable = false
    }

    private fun enableManualOperation () {
        prevButton.isEnabled = true
        prevButton.isClickable = true
        nextButton.isEnabled = true
        nextButton.isClickable = true
    }

    private fun disableAllOperation () {
        disableManualOperation()
        controlButton.isEnabled = false
        controlButton.isClickable =false
    }

    private fun enableAllOperation () {
        enableManualOperation()
        controlButton.isEnabled = true
        controlButton.isClickable = true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE ->
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setUp()
                } else {
                    showAlertDialog("????????????????????????????????????", "???????????????????????????????????????????????????????????????????????????????????????")
                }
        }
    }

    private fun getContentsInfo() {
        // ??????????????????????????????
        val resolver = contentResolver
        val cursor = resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, // ??????????????????
            null, // ?????????null = ????????????
            null, // ?????????????????????null = ?????????????????????
            null, // ??????????????????????????????
            null // ????????? (null??????????????????
        )

        if (cursor != null && cursor.moveToFirst()) {
            imageUrlList.clear()
            do {
                // index??????ID?????????????????????ID???????????????URI???????????????
                val fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                val id = cursor.getLong(fieldIndex)
                val imageUri: Uri =
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                Log.d("DEBUG_PRINT", "URI : " + imageUri.toString())

                imageUrlList += imageUri

            } while (cursor.moveToNext())
            cursor.close()
        }
    }

    private fun showAlertDialog(title: String, message: String) {
        AlertDialog.Builder(this) // Fragment??????Activity?????????????????????
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                Log.d("DEBUG_PRINT", "Closed Alert dialog")
            }
            .show()
    }
}
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
        controlButton.text = "停止"
    }

    private fun stopAutoSlideshowState() {
        enableManualOperation()
        stopAutoSlideshow()
        controlButton.text = "再生"
    }

    private fun checkPermission(): Boolean {

        // Android 6.0以降の場合
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // パーミッションの許可状態を確認する
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                // 許可されている
                return true
            } else {
                // 許可されていないので許可ダイアログを表示する
                requestPermissions(
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSIONS_REQUEST_CODE
                )
            }
            // Android 5系以下の場合
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
            showAlertDialog("画像が存在しません", "画像を表示するにはPhotosに画像を保存して下さい")
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
                    showAlertDialog("権限が許可されていません", "ご利用になる場合は、アプリの設定で権限の許可をお願いします")
                }
        }
    }

    private fun getContentsInfo() {
        // 画像の情報を取得する
        val resolver = contentResolver
        val cursor = resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, // データの種類
            null, // 項目（null = 全項目）
            null, // フィルタ条件（null = フィルタなし）
            null, // フィルタ用パラメータ
            null // ソート (nullソートなし）
        )

        if (cursor != null && cursor.moveToFirst()) {
            imageUrlList.clear()
            do {
                // indexからIDを取得し、そのIDから画像のURIを取得する
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
        AlertDialog.Builder(this) // FragmentではActivityを取得して生成
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                Log.d("DEBUG_PRINT", "Closed Alert dialog")
            }
            .show()
    }
}
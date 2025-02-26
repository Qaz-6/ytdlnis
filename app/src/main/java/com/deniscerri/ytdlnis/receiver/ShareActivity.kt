package com.deniscerri.ytdlnis.receiver

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.deniscerri.ytdlnis.MainActivity
import com.deniscerri.ytdlnis.R
import com.deniscerri.ytdlnis.database.models.DownloadItem
import com.deniscerri.ytdlnis.database.models.ResultItem
import com.deniscerri.ytdlnis.database.viewmodel.CookieViewModel
import com.deniscerri.ytdlnis.database.viewmodel.DownloadViewModel
import com.deniscerri.ytdlnis.database.viewmodel.ResultViewModel
import com.deniscerri.ytdlnis.ui.BaseActivity
import com.deniscerri.ytdlnis.ui.downloadcard.DownloadBottomSheetDialog
import com.deniscerri.ytdlnis.ui.downloadcard.SelectPlaylistItemsDialog
import com.deniscerri.ytdlnis.util.ThemeUtil
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.properties.Delegates
import kotlin.system.exitProcess

class ShareActivity : BaseActivity() {

    lateinit var context: Context
    private lateinit var resultViewModel: ResultViewModel
    private lateinit var downloadViewModel: DownloadViewModel
    private lateinit var cookieViewModel: CookieViewModel
    private lateinit var sharedPreferences: SharedPreferences
    private var quickDownload by Delegates.notNull<Boolean>()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.updateTheme(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
            v.setPadding(0, 0, 0, 0)
            insets
        }
        window.run {
            setBackgroundDrawable(ColorDrawable(0))
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            } else {
                setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
            }
        }

        setContentView(R.layout.activity_share)
        context = baseContext
        resultViewModel = ViewModelProvider(this)[ResultViewModel::class.java]
        downloadViewModel = ViewModelProvider(this)[DownloadViewModel::class.java]
        cookieViewModel = ViewModelProvider(this)[CookieViewModel::class.java]
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        cookieViewModel.updateCookiesFile()
        val intent = intent
        handleIntents(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntents(intent)
    }

    private fun handleIntents(intent: Intent) {
        askPermissions()

        val action = intent.action
        Log.e("aa", intent.toString())
        if (Intent.ACTION_SEND == action || Intent.ACTION_VIEW == action) {
            if (intent.getStringExtra(Intent.EXTRA_TEXT) == null && Intent.ACTION_SEND == action){
                intent.setClass(this, MainActivity::class.java)
                startActivity(intent)
                finishAffinity()
                return
            }

            runCatching { supportFragmentManager.popBackStack() }

            quickDownload = intent.getBooleanExtra("quick_download", sharedPreferences.getBoolean("quick_download", false) || sharedPreferences.getString("preferred_download_type", "video") == "command")

            val loadingBottomSheet = BottomSheetDialog(this)
            loadingBottomSheet.requestWindowFeature(Window.FEATURE_NO_TITLE)
            loadingBottomSheet.setContentView(R.layout.please_wait_bottom_sheet)
            loadingBottomSheet.show()
            val cancel = loadingBottomSheet.findViewById<TextView>(R.id.cancel)
            cancel!!.setOnClickListener {
                this.finish()
            }

            val inputQuery = when(action){
                Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)!!
                else -> intent.dataString!!
            }

            lifecycleScope.launch {
                try {
                    val result = resultViewModel.getItemByURL(inputQuery)
                    loadingBottomSheet.dismiss()
                    showDownloadSheet(result)
                }catch (e: Exception){
                    resultViewModel.deleteAll()
                    if (quickDownload && inputQuery.matches("(https?://(?:www\\.|(?!www))[a-zA-Z\\d][a-zA-Z\\d-]+[a-zA-Z\\d]\\.\\S{2,}|www\\.[a-zA-Z\\d][a-zA-Z\\d-]+[a-zA-Z\\d]\\.\\S{2,}|https?://(?:www\\.|(?!www))[a-zA-Z\\d]+\\.\\S{2,}|www\\.[a-zA-Z\\d]+\\.\\S{2,})".toRegex())){
                        val result = downloadViewModel.createEmptyResultItem(inputQuery)
                        loadingBottomSheet.dismiss()
                        showDownloadSheet(result)
                    }else{
                        val res = withContext(Dispatchers.IO){
                            resultViewModel.parseQuery(inputQuery, true)
                        }
                        if (res.isEmpty()) {
                            Toast.makeText(this@ShareActivity, "No Results Found!", Toast.LENGTH_SHORT).show()
                            exit()
                        }else{
                            loadingBottomSheet.dismiss()
                            if (res.size == 1){
                                showDownloadSheet(res[0]!!)
                            }else{
                                showSelectPlaylistItems(res.toList())
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showDownloadSheet(it: ResultItem){
        if (sharedPreferences.getBoolean("download_card", true)){
            val bottomSheet = DownloadBottomSheetDialog(it, DownloadViewModel.Type.valueOf(sharedPreferences.getString("preferred_download_type", "video")!!), null, quickDownload)
            bottomSheet.show(supportFragmentManager, "downloadSingleSheet")
        }else{
            lifecycleScope.launch(Dispatchers.IO){
                val downloadItem = downloadViewModel.createDownloadItemFromResult(it, DownloadViewModel.Type.valueOf(sharedPreferences.getString("preferred_download_type", "video")!!))
                downloadViewModel.queueDownloads(listOf(downloadItem))
            }
            this.finish()
        }
    }

    private fun showSelectPlaylistItems(it: List<ResultItem?>){
        if (sharedPreferences.getBoolean("download_card", true)){
            val bottomSheet = SelectPlaylistItemsDialog(it, DownloadViewModel.Type.valueOf(sharedPreferences.getString("preferred_download_type", "video")!!))
            bottomSheet.show(supportFragmentManager, "downloadPlaylistSheet")
        }else{
            lifecycleScope.launch(Dispatchers.IO){
                val downloadItems = mutableListOf<DownloadItem>()
                lifecycleScope.launch(Dispatchers.IO){
                    it.forEach { res ->
                        val i = downloadViewModel.createDownloadItemFromResult(res!!, DownloadViewModel.Type.valueOf(sharedPreferences.getString("preferred_download_type", "video")!!))
                        i.format = downloadViewModel.getLatestCommandTemplateAsFormat()
                        downloadItems.add(i)
                    }
                    downloadViewModel.queueDownloads(downloadItems)
                }
            }
            this.finish()
        }
    }

    private fun askPermissions() {
        val permissions = arrayListOf<String>()
        if (!checkFilePermission()) {
            if (Build.VERSION.SDK_INT >= 33){
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
                permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            }else{
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        if (!checkNotificationPermission()){
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissions.isNotEmpty()){
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                1
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        for (i in permissions.indices) {
            if (permissions.contains(Manifest.permission.POST_NOTIFICATIONS)) break
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                createPermissionRequestDialog()
            }
        }
    }

    private fun exit() {
        finishAffinity()
        exitProcess(0)
    }

    private fun checkFilePermission(): Boolean {
        return (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED)
    }

    private fun checkNotificationPermission(): Boolean {
        return (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED)
    }

    private fun createPermissionRequestDialog() {
        val dialog = MaterialAlertDialogBuilder(this)
        dialog.setTitle(getString(R.string.warning))
        dialog.setMessage(getString(R.string.request_permission_desc))
        dialog.setOnCancelListener { exit() }
        dialog.setNegativeButton(getString(R.string.exit_app)) { _: DialogInterface?, _: Int -> exit() }
        dialog.setPositiveButton(getString(R.string.ok)) { _: DialogInterface?, _: Int ->
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null)
            )
            startActivity(intent)
            exitProcess(0)
        }
        dialog.show()
    }
    override fun onConfigurationChanged(newConfig: Configuration) {
        startActivity(Intent(this, MainActivity::class.java))
        super.onConfigurationChanged(newConfig)
    }
}
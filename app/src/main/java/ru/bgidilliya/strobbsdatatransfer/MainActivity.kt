package ru.bgidilliya.strobbsdatatransfer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import jcifs.smb.NtlmPasswordAuthentication
import jcifs.smb.SmbFile
import jcifs.smb.SmbFileOutputStream
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1
    private var files = arrayOf<File>()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            } else {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE)
            }
        } else {
            initActivity()
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE -> {

                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    initActivity()
                }
            }
            else -> {
            }
        }
    }

    private fun initActivity() {

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val folderPath = prefs.getString("source_folder", "")

        setTransferringState(0)

        if (folderPath!!.isEmpty()) {
            val settingsIntent = Intent(this, SettingsActivity::class.java)
            startActivity(settingsIntent)
            return
        }

        val folder = File(folderPath)
        if (!folder.isDirectory) {
            val builder = AlertDialog.Builder(this)
            builder//.setTitle("Ошибка")
                .setMessage("Папки '$folderPath' не существует")
                .setPositiveButton("ОК") {
                        dialog, _ ->  dialog.cancel()
                }
            builder.show()
            return
        }

        files = folder.listFiles()
        if (files.isEmpty()) {
            Toast.makeText(this, "В папке '$folderPath' нет файлов", Toast.LENGTH_LONG).show()
            return
        }

        setTransferringState(1)

    }

    fun transferData(view: View) {

        setTransferringState(2)

    }

    fun declineTransferring(view: View) {

        setTransferringState(1)

    }

    fun acceptTransferring(view: View) {

        textViewTransfering.visibility = View.VISIBLE
        setTransferringState(0)
        NetworkTask().execute(this)

    }

    fun openSettings(view: View) {

        val settingsIntent = Intent(this, SettingsActivity::class.java)
        startActivity(settingsIntent)

    }

    private fun setTransferringState(state: Int) {

        buttonTransfer.isEnabled = (state == 1)
        val res = if (state == 1) R.drawable.green_circle_button else R.drawable.grey_circle_button
        buttonTransfer.background = ActivityCompat.getDrawable(this, res)

        val visibility = if (state == 2) View.VISIBLE else View.INVISIBLE
        textViewAccept.visibility = visibility
        buttonNo.visibility = visibility
        buttonYes.visibility = visibility

    }

    internal class NetworkTask : AsyncTask<MainActivity, Void, String>() {

        private var context: MainActivity? = null

        override fun doInBackground(vararg activity: MainActivity): String? {
            try {

                context = activity[0]
                val prefs = PreferenceManager.getDefaultSharedPreferences(context)

                var auth = NtlmPasswordAuthentication(null, null, null)
                if (!prefs.getBoolean("anonymous", false)) {
                    auth = NtlmPasswordAuthentication(
                        prefs.getString("domain", ""),
                        prefs.getString("username", ""),
                        prefs.getString("password", ""))
                }

                val dateNow = Date()
                val formatForDateNow = SimpleDateFormat("_dd.MM.yyyy_HH.mm.ss")

                var destPath: String = "smb://" + prefs.getString("destination_folder", "")

                /*
                destPath += File.separator + prefs.getString("terminal_number", "")
                destPath += formatForDateNow.format(dateNow)

                val smbDir = SmbFile(destPath, auth)
                smbDir.mkdir()
                 */

                for (srcFile in context!!.files) {

                    val destFile = SmbFile(destPath + File.separator + srcFile.name, auth)
                    val srcFileStream: InputStream = FileInputStream(srcFile)
                    val destFileStream = SmbFileOutputStream(destFile)

                    val bytes = ByteArray(16 * 1024)
                    var read: Int
                    while (srcFileStream.read(bytes, 0, bytes.size).also { read = it } > 0) {
                        destFileStream.write(bytes, 0, read)
                    }

                    destFileStream.flush()
                    srcFile.delete()
                }
            } catch (exc: Exception) {
                return exc.message
            }
            return ""
        }

        override fun onPostExecute(error: String) {

           context?.textViewTransfering?.visibility = View.INVISIBLE

           if (error == "") {
               Toast.makeText(context, "ПЕРЕДАНО УСПЕШНО", Toast.LENGTH_LONG).show()
               return
           }

           context?.setTransferringState(2)
           val builder = context?.let { AlertDialog.Builder(it) }
           if (builder != null) {
               builder//.setTitle("Ошибка")
                   .setMessage(error)
                   .setPositiveButton("ОК") { dialog, _ ->  dialog.cancel()
                   }
               builder.show()
           }
        }
    }

}


package com.s2labs.shreemagnetfota

import android.app.ProgressDialog
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

open class BaseActivity : AppCompatActivity() {
	private val progress by lazy {
		ProgressDialog(this).apply {
			setCancelable(false)
		}
	}

	fun showProgress(msg: String = "") {
		progress.setMessage(msg)
		progress.show()
	}

	suspend fun showProgressAsync(msg: String = "") {
		withContext(Dispatchers.Main) {
			showProgress(msg)
		}
	}

	fun dismissProgress() {
		progress.dismiss()
	}

	fun showToast(msg: String) {
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
	}

	suspend fun showToastAsync(msg: String) {
		withContext(Dispatchers.Main) {
			showToast(msg)
		}
	}

	fun showConnectionError(close: Boolean = false) {
		AlertDialog.Builder(this)
			.setMessage("Connection error, please check your internet connection")
			.setTitle("Connection Error")
			.setPositiveButton("Close") { _, _ ->
				if (close) {
					finish()
				}
			}
	}
}
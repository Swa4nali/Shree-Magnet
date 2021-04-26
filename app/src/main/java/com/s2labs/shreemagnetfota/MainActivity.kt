package com.s2labs.shreemagnetfota

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.s2labs.shreemagnetfota.adapter.DeviceListAdapter
import com.s2labs.shreemagnetfota.api.ApiService
import com.s2labs.shreemagnetfota.databinding.ActivityMainBinding
import com.s2labs.shreemagnetfota.util.Constant
import com.s2labs.shreemagnetfota.viewmodel.DeviceListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.util.*
import kotlin.experimental.xor
import kotlin.math.roundToInt


@Suppress("BlockingMethodInNonBlockingContext")
class MainActivity : BaseActivity() {

	private lateinit var binding: ActivityMainBinding
	private val deviceListAdapter by lazy {
		DeviceListAdapter()
	}
	private val mBluetoothAdapter by lazy {
		BluetoothAdapter.getDefaultAdapter()
	}
	private var socket: BluetoothSocket? = null
	private lateinit var selectedDevice: DeviceListAdapter.DeviceItem
	private val viewModel by lazy {
		ViewModelProvider(this).get(DeviceListViewModel::class.java)
	}
	private val selectedVersion by lazy {
		intent.getIntExtra("version", 0)
	}
	private val otaFile by lazy {
		intent.getStringExtra("file")
	}
	private val apiService by lazy { ApiService.getInstance() }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

		if (mBluetoothAdapter.isEnabled) {
			listPaired()
		} else {
			mBluetoothAdapter.enable()
			lifecycleScope.launchWhenCreated {
				delay(1000)
				listPaired()
			}
		}

		binding.deviceListRV.adapter = deviceListAdapter
		deviceListAdapter.clickListener = object : DeviceListAdapter.OnClickListener {
			override fun onClick(item: DeviceListAdapter.DeviceItem, position: Int) {
				selectedDevice = item
				lifecycleScope.launch {
					if (!mBluetoothAdapter.isEnabled) {
						mBluetoothAdapter.enable()
						delay(1000)
					}
					showProgress("Connecting.....")
					withContext(Dispatchers.IO) {
						try {
							connect(0)
							withContext(Dispatchers.Main) {
								selectFile()
							}
						} catch (e: Exception) {
							e.printStackTrace()
							showToastAsync("Connection failed, please try again")
						}
						dismissProgress()
					}
				}
			}
		}
		binding.viewModel = viewModel
		viewModel.search.observe(this) {
			listPaired(it)
		}
	}

	private fun listPaired(search: String? = viewModel.search.value) {
		val pairedDevices = mBluetoothAdapter.bondedDevices
		deviceListAdapter.submitList(pairedDevices.filter {
			if (search.isNullOrEmpty()) {
				true
			} else {
				it.name.contains(search, true)
			}
		}.map {
			DeviceListAdapter.DeviceItem(it.name ?: it.address, it.address)
		})
	}

	private suspend fun connect(delay: Long = 500): Boolean {
		val status = socket?.isConnected ?: false
		if (!status) {
			if (delay > 0) {
				delay(delay)
			}
			val device = mBluetoothAdapter.getRemoteDevice(selectedDevice.mac)
			socket = device.createRfcommSocketToServiceRecord(UUID.fromString(Constant.BLUETOOTH_UUID))
			socket?.connect()

			os = socket?.outputStream?.bufferedWriter(Charsets.US_ASCII)
			ipS = socket?.inputStream?.bufferedReader(Charsets.US_ASCII)
		}

		return status
	}

	private var os: BufferedWriter? = null
	private var ipS: BufferedReader? = null

	private suspend fun checkAndReconnect() {
		/*if (!connect()) {
			os = socket?.outputStream?.bufferedWriter(Charsets.US_ASCII)
			ipS = socket?.inputStream?.bufferedReader(Charsets.US_ASCII)
		}*/
		connect()
		if (os == null || ipS == null) {
			throw Exception("CONNECTION_FAILED")
		}
	}

	private suspend fun getVersion(): Int {
		checkAndReconnect()

		os?.write("V\n")
		os?.flush()

		return ipS!!.readLine().trim().toIntOrNull() ?: -1
	}

	private suspend fun sendOTAFile(data: List<String>) {
		checkAndReconnect()

		os?.write("S\n")
		os?.flush()
		delay(1000)

		// wait for ACK first
		checkAndReconnect()
		ipS?.read()

		val length = data.size.toDouble()
		for ((i, line) in data.withIndex()) {
			var ack = false
			// keep sending current line if ACK(P) not received
			while (!ack) {
				checkAndReconnect()
				os?.write(line + "\n")
				os?.flush()
				showProgressAsync("Pushing OTA file.... ${((i + 1) / length * 100).roundToInt()}%")
				checkAndReconnect()
				val ip = ipS?.read()
				if (ip == 'P'.toInt()) {
					ack = true
				}
			}
		}
		socket?.close()
	}

	private fun selectFile() {
//		val intent = Intent(Intent.ACTION_GET_CONTENT)
//		intent.type = "*/*"
//		intent.addCategory(Intent.CATEGORY_OPENABLE)
//
//		try {
//			startActivityForResult(Intent.createChooser(intent, "Select OTA File"), FILE_PICKER)
//		} catch (ex: ActivityNotFoundException) {
//			Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show()
//		}
		showProgress("Loading OTA file")
		lifecycleScope.launch(Dispatchers.IO) {
			try {
				val resp = apiService.downloadOtaFile(otaFile ?: "")
				val stringResp = String(resp.bytes(), Charsets.US_ASCII)
				val lines = stringResp.lines()

				try {
					val version = getVersion()
					if (version > selectedVersion) {
						// downgrading
						withContext(Dispatchers.Main) {
							AlertDialog.Builder(this@MainActivity)
								.setMessage("Device is running firmware with version $version and you have selected firmware with version $selectedVersion?\n\nAre you sure to downgrade?").setPositiveButton("Yes") { _, _ ->
									lifecycleScope.launch(Dispatchers.IO) {
										pushOTA(lines)
										finish()
									}
								}.setNegativeButton("No"){_, _ ->
									showToast("OTA cancelled")
									finish()
								}
								.create().show()
						}
					} else {
						pushOTA(lines)
						finish()
					}
				} catch (e: Exception) {
					e.printStackTrace()
					showToastAsync("OTA failed, please try again")
					socket?.close()
					finish()
				}
			} catch (e: Exception) {
				e.printStackTrace()
				dismissProgress()
				showToastAsync("Failed to load OTA file")
				socket?.close()
				finish()
			}
		}
	}

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.device_list_menu, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.refresh -> listPaired()
		}
		return true
	}

	/*private fun bitInverse(line: String): String {
		val bytes = line.toByteArray(Charsets.US_ASCII)
		val newArr = ByteArray(bytes.size)
		val reversalByte = (-1).toByte()
		for ((index, byte) in bytes.withIndex()) {
			newArr[index] = byte xor reversalByte
		}
		return newArr.toString(Charsets.US_ASCII)
	}*/

	/*override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		if (requestCode == FILE_PICKER && resultCode == RESULT_OK) {
			val uri = data?.data
			if (uri != null) {
				val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
				contentResolver.query(uri, projection, null, null, null)?.use { metaCursor ->
					if (metaCursor.moveToFirst()) {
						val path = metaCursor.getString(0)
						val parts = path.split(".")
						if (parts.last() == "hex") {
							lifecycleScope.launch(Dispatchers.IO) {
								var lines: List<String>? = null
								contentResolver.openInputStream(uri)?.bufferedReader(Charsets.US_ASCII).use {
									if (it != null) {
										lines = it.readLines()
									} else {
										showToastAsync("Failed to load file")
										socket?.close()
									}
								}
								if (lines != null) {

								}
							}
						} else {
							showToast("Invalid update file")
							socket?.close()
						}
					}
				}
			} else {
				showToast("Failed to load file")
				socket?.close()
			}
		}
	}*/

	private suspend fun pushOTA(lines: List<String>) {
		showProgressAsync("Pushing OTA file....")
		try {
			sendOTAFile(lines)
			showToastAsync("OTA completed successfully")
		} catch (e: Exception) {
			e.printStackTrace()
			showToastAsync("OTA failed, please try again")
		}
		socket?.close()
		dismissProgress()
	}

	/*companion object {
		const val FILE_PICKER = 10001
	}*/
}
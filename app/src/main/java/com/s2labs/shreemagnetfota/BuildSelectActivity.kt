package com.s2labs.shreemagnetfota

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.s2labs.shreemagnetfota.api.ApiService
import com.s2labs.shreemagnetfota.api.BuildModel
import com.s2labs.shreemagnetfota.api.VersionModel
import com.s2labs.shreemagnetfota.databinding.ActivityBuildSelectBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BuildSelectActivity : BaseActivity() {

	private lateinit var binding: ActivityBuildSelectBinding

	private lateinit var buildAdapter: ArrayAdapter<String>
	private lateinit var versionAdapter: ArrayAdapter<String>

	private val apiService by lazy { ApiService.getInstance() }

	private var buildList: List<BuildModel>? = null
	private var versionList: List<VersionModel>? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = DataBindingUtil.setContentView(this, R.layout.activity_build_select)

		buildAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
		buildAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
		binding.selectBuildSp.adapter = buildAdapter

		versionAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
		versionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
		binding.selectVersionSp.adapter = versionAdapter

		buildAdapter.add("Loading")
		versionAdapter.add("No Data Available")

		lifecycleScope.launch(Dispatchers.IO) {
			try {
				buildList = apiService.buildList()

				withContext(Dispatchers.Main) {
					buildAdapter.clear()
					if (buildList.isNullOrEmpty()) {
						buildAdapter.add("No Data Available")
					} else {
						buildAdapter.add("Select")
						buildList?.forEach {
							buildAdapter.add(it.name)
						}
						binding.selectBuildSp.setSelection(0)
					}
				}
			} catch (e: Exception) {
				e.printStackTrace()
				withContext(Dispatchers.Main) {
					showConnectionError(true)
				}
			}
		}

		binding.selectBuildSp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
			override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
				versionAdapter.clear()
				if (position > 0) {
					versionAdapter.add("Loading")
					lifecycleScope.launch(Dispatchers.IO) {
						lifecycleScope.launch(Dispatchers.IO) {
							versionList = apiService.versionList(buildList!![position - 1].id)

							withContext(Dispatchers.Main) {
								versionAdapter.clear()
								if (versionList.isNullOrEmpty()) {
									versionAdapter.add("No Data Available")
								} else {
									versionAdapter.add("Select")
									versionList?.forEach {
										versionAdapter.add(it.version.toString())
									}
									binding.selectVersionSp.setSelection(0)
								}
							}
						}
					}
				} else {
					versionAdapter.add("No Data Available")
				}
			}

			override fun onNothingSelected(parent: AdapterView<*>?) {
			}

		}

		binding.selectVersionSp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
			override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
				binding.nextBtn.isEnabled = position > 0
			}

			override fun onNothingSelected(parent: AdapterView<*>?) {
			}
		}
	}

	fun next(v: View) {
		val pos = binding.selectVersionSp.selectedItemPosition - 1
		if (pos >= 0) {
			if (versionList!![pos].filePath.isNullOrEmpty()) {
				showToast("No OTA file found.")
			} else {
				startActivity(Intent(this, MainActivity::class.java).apply {
					putExtra("version", versionList!![pos].version)
					putExtra("file", versionList!![pos].filePath)
				})
			}
		}
	}
}
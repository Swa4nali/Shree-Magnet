package com.s2labs.shreemagnetfota.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DeviceListViewModel: ViewModel() {
	val search = MutableLiveData<String>()
}
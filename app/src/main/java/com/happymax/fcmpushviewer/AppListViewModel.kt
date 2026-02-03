package com.happymax.fcmpushviewer

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppListViewModel(application: Application) : AndroidViewModel(application) {
    private val context = getApplication<Application>().applicationContext
    // 使用 Flow 存储列表状态
    private val _appList = MutableStateFlow<List<AppInfo>>(emptyList())
    val appList: StateFlow<List<AppInfo>> = _appList

    // 加载状态（可选）
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // 模拟网络耗时或数据库查询
                val data = withContext(Dispatchers.IO) {
                    getAppList()
                }
                _appList.value = data
            } catch (e: Exception) {
                // 处理错误逻辑
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun getAppList(): ArrayList<AppInfo>{
        val appList:ArrayList<AppInfo> = ArrayList<AppInfo>()
        val packageManager = context.packageManager
        for (packageInfo in packageManager.getInstalledPackages(PackageManager.GET_RECEIVERS)) {

            if (packageInfo.receivers != null) {
                var supportFCM = false
                for (receiverInfo in packageInfo.receivers) {
                    if ( packageInfo.applicationInfo != null && receiverInfo.name == "com.google.firebase.iid.FirebaseInstanceIdReceiver" || receiverInfo.name == "com.google.android.gms.measurement.AppMeasurementReceiver") {
                        supportFCM = true
                        break
                    }
                }

                val appName = packageInfo.applicationInfo!!.loadLabel(packageManager).toString()
                val packageName = packageInfo.packageName
                var icon:Drawable? = packageInfo.applicationInfo!!.loadIcon(packageManager);
                val isSystemApp = (packageInfo.applicationInfo!!.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val appInfo = AppInfo(appName, packageName, if (icon!=null) drawableToBitmap(icon) else null, isSystemApp, supportFCM)
                appList.add(appInfo)
            }

        }
        return  appList
    }

}
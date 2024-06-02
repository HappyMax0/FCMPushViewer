package com.happymax.fcmpushviewer

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuItemCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {
    private lateinit var mSearchView: SearchView
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var sharedPreferences: SharedPreferences
    private val appList = ArrayList<AppInfo>()
    private var hideSystemApp:Boolean = false
        get() = field
        set(value){
            getAppList(value)
            val editor = sharedPreferences.edit()
            editor.putBoolean("HideSystemApp", value)
            editor.apply()
            field = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val apiAvailability: GoogleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode: Int = apiAvailability.isGooglePlayServicesAvailable(this)
        if(resultCode != ConnectionResult.SUCCESS){
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setTitle(R.string.dialog_title_error)
                .setMessage(R.string.dialog_msg_missing_gms)
                .setPositiveButton(R.string.dialog_btn_ok) { dialog, which ->
                    // OK button clicked
                    this.finish()
                }

            val dialog = builder.create()
            dialog.show()
        }

        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)
        val toolbar: Toolbar = findViewById(R.id.toolBar)
        setSupportActionBar(toolbar)

        recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        hideSystemApp = sharedPreferences.getBoolean("HideSystemApp", false)

        swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefresh.setColorSchemeResources(R.color.purple_500)
        swipeRefresh.setOnRefreshListener {
            getAppList(hideSystemApp)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_main_toolbar, menu)
        val item = menu?.findItem(R.id.search)
        if(item != null){
            mSearchView = MenuItemCompat.getActionView(item) as SearchView
            mSearchView.isIconified = true
            mSearchView.isSubmitButtonEnabled = false
            mSearchView.setOnSearchClickListener(object : View.OnClickListener{
                override fun onClick(view: View?) {
                    supportActionBar?.setDisplayHomeAsUpEnabled(true)//添加默认的返回图标
                    supportActionBar?.setHomeButtonEnabled(true)//设置返回键可用
                }
            })

            mSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
                override fun onQueryTextSubmit(query: String?): Boolean {
                    //提交文本时调用
                    if(!query.isNullOrEmpty())
                    {
                        val querySequence = query
                        val filteredList = appList.filter { it.appName.contains(querySequence, true) || it.packageName.contains(querySequence, true) }
                        recyclerView.adapter = AppInfoListAdapter(filteredList)
                        return true
                    }
                    else
                        return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    //文本搜索框发生变化时调用
                    if(!newText.isNullOrEmpty()){
                        val querySequence = newText
                        val filteredList = appList.filter { it.appName.contains(querySequence, true) || it.packageName.contains(querySequence, true) }
                        recyclerView.adapter = AppInfoListAdapter(filteredList)
                        return true
                    }
                    else{
                        recyclerView.adapter = AppInfoListAdapter(appList)
                        return true
                    }
                    return false
                }
            })
            mSearchView.setOnCloseListener(object : SearchView.OnCloseListener{
                override fun onClose(): Boolean {
                    recyclerView.adapter = AppInfoListAdapter(appList)
                    mSearchView.clearFocus()
                    mSearchView.onActionViewCollapsed()
                    supportActionBar?.setDisplayHomeAsUpEnabled(false)//添加默认的返回图标
                    supportActionBar?.setHomeButtonEnabled(false)//设置返回键可用
                    return true
                }
            })

        }
        val checkItem = menu?.findItem(R.id.HideSystemApp)
        if(checkItem != null){
            checkItem.isChecked = hideSystemApp
            checkItem.setOnMenuItemClickListener(object : MenuItem.OnMenuItemClickListener{
                override fun onMenuItemClick(item: MenuItem): Boolean {
                    if(!item.isChecked){
                        //Hide System App
                        hideSystemApp = true

                        item.isChecked = true
                    }
                    else{
                        hideSystemApp = false

                        item.isChecked = false
                    }
                    return true;
                }
            })
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId){
            android.R.id.home -> {
                PressBackBtn()
            }
            R.id.help -> {
                val intent = Intent(this, HelpActivity::class.java)
                startActivity(intent)
            }
            R.id.GcmDiagnostics -> {
                val intent = Intent()
                val comp = ComponentName("com.google.android.gms", "com.google.android.gms.gcm.GcmDiagnostics")
                intent.setComponent(comp)
                startActivity(intent)
            }
            R.id.HideSystemApp -> {

            }
        }
        return true
    }

    private fun PressBackBtn(){
        if(!mSearchView.isIconified){
            mSearchView.setQuery("", false)
            mSearchView.clearFocus()
            mSearchView.onActionViewCollapsed()
            supportActionBar?.setDisplayHomeAsUpEnabled(false)//添加默认的返回图标
            supportActionBar?.setHomeButtonEnabled(false)//设置返回键可用
        }else{
            moveTaskToBack(true)
        }
    }

    private fun getAppList(hideSystemApp:Boolean = false){
        appList.clear()

        thread {
            val packageManager = packageManager
            for (packageInfo in packageManager.getInstalledPackages(PackageManager.GET_RECEIVERS)) {
                if (packageInfo.receivers != null) {
                    for (receiverInfo in packageInfo.receivers) {
                        if (receiverInfo.name == "com.google.firebase.iid.FirebaseInstanceIdReceiver" || receiverInfo.name == "com.google.android.gms.measurement.AppMeasurementReceiver") {
                            val appName = packageInfo.applicationInfo.loadLabel(packageManager).toString()
                            val packageName = packageInfo.packageName
                            var icon:Drawable? = packageInfo.applicationInfo.loadIcon(packageManager);
                            val isSystemApp = (packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                            if(!(hideSystemApp && isSystemApp)){
                                val appInfo = AppInfo(appName, packageName, icon, isSystemApp)
                                appList.add(appInfo)
                            }

                            break
                        }
                    }
                }
            }
            runOnUiThread {
                recyclerView.layoutManager = LinearLayoutManager(this)
                recyclerView.adapter = AppInfoListAdapter(appList)
                swipeRefresh.isRefreshing = false
            }
        }

    }
}
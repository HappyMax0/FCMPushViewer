package com.happymax.fcmpushviewer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.graphics.Insets
import androidx.core.view.MenuItemCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {
    private lateinit var mainFragment: MainFragment
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

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        setupWindow()

        val toolbar: Toolbar = findViewById(R.id.toolBar)
        setSupportActionBar(toolbar)

        sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)

        mainFragment = supportFragmentManager.findFragmentById(R.id.main_fragment) as MainFragment

        recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        hideSystemApp = sharedPreferences.getBoolean("HideSystemApp", false)

        val colorAccent = getThemeColor(this, androidx.appcompat.R.attr.colorAccent)

        swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefresh.setColorSchemeColors(colorAccent)
        swipeRefresh.setOnRefreshListener {
            getAppList(hideSystemApp)
        }

        val floatingActionBtn:FloatingActionButton = findViewById(R.id.floatingActionBtn)
        floatingActionBtn.setOnClickListener { view ->
            navigateToFCM()
           }
    }

    private fun setupWindow() {
        val coordinatorLayout: CoordinatorLayout = findViewById(R.id.coordinatorLayout)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(coordinatorLayout) { v, insets ->
            val systemWindowInsets = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            coordinatorLayout.updatePadding(
                top = systemWindowInsets.top,
                left = systemWindowInsets.left,
                right = systemWindowInsets.right,
                bottom = systemWindowInsets.bottom)

            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            WindowInsetsCompat.Builder(insets)
                .setInsets(
                    WindowInsetsCompat.Type.systemBars(),
                    Insets.of(0, 0, 0, systemBarInsets.bottom)
                )
                .setInsets(
                    WindowInsetsCompat.Type.ime(),
                    Insets.of(0, 0, 0, imeInsets.bottom)
                )
                .build()
        }
    }

    private fun getThemeColor(context: Context, attribute: Int): Int {
        val typedValue = TypedValue()
        val theme = context.theme
        theme.resolveAttribute(attribute, typedValue, true)
        return typedValue.data
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_main_toolbar, menu)
        val item = menu?.findItem(R.id.search)
        if(item != null){
            mSearchView = MenuItemCompat.getActionView(item) as SearchView
            mSearchView.isIconified = true
            mSearchView.isSubmitButtonEnabled = false
            mSearchView.setOnSearchClickListener {
                supportActionBar?.setDisplayHomeAsUpEnabled(true)//添加默认的返回图标
                supportActionBar?.setHomeButtonEnabled(true)//设置返回键可用
            }

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
            R.id.HideSystemApp -> {

            }
        }
        return true
    }

    private fun navigateToFCM(){
        val intent = Intent()
        val comp = ComponentName("com.google.android.gms", "com.google.android.gms.gcm.GcmDiagnostics")
        intent.setComponent(comp)
        startActivity(intent)
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
                var supportFCM = false
                if (packageInfo?.receivers != null) {
                    for (receiverInfo in packageInfo.receivers!!) {
                        supportFCM = receiverInfo.name == "com.google.firebase.iid.FirebaseInstanceIdReceiver" || receiverInfo.name == "com.google.android.gms.measurement.AppMeasurementReceiver"
                        if(supportFCM)
                            break
                    }
                }

                if(packageInfo.applicationInfo != null)
                {
                    val appName = packageInfo.applicationInfo!!.loadLabel(packageManager).toString()
                    val packageName = packageInfo.packageName
                    var icon:Drawable? = packageInfo.applicationInfo!!.loadIcon(packageManager);
                    val isSystemApp = (packageInfo.applicationInfo!!.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    if(!(hideSystemApp && isSystemApp)){
                        val appInfo = AppInfo(appName, packageName, icon, isSystemApp, supportFCM)
                        appList.add(appInfo)
                    }
                }
            }
            runOnUiThread {
                val isLargeLayout = (resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE
                if(!isLargeLayout){
                    recyclerView.layoutManager = LinearLayoutManager(this)
                }
                else{
                    val spanCount = calculateSpanCount()
                    recyclerView.layoutManager = GridLayoutManager(this, spanCount)
                }

                recyclerView.adapter = AppInfoListAdapter(appList)
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun replaceFragment(layoutID:Int, fragment: Fragment){
        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(layoutID, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun calculateSpanCount(): Int {
        val displayMetrics = resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        val columnWidthDp = 400 // 每列的宽度（dp）

        return (screenWidthDp / columnWidthDp).toInt().coerceAtLeast(1)
    }
}
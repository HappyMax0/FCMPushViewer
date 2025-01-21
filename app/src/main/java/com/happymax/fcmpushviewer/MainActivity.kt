package com.happymax.fcmpushviewer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.MenuItemCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlin.concurrent.thread
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        var composeView = findViewById<ComposeView>(R.id.compose_view)
        composeView.setContent {
            MaterialTheme{
                AppList()
            }
        }
    }

    private fun calculateSpanCount(): Int {
        val displayMetrics = resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        val columnWidthDp = 400 // 每列的宽度（dp）

        return (screenWidthDp / columnWidthDp).toInt().coerceAtLeast(1)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppList(){
    //val navController = rememberNavController()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("settings", MODE_PRIVATE)
    var showSystemApp by rememberSaveable { mutableStateOf(!sharedPreferences.getBoolean("HideSystemApp", false)) }
    var menuExpanded by remember { mutableStateOf(false) }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var searchText by rememberSaveable { mutableStateOf("") }
    val fullAppList:ArrayList<AppInfo> by rememberSaveable { mutableStateOf(getAppList(context)) }

    val appList = fullAppList
        .filter { it.appName.contains(searchText) }

    Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    if(!isSearchActive)
                        Text(stringResource(id = R.string.app_name))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                actions = {
                    if(!isSearchActive) {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(id = R.string.toolbar_search))
                        }
                    }else
                    {
                        Row {
                            TextField(
                                value = searchText,
                                onValueChange = { query ->
                                    searchText = query
                                },
                                placeholder = { Text(stringResource(id = R.string.toolbar_search)) },
                                singleLine = true,
                                modifier = Modifier
                                    .width(200.dp)
                                ,
                            )
                            IconButton(onClick = {
                                isSearchActive = false
                                searchText = ""
                            }, modifier = Modifier
                                .align(Alignment.CenterVertically)) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(id = R.string.toolbar_exitSearch))
                            }
                        }
                    }
                    //more button
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.toolbar_more))
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = stringResource(id = R.string.toolbar_showSystemApp))

                                Checkbox(
                                    checked = showSystemApp,
                                    onCheckedChange = {
                                        showSystemApp = it
                                        val editor = sharedPreferences.edit()
                                        editor.putBoolean("HideSystemApp", !showSystemApp)
                                        editor.apply()
                                    })
                            }
                        }, onClick = {
                            showSystemApp = !showSystemApp
                            val editor = sharedPreferences.edit()
                            editor.putBoolean("HideSystemApp", !showSystemApp)
                            editor.apply()
                        })
                        DropdownMenuItem(text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = stringResource(id = R.string.toolbar_help))
                            }
                        }, onClick = {
                            val intent = Intent(context, HelpActivity::class.java)
                            ContextCompat.startActivity(context, intent, null)
                        })
                    }
                },
                scrollBehavior = scrollBehavior)
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val intent = Intent()
                val comp = ComponentName("com.google.android.gms", "com.google.android.gms.gcm.GcmDiagnostics")
                intent.setComponent(comp)
                context.startActivity(intent) })
            {
                Icon(painterResource(R.drawable.baseline_cloud_sync), contentDescription = stringResource(R.string.toolbar_openGcmDiagnostics))

            }
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)){
            items(appList) { item ->
                if(!item.systemApp || (item.systemApp && showSystemApp))
                    ShowAppInfo(item)
            }
        }
    }
}

@Composable
fun ShowAppInfo(appInfo: AppInfo, modifier: Modifier = Modifier) {

    Surface(
        modifier = modifier,
        content =  {
            Column(modifier=modifier.fillMaxWidth()) {
                Row(modifier = modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    , horizontalArrangement = Arrangement.SpaceBetween){
                    Box(modifier=modifier.weight(1f)){
                        Column {
                            Row {
                                if(appInfo.icon != null)
                                    Image(bitmap = appInfo.icon.asImageBitmap(), contentDescription = appInfo.appName,
                                        modifier = Modifier
                                            .width(60.dp)
                                            .height(60.dp)
                                            .padding(10.dp))
                                Column(modifier = Modifier
                                    .align(Alignment.CenterVertically)) {
                                    Text(
                                        text = appInfo.appName,
                                        modifier = modifier
                                    )
                                    Text(
                                        text = appInfo.packageName,
                                        modifier = modifier,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    Row{

                    }
                }

            }

        })
}

fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable) {
        return drawable.bitmap
    }

    val bitmap = Bitmap.createBitmap(
        drawable.intrinsicWidth,
        drawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

private fun getAppList(context: Context): ArrayList<AppInfo>{
    val appList:ArrayList<AppInfo> = ArrayList<AppInfo>()
    val packageManager = context.packageManager
    for (packageInfo in packageManager.getInstalledPackages(PackageManager.GET_RECEIVERS)) {
        if (packageInfo.receivers != null) {
            for (receiverInfo in packageInfo.receivers) {
                if (receiverInfo.name == "com.google.firebase.iid.FirebaseInstanceIdReceiver" || receiverInfo.name == "com.google.android.gms.measurement.AppMeasurementReceiver") {
                    val appName = packageInfo.applicationInfo.loadLabel(packageManager).toString()
                    val packageName = packageInfo.packageName
                    var icon:Drawable? = packageInfo.applicationInfo.loadIcon(packageManager);
                    val isSystemApp = (packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val appInfo = AppInfo(appName, packageName, if (icon!=null) drawableToBitmap(icon) else null, isSystemApp)
                    appList.add(appInfo)

                    break
                }
            }
        }
    }
    return  appList
}

@Preview
@Composable
fun AppListPreview() {
    MaterialTheme{
        AppList()
    }
}

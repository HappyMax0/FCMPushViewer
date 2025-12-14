package com.happymax.fcmpushviewer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.happymax.fcmpushviewer.ui.theme.FCMPushViewerTheme
import kotlinx.serialization.Serializable
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.graphics.Color
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import kotlinx.coroutines.launch

@Serializable
object AppList

@Serializable
object Help

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FCMPushViewerTheme{
                NavBase()
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

@Composable
fun NavBase(){
    val navController = rememberNavController()
    val context = LocalContext.current
    NavHost(navController = navController, startDestination = AppList) {
        composable<AppList> {
            AppListScreen(onItemClick = { packageName -> val intent = Intent()
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.setData(Uri.parse("package:" + packageName))
                        context.startActivity(intent)
            }, onFloatButtonClick = {
                //navController.navigate(FCMDiagnostics)
                val intent = Intent(context, FCMActivity::class.java)
                context.startActivity(intent)},
                onHelpItemClick = { navController.navigate(route = Help) }) }
        composable<Help> { HelpPage(onBackBtnPressed = { navController.popBackStack() }) }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun AppListScreen(onItemClick: (String) -> Unit ={} , onFloatButtonClick: () -> Unit = {}, onHelpItemClick: () -> Unit = {}){
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("settings", MODE_PRIVATE)
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    var showSystemApp by rememberSaveable { mutableStateOf(!sharedPreferences.getBoolean("HideSystemApp", false)) }
    var menuExpanded by remember { mutableStateOf(false) }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var searchText by rememberSaveable { mutableStateOf("") }
    var fullAppList: List<AppInfo> = getAppList(context)

    // 2. 下拉刷新逻辑
    fun refreshData() = coroutineScope.launch {
        isRefreshing = true
        // 更新数据（例如：在现有列表前插入新数据，或完全替换）
        fullAppList = getAppList(context)
        isRefreshing = false
    }

    // 3. 创建 PullRefreshState
    // 记住状态，用于管理下拉手势和刷新指示器的位置
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing, // 当前是否正在刷新
        onRefresh = ::refreshData // 触发下拉时调用的函数
    )

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
                        Row(
                            modifier = Modifier.fillMaxWidth(), // Row 占据整个宽度
                            verticalAlignment = Alignment.CenterVertically // 垂直方向居中对齐
                        ) {
                            IconButton(onClick = { isSearchActive = false }) {
                                Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = stringResource(id = R.string.toolbar_back))
                            }
                            TextField(
                                value = searchText,
                                onValueChange = { query ->
                                    searchText = query
                                },
                                placeholder = { Text(stringResource(id = R.string.toolbar_search)) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                trailingIcon = {
                                    IconButton(onClick = {
                                        isSearchActive = false
                                        searchText = ""
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = stringResource(id = R.string.toolbar_exitSearch))
                                    }
                                }
                            )
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
                            onHelpItemClick()
                        })
                    }
                },
                scrollBehavior = scrollBehavior)
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                onFloatButtonClick()
            })
            {
                Icon(painterResource(R.drawable.baseline_cloud_sync), contentDescription = stringResource(R.string.toolbar_openGcmDiagnostics))
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // 确保内容避开 TopAppBar 和 BottomBar
                // 将 pullRefresh 修改器应用于 Box
                .pullRefresh(pullRefreshState)
        ) {
            LazyColumn() {
                items(appList) { item ->
                    if (!item.systemApp || (item.systemApp && showSystemApp))
                        ShowAppInfo(item, onClick = { item ->
                            onItemClick(item.packageName)
                        })
                }
            }
            // PullRefreshIndicator - 刷新指示器
            // 确保它覆盖在 LazyColumn 之上，并位于顶部中央
            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                // 可选：更改颜色等属性
                // scale = true // 如果你想要 Material 3 风格的缩小/放大动画
            )
        }
    }
}

@Composable
fun ShowAppInfo(appInfo: AppInfo, onClick:(AppInfo) -> Unit, modifier: Modifier = Modifier) {

    Surface(
        modifier = modifier,
        content =  {
            Column(modifier=modifier.fillMaxWidth()) {
                Row(modifier = modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .clickable { onClick(appInfo) }, horizontalArrangement = Arrangement.SpaceBetween){
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
            for (receiverInfo in packageInfo.receivers!!) {
                if (packageInfo.applicationInfo != null && receiverInfo.name == "com.google.firebase.iid.FirebaseInstanceIdReceiver" || receiverInfo.name == "com.google.android.gms.measurement.AppMeasurementReceiver") {
                    val appName = packageInfo.applicationInfo!!.loadLabel(packageManager).toString()
                    val packageName = packageInfo.packageName
                    var icon:Drawable? = packageInfo.applicationInfo!!.loadIcon(packageManager);
                    val isSystemApp = (packageInfo.applicationInfo!!.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val appInfo = AppInfo(appName, packageName, if (icon!=null) drawableToBitmap(icon) else null, isSystemApp)
                    appList.add(appInfo)

                    break
                }
            }
        }
    }
    return  appList
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpPage(onBackBtnPressed:()->Unit = {}){
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(id = R.string.toolbar_help))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                navigationIcon = {
                    IconButton(onClick = { onBackBtnPressed() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.toolbar_back))
                    }
                },
                scrollBehavior = scrollBehavior)
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)){
            Text(stringResource(R.string.help_text), modifier = Modifier.padding(20.dp))
        }
    }
}

@Preview
@Composable
fun HelpPagePreview() {
    MaterialTheme{
        HelpPage()
    }
}

@Preview
@Composable
fun AppListPreview() {
    MaterialTheme{
        AppListScreen()
    }
}

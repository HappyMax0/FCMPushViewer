package com.happymax.fcmpushviewer

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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
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
import androidx.compose.ui.graphics.Color
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.util.Locale.getDefault

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
    NavHost(navController = navController,
        startDestination = AppList,
        // 整个 NavHost 的全局动画配置
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(500)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(500)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(500)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(500)
            )
        }) {
        composable<AppList> {
            AppListScreen(onItemClick = { packageName -> val intent = Intent()
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.setData(Uri.parse("package:" + packageName))
                        context.startActivity(intent)
            }, onFloatButtonClick = {
                val intent = Intent(context, FCMActivity::class.java)
                context.startActivity(intent)},
                onHelpItemClick = { navController.navigate(route = Help) }) }
        composable<Help> { HelpPage(onBackBtnPressed = { navController.popBackStack() }) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleSearchBar(
    onExit: ()->Unit,
    source: List<AppInfo>,
    modifier: Modifier = Modifier
) {
    // Controls expansion state of the search bar
    var expanded by rememberSaveable { mutableStateOf(true) }
    // Manage query state
    var query by rememberSaveable { mutableStateOf("") }

    // Filter items based on query
    val resultList by remember {
        derivedStateOf {
            source.filter {
                it.appName.lowercase(getDefault()).contains(query.trim().lowercase(getDefault()))
                        || it.packageName.lowercase(getDefault()).contains(query.trim().lowercase(getDefault()))
            }
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .semantics { isTraversalGroup = true }
    ) {
        SearchBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .semantics { traversalIndex = 0f },
            inputField = {
                SearchBarDefaults.InputField(
                    query = query,
                    onQueryChange = {
                        query = it
                        Log.d("SimpleSearchBar", query)
                        },
                    onSearch = {
                       // expanded = false
                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    placeholder = { Text(stringResource(R.string.toolbar_search)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = { IconButton(onClick = {
                        expanded = false
                        onExit()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(id = R.string.toolbar_exitSearch)) }
                         },
                )
            },
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            // Show search results in a lazy column for better performance
            Column {
                // 使用 Spacer 手动空行
                Spacer(modifier = Modifier.height(20.dp))

                LazyVerticalGrid(// 🌟 核心：设置最小宽度为 150.dp，系统自动决定列数
                    columns = GridCells.Adaptive(minSize = 360.dp)
                ) {
                    items(count = resultList.size) { index ->
                        ShowAppInfo(resultList[index], {})
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun AppListScreen(onItemClick: (String) -> Unit ={} , onFloatButtonClick: () -> Unit = {}, onHelpItemClick: () -> Unit = {}, viewModel: AppListViewModel = viewModel()){
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("settings", MODE_PRIVATE)
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    var showSystemApp by rememberSaveable { mutableStateOf(!sharedPreferences.getBoolean("HideSystemApp", false)) }
    var showNotSupportedApp by rememberSaveable { mutableStateOf(sharedPreferences.getBoolean("ShowNotSupportedApp", false)) }
    var menuExpanded by remember { mutableStateOf(false) }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    val fullAppList: List<AppInfo> by viewModel.appList.collectAsStateWithLifecycle()
    //val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    // 2. 下拉刷新逻辑
    fun refreshData() = coroutineScope.launch {
        isRefreshing = true
        // 更新数据（例如：在现有列表前插入新数据，或完全替换）
        viewModel.loadData()
        isRefreshing = false
    }
    // 3. 创建 PullRefreshState
    // 记住状态，用于管理下拉手势和刷新指示器的位置
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing, // 当前是否正在刷新
        onRefresh = ::refreshData // 触发下拉时调用的函数
    )

    val appList = fullAppList
        .filter { (!it.systemApp || (it.systemApp && showSystemApp)) && (it.supportFCM || it.supportFCM != showNotSupportedApp) }

    if(isSearchActive){
        SimpleSearchBar({ isSearchActive = false }, appList)
    }
    else{
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TopAppBar(
                    title = {
                        if(!isSearchActive){
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(stringResource(id = R.string.app_name))
                                Spacer(modifier = Modifier.width(8.dp))
                                // 数量显示：例如 " (120)"
                                Text(
                                    text = "(${appList.size})",
                                    style = MaterialTheme.typography.titleMedium, // 数量可以用稍小的字体
                                    color = MaterialTheme.colorScheme.onSurfaceVariant // 使用副文本颜色
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer // 滚动后也不变色
                    ),
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(id = R.string.toolbar_search))
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
                                    Text(text = stringResource(id = R.string.toolbar_showUnsupportApp))

                                    Checkbox(
                                        checked = showNotSupportedApp,
                                        onCheckedChange = {
                                            showNotSupportedApp = it
                                            val editor = sharedPreferences.edit()
                                            editor.putBoolean("ShowNotSupportedApp", showNotSupportedApp)
                                            editor.apply()
                                        })
                                }
                            }, onClick = {
                                showNotSupportedApp = !showNotSupportedApp
                                val editor = sharedPreferences.edit()
                                editor.putBoolean("ShowNotSupportedApp", showNotSupportedApp)
                                editor.apply()
                            })
                            DropdownMenuItem(text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = stringResource(id = R.string.toolbar_help))
                                }
                            }, onClick = {
                                menuExpanded = false
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
                Column {
                    // 使用 Spacer 手动空行
                    Spacer(modifier = Modifier.height(20.dp))

                    LazyVerticalGrid(// 🌟 核心：设置最小宽度为 150.dp，系统自动决定列数
                        columns = GridCells.Adaptive(minSize = 360.dp)
                    ) {
                        items(appList) { item ->
                            ShowAppInfo(item, onClick = { item ->
                                onItemClick(item.packageName)
                            })
                        }
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
}

@Composable
fun ShowAppInfo(appInfo: AppInfo, onClick:(AppInfo) -> Unit, modifier: Modifier = Modifier) {

    // 使用 Card 或 Surface 来自动处理圆角和深色模式背景
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick(appInfo) }, // 左右外边距
        shape = RoundedCornerShape(24.dp), // 设置较大的圆角
        colors = CardDefaults.cardColors(
            // 关键：容器颜色会自动随系统深/浅色模式切换
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // 如果不需要阴影可以设为0
    ){
        Row(modifier = modifier
            .fillMaxWidth()
            .padding(10.dp)
            , horizontalArrangement = Arrangement.SpaceBetween){
            Box(modifier=modifier.weight(1f)){
                Row {
                    if(appInfo.icon != null)
                        Image(bitmap = appInfo.icon.asImageBitmap(), contentDescription = appInfo.appName,
                            modifier = Modifier
                                .width(60.dp)
                                .height(60.dp)
                                .padding(10.dp))
                    Column(modifier = Modifier
                        .align(Alignment.CenterVertically)) {
                        Row{
                            Text(
                                text = appInfo.appName,
                                modifier = modifier
                            )
                            // 这个 Spacer 会占据所有剩余空间
                            Spacer(modifier = Modifier.weight(1f))
                            if(appInfo.supportFCM)
                                Icon(painterResource(R.drawable.cloud_done_24px), contentDescription = stringResource(R.string.shortcut_shortlabel_GcmDiagnostics),
                                    modifier=Modifier.padding(4.dp, 4.dp, 10.dp, 4.dp))
                        }

                        Text(
                            text = appInfo.packageName,
                            modifier = modifier,
                            fontSize = 12.sp
                        )
                    }

                }
            }
        }
    }
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
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer // 滚动后也不变色
                ),
                navigationIcon = {
                    IconButton(onClick = { onBackBtnPressed() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.toolbar_back))
                    }
                },
                scrollBehavior = scrollBehavior)
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).
        verticalScroll(rememberScrollState()) // 使内容可滚动
        ){
            //Text(stringResource(R.string.help_text), modifier = Modifier.padding(20.dp))
            // 使用 Spacer 手动空行
            Spacer(modifier = Modifier.height(20.dp))

            HelpTitle(R.string.help_whatsfcm)
            HelpDescription(R.string.help_whatsfcm_description)

            HelpTitle(R.string.help_user_guide)
            HelpDescription(R.string.help_user_guide_1)
            HelpDescription(R.string.help_user_guide_2)
            HelpDescription(R.string.help_user_guide_3)
            HelpDescription(R.string.help_user_guide_4)

            Spacer(modifier = Modifier.weight(1f)) // 对应 layout_alignParentBottom 的效果

            // 底部版权声明
            Text(
                text = stringResource(R.string.help_copyright_statement_google),
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier
                    .padding(vertical = 20.dp, horizontal = 25.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}

// 封装一个通用的标题样式
@Composable
fun HelpTitle(textRes: Int) {
    Text(
        text = stringResource(textRes),
        fontSize = 20.sp,
        modifier = Modifier.padding(vertical = 5.dp, horizontal = 25.dp)
    )
}

// 封装一个通用的描述正文样式
@Composable
fun HelpDescription(textRes: Int) {
    Text(
        text = stringResource(textRes),
        fontSize = 16.sp,
        modifier = Modifier.padding(vertical = 20.dp, horizontal = 25.dp)
    )
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

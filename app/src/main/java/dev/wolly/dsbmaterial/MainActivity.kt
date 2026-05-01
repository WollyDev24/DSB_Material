package dev.wolly.dsbmaterial

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.wolly.dsbmaterial.data.SubstitutionEntry
import dev.wolly.dsbmaterial.ui.MainViewModel
import dev.wolly.dsbmaterial.ui.UiState
import dev.wolly.dsbmaterial.ui.theme.DSBMaterialTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val dynamicColor by viewModel.dynamicColor.collectAsState()
            
            DSBMaterialTheme(dynamicColor = dynamicColor) {
                DSBApp(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DSBApp(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val isRoomFirst by viewModel.isRoomFirst.collectAsState()
    val sortByPeriod by viewModel.sortByPeriod.collectAsState()
    val dynamicColor by viewModel.dynamicColor.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val archiveEntries by viewModel.archive.collectAsState()
    
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var selectedDay by remember { mutableStateOf<String?>(null) }
    var showSheet by remember { mutableStateOf(false) }

    BackHandler(enabled = showSheet || uiState is UiState.SelectingClass || selectedTab != 0) {
        if (showSheet) {
            showSheet = false
        } else if (uiState is UiState.SelectingClass) {
            viewModel.cancelClassSelection()
        } else {
            viewModel.setTab(0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        when (selectedTab) {
                            1 -> stringResource(R.string.label_archive)
                            2 -> stringResource(R.string.title_settings)
                            else -> stringResource(R.string.title_main)
                        },
                        fontWeight = FontWeight.ExtraBold
                    ) 
                },
                actions = {
                    if (selectedTab == 0 && (uiState is UiState.Success || uiState is UiState.Idle)) {
                        IconButton(onClick = { viewModel.fetchData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_refresh))
                        }
                    } else if (selectedTab == 1 && archiveEntries.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearArchive() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear archive")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (uiState !is UiState.NeedsLogin && uiState !is UiState.Loading && uiState !is UiState.SelectingClass) {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 80.dp, vertical = 24.dp)
                        .fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp
                ) {
                    NavigationBar(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        modifier = Modifier.height(80.dp),
                        windowInsets = WindowInsets(0, 0, 0, 0)
                    ) {
                        NavigationBarItem(
                            icon = { 
                                val scale by animateFloatAsState(
                                    targetValue = if (selectedTab == 0) 1.25f else 1f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                    label = "home_scale"
                                )
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                                    Icon(
                                        Icons.Default.Home, 
                                        contentDescription = stringResource(R.string.title_main), 
                                        modifier = Modifier.size(26.dp).graphicsLayer(scaleX = scale, scaleY = scale)
                                    )
                                }
                            },
                            selected = selectedTab == 0,
                            onClick = { viewModel.setTab(0) },
                            alwaysShowLabel = false,
                            label = null
                        )
//                        NavigationBarItem(
//                            icon = {
//                                val scale by animateFloatAsState(
//                                    targetValue = if (selectedTab == 1) 1.25f else 1f,
//                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
//                                    label = "archive_scale"
//                                )
//                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
//                                    Icon(
//                                        Icons.Default.Archive,
//                                        contentDescription = stringResource(R.string.label_archive),
//                                        modifier = Modifier.size(26.dp).graphicsLayer(scaleX = scale, scaleY = scale)
//                                    )
//                                }
//                            },
//                            selected = selectedTab == 1,
//                            onClick = { viewModel.setTab(1) },
//                            alwaysShowLabel = false,
//                            label = null
//                        )
                        NavigationBarItem(
                            icon = { 
                                val scale by animateFloatAsState(
                                    targetValue = if (selectedTab == 2) 1.25f else 1f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                    label = "settings_scale"
                                )
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                                    Icon(
                                        Icons.Default.Settings, 
                                        contentDescription = stringResource(R.string.title_settings), 
                                        modifier = Modifier.size(26.dp).graphicsLayer(scaleX = scale, scaleY = scale)
                                    )
                                }
                            },
                            selected = selectedTab == 2,
                            onClick = { viewModel.setTab(2) },
                            alwaysShowLabel = false,
                            label = null
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            AnimatedContent(
                targetState = if (uiState is UiState.NeedsLogin || uiState is UiState.Loading || uiState is UiState.SelectingClass) uiState else selectedTab,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(300, delayMillis = 90)) + scaleIn(initialScale = 0.92f, animationSpec = tween(300, delayMillis = 90)))
                        .togetherWith(fadeOut(animationSpec = tween(90)))
                },
                label = "screen_transition"
            ) { target ->
                when (target) {
                    is UiState.Loading -> LoadingScreen()
                    is UiState.SelectingClass -> ClassSelectionScreen(
                        classes = target.classes,
                        onClassSelected = { cls -> viewModel.selectClass(target.u, target.p, cls) }
                    )
                    is UiState.NeedsLogin -> LoginScreen(onLogin = viewModel::login)
                    0 -> { // Substitutions
                        val state = uiState
                        if (state is UiState.Success) {
                            DayList(
                                entries = state.entries,
                                onDayClick = { day ->
                                    selectedDay = day
                                    showSheet = true
                                }
                            )
                            
                            if (showSheet && selectedDay != null) {
                                ModalBottomSheet(
                                    onDismissRequest = { showSheet = false },
                                    sheetState = sheetState,
                                    shape = MaterialTheme.shapes.extraLarge,
                                    dragHandle = {
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    scope.launch {
                                                        if (sheetState.currentValue == SheetValue.PartiallyExpanded) {
                                                            sheetState.expand()
                                                        } else {
                                                            sheetState.partialExpand()
                                                        }
                                                    }
                                                }
                                                .padding(top = 16.dp, bottom = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            BottomSheetDefaults.DragHandle()
                                        }
                                    }
                                ) {
                                    val dayEntries = state.entries.filter { it.day == selectedDay }
                                    val isExpanded = sheetState.targetValue == SheetValue.Expanded
                                    SubstitutionViewer(
                                        selectedDay!!, 
                                        dayEntries, 
                                        isRoomFirst, 
                                        isExpanded
                                    )
                                }
                            }
                        } else if (state is UiState.Error) {
                            ErrorScreen(state.message, onRetry = { viewModel.fetchData() })
                        } else {
                            Box(Modifier.fillMaxSize())
                        }
                    }
                    1 -> ArchiveScreen(archiveEntries, isRoomFirst, onRemove = viewModel::removeFromArchive)
                    2 -> SettingsScreen(
                        isRoomFirst = isRoomFirst,
                        sortByPeriod = sortByPeriod,
                        dynamicColor = dynamicColor,
                        onToggleOrder = viewModel::toggleColumnOrder,
                        onToggleSort = viewModel::toggleSortByPeriod,
                        onToggleDynamic = viewModel::toggleDynamicColor,
                        onChangeClass = viewModel::changeClass,
                        onLogout = viewModel::logout,
                        onAbout = { /* Archive and Settings already have their screens */ }
                    )
                    else -> Box(Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
fun ArchiveScreen(entries: List<SubstitutionEntry>, isRoomFirst: Boolean, onRemove: (SubstitutionEntry) -> Unit) {
    if (entries.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(16.dp))
                Text("No archived substitutions", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
            }
        }
    } else {
        val grouped = entries.groupBy { it.day }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            grouped.forEach { (day, dayEntries) ->
                item {
                    Text(day, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
                }
                items(dayEntries) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
                    ) {
                        Column {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                SubstitutionTableRow(entry, isRoomFirst)
                                IconButton(onClick = { onRemove(entry) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    isRoomFirst: Boolean,
    sortByPeriod: Boolean,
    dynamicColor: Boolean,
    onToggleOrder: () -> Unit,
    onToggleSort: () -> Unit,
    onToggleDynamic: () -> Unit,
    onChangeClass: () -> Unit,
    onLogout: () -> Unit,
    onAbout: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                stringResource(R.string.label_preferences),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
            )
        }

        item {
            SettingCard {
                Column {
                    SettingItem(
                        title = stringResource(R.string.action_swap_data),
                        description = if (isRoomFirst) stringResource(R.string.desc_swap_default) else stringResource(R.string.desc_swap_active),
                        icon = Icons.Default.SwapHoriz,
                        trailing = { Switch(checked = !isRoomFirst, onCheckedChange = { onToggleOrder() }) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                    SettingItem(
                        title = stringResource(R.string.label_sort_period),
                        description = stringResource(R.string.desc_sort_period),
                        icon = Icons.Default.Sort,
                        trailing = { Switch(checked = sortByPeriod, onCheckedChange = { onToggleSort() }) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                    SettingItem(
                        title = stringResource(R.string.label_dynamic_color),
                        description = stringResource(R.string.desc_dynamic_color),
                        icon = Icons.Default.Palette,
                        trailing = { Switch(checked = dynamicColor, onCheckedChange = { onToggleDynamic() }) }
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.label_account),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
            )
        }

        item {
            SettingCard {
                Column {
                    SettingItem(
                        title = stringResource(R.string.action_switch_class),
                        description = stringResource(R.string.desc_switch_class),
                        icon = Icons.Default.School,
                        onClick = onChangeClass
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                    SettingItem(
                        title = stringResource(R.string.action_logout),
                        description = stringResource(R.string.desc_logout),
                        icon = Icons.Filled.Logout,
                        iconColor = MaterialTheme.colorScheme.error,
                        onClick = onLogout
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            SettingCard(onClick = onAbout) {
                SettingItem(
                    title = stringResource(R.string.label_about),
                    description = stringResource(R.string.desc_about),
                    icon = Icons.Default.Info
                )
            }
        }
    }
}

@Composable
fun SettingCard(onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
        content = content
    )
}

@Composable
fun SettingItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconColor)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing?.invoke()
    }
}

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.size(140.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.School,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        Text(stringResource(R.string.title_main), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
        Text(stringResource(R.string.desc_about), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "An open-source material you replacement for the DSBmobile app. Built with modern Jetpack Compose for a better user experience.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(24.dp),
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
fun LoadingScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearEasing)),
        label = "rotation"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "scale"
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.size(100.dp).graphicsLayer {
                rotationZ = rotation
                scaleX = scale
                scaleY = scale
            },
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.errorContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.msg_error), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(8.dp))
        Text(message, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onRetry, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_retry))
        }
    }
}

@Composable
fun LoginScreen(onLogin: (String, String) -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    // Detect if keyboard is open to scale the icon
    val isKeyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val iconSize by animateDpAsState(if (isKeyboardOpen) 60.dp else 100.dp, label = "icon_size")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding() // Shifting upward
            .verticalScroll(scrollState)
            .padding(horizontal = 32.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(iconSize),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.School, 
                    contentDescription = null, 
                    modifier = Modifier.size(iconSize * 0.5f), 
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        Text(stringResource(R.string.title_login), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(40.dp))
        
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(stringResource(R.string.label_username)) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.label_password)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            singleLine = true
        )
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = { onLogin(username, password) },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Text(stringResource(R.string.action_continue), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        
        // Extra space at bottom to ensure scrollability when keyboard is up
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun ClassSelectionScreen(classes: List<String>, onClassSelected: (String) -> Unit) {
    var customClass by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.title_select_class), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.desc_select_class), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        
        OutlinedTextField(
            value = customClass,
            onValueChange = { customClass = it },
            label = { Text(stringResource(R.string.label_manual_class)) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            trailingIcon = {
                if (customClass.isNotEmpty()) {
                    IconButton(
                        onClick = { onClassSelected(customClass) },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = stringResource(R.string.action_submit))
                    }
                }
            }
        )
        
        Spacer(Modifier.height(32.dp))
        
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(classes) { cls ->
                Surface(
                    onClick = { onClassSelected(cls) },
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Class, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(16.dp))
                        Text(cls, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DayList(entries: List<SubstitutionEntry>, onDayClick: (String) -> Unit) {
    val days = remember(entries) {
        entries.map { it.day }.distinct().filter { day ->
            val lowerDay = day.lowercase()
            !lowerDay.contains("samstag") && !lowerDay.contains("sonntag") &&
            !lowerDay.contains("saturday") && !lowerDay.contains("sunday")
        }
    }
    
    if (days.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.msg_no_substitutions))
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            items(days) { day ->
                val count = entries.count { it.day == day }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(text = day, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (count == 1) stringResource(R.string.format_substitutions_count_one, count)
                                   else stringResource(R.string.format_substitutions_count_many, count),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { onDayClick(day) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(R.string.action_view_substitutions), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubstitutionViewer(
    day: String, 
    entries: List<SubstitutionEntry>, 
    isRoomFirst: Boolean, 
    isExpanded: Boolean
) {
    val headerFontSize by animateFloatAsState(
        targetValue = if (isExpanded) 36f else 22f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "header_font_size"
    )
    val containerPadding by animateDpAsState(
        targetValue = if (isExpanded) 8.dp else 12.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "container_padding"
    )
    val textPaddingStart by animateDpAsState(
        targetValue = if (isExpanded) 12.dp else 16.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "text_padding_start"
    )
    val textPaddingTop by animateDpAsState(
        targetValue = if (isExpanded) 8.dp else 16.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "text_padding_top"
    )

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = containerPadding)) {
        Text(
            text = day,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = headerFontSize.sp,
                lineHeight = (headerFontSize * 1.2f).sp
            ),
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(start = textPaddingStart, top = textPaddingTop, bottom = 16.dp)
        )
        
        Surface(
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Row(modifier = Modifier.padding(vertical = 14.dp, horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                TableHeaderCell(stringResource(R.string.label_period_short), 1f)
                TableHeaderCell(stringResource(R.string.label_subject_short), 1.8f)
                TableHeaderCell(stringResource(R.string.label_room), 1.4f)
                TableHeaderCell(stringResource(R.string.label_type), 2f)
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f), 
            contentPadding = PaddingValues(bottom = 100.dp, start = 8.dp, end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(entries) { entry ->
                SubstitutionTableRow(entry, isRoomFirst)
            }
        }
    }
}

@Composable
fun RowScope.TableHeaderCell(text: String, weight: Float) {
    Text(
        text = text, 
        modifier = Modifier.weight(weight), 
        style = MaterialTheme.typography.labelMedium, 
        fontWeight = FontWeight.ExtraBold, 
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
        letterSpacing = 1.sp
    )
}

@Composable
fun SubstitutionTableRow(
    entry: SubstitutionEntry, 
    isRoomFirst: Boolean
) {
    val roomDisplay = if (isRoomFirst) entry.room else entry.art
    val typeDisplay = if (isRoomFirst) entry.art else entry.room
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TableCell(entry.lesson, 1f, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                TableCell(entry.subject, 1.8f, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                TableCell(roomDisplay.ifEmpty { "—" }, 1.4f, fontWeight = FontWeight.Bold)
                TableCell(typeDisplay, 2f, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Medium)
            }
            
            if (entry.text.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.padding(top = 12.dp).fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info, 
                            contentDescription = null, 
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = entry.text, 
                            style = MaterialTheme.typography.bodyMedium, 
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.TableCell(
    text: String, 
    weight: Float, 
    fontWeight: FontWeight = FontWeight.Normal, 
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge
) {
    Text(
        text = text, 
        modifier = Modifier.weight(weight), 
        style = style, 
        fontWeight = fontWeight, 
        color = color, 
        maxLines = 2
    )
}

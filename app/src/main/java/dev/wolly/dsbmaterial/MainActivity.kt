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
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var selectedDay by remember { mutableStateOf<String?>(null) }
    var showSheet by remember { mutableStateOf(false) }

    BackHandler(enabled = showSheet || uiState is UiState.Settings || uiState is UiState.SelectingClass || uiState is UiState.About) {
        if (showSheet) {
            showSheet = false
        } else if (uiState is UiState.Settings) {
            viewModel.closeSettings()
        } else if (uiState is UiState.SelectingClass) {
            viewModel.cancelClassSelection()
        } else if (uiState is UiState.About) {
            viewModel.openSettings()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        when {
                            uiState is UiState.Settings -> stringResource(R.string.title_settings)
                            uiState is UiState.About -> stringResource(R.string.title_about)
                            else -> stringResource(R.string.title_main)
                        },
                        fontWeight = FontWeight.ExtraBold
                    ) 
                },
                navigationIcon = {
                    if (uiState is UiState.Settings || uiState is UiState.About) {
                        IconButton(onClick = { 
                            if (uiState is UiState.About) viewModel.openSettings() else viewModel.closeSettings() 
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    }
                },
                actions = {
                    if (uiState is UiState.Success || uiState is UiState.Idle) {
                        IconButton(onClick = { viewModel.fetchData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_refresh))
                        }
                        IconButton(onClick = { viewModel.openSettings() }) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.title_settings))
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            AnimatedContent(
                targetState = uiState,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(300, delayMillis = 90)) + scaleIn(initialScale = 0.92f, animationSpec = tween(300, delayMillis = 90)))
                        .togetherWith(fadeOut(animationSpec = tween(90)))
                },
                label = "screen_transition"
            ) { state ->
                when (state) {
                    is UiState.Loading -> LoadingScreen()
                    is UiState.Success -> {
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
                                SubstitutionViewer(selectedDay!!, dayEntries, isRoomFirst)
                            }
                        }
                    }
                    is UiState.SelectingClass -> ClassSelectionScreen(
                        classes = state.classes,
                        onClassSelected = { cls -> viewModel.selectClass(state.u, state.p, cls) }
                    )
                    is UiState.Settings -> SettingsScreen(
                        isRoomFirst = isRoomFirst,
                        sortByPeriod = sortByPeriod,
                        dynamicColor = dynamicColor,
                        onToggleOrder = viewModel::toggleColumnOrder,
                        onToggleSort = viewModel::toggleSortByPeriod,
                        onToggleDynamic = viewModel::toggleDynamicColor,
                        onChangeClass = viewModel::changeClass,
                        onLogout = viewModel::logout,
                        onAbout = viewModel::openAbout
                    )
                    is UiState.About -> AboutScreen()
                    is UiState.Error -> ErrorScreen(state.message, onRetry = { viewModel.fetchData() })
                    is UiState.NeedsLogin -> LoginScreen(onLogin = viewModel::login)
                    is UiState.Idle -> Box(Modifier.fillMaxSize())
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
                        icon = Icons.Default.Logout,
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
fun SubstitutionViewer(day: String, entries: List<SubstitutionEntry>, isRoomFirst: Boolean) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Text(text = day, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(16.dp))
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Row(modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                TableHeaderCell(stringResource(R.string.label_period_short), 1.2f)
                TableHeaderCell(stringResource(R.string.label_subject_short), 1.8f)
                TableHeaderCell(stringResource(R.string.label_room), 1.4f)
                TableHeaderCell(stringResource(R.string.label_type), 2f)
            }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(bottom = 48.dp, start = 8.dp, end = 8.dp)) {
            items(entries) { entry ->
                SubstitutionTableRow(entry, isRoomFirst)
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 8.dp))
            }
        }
    }
}

@Composable
fun RowScope.TableHeaderCell(text: String, weight: Float) {
    Text(text = text, modifier = Modifier.weight(weight), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
}

@Composable
fun SubstitutionTableRow(entry: SubstitutionEntry, isRoomFirst: Boolean) {
    val roomDisplay = if (isRoomFirst) entry.room else entry.art
    val typeDisplay = if (isRoomFirst) entry.art else entry.room
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TableCell(entry.lesson, 1.2f, fontWeight = FontWeight.ExtraBold)
            TableCell(entry.subject, 1.8f, fontWeight = FontWeight.Bold)
            TableCell(roomDisplay.ifEmpty { "—" }, 1.4f)
            TableCell(typeDisplay, 2f, color = MaterialTheme.colorScheme.secondary)
        }
        if (entry.text.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
            ) {
                Text(text = entry.text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp), lineHeight = 18.sp)
            }
        }
    }
}

@Composable
fun RowScope.TableCell(text: String, weight: Float, fontWeight: FontWeight = FontWeight.Normal, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Text(text = text, modifier = Modifier.weight(weight), style = MaterialTheme.typography.bodyMedium, fontWeight = fontWeight, color = color, maxLines = 2)
}

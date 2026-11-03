package studio.tivaroid.treemivo

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import studio.tivaroid.treemivo.data.*
import studio.tivaroid.treemivo.ui.*
import java.util.Locale

private enum class Section(val labelId: Int, val icon: ImageVector) {
    MAP(R.string.map, Icons.Outlined.GridView),
    FOLDERS(R.string.folders, Icons.Outlined.Folder),
    TYPES(R.string.file_types, Icons.Outlined.InsertDriveFile),
    DUPLICATES(R.string.duplicates, Icons.Outlined.ContentCopy)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = TreemivoAccent,
                    onPrimary = Color(0xFF071926),
                    background = TreemivoBackground,
                    surface = TreemivoPanel,
                    onBackground = Color.White,
                    onSurface = Color.White,
                    surfaceVariant = TreemivoSurface,
                    onSurfaceVariant = TreemivoMuted
                )
            ) {
                TreemivoApp()
            }
        }
    }
}

@Composable
private fun TreemivoApp(vm: StorageViewModel = viewModel()) {
    val context = LocalContext.current
    var section by rememberSaveable { mutableStateOf(Section.MAP) }
    var folderPath by rememberSaveable { mutableStateOf("") }
    var selectedFile by remember { mutableStateOf<FileRecord?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    var settingsOpen by remember { mutableStateOf(false) }

    val chooseTree = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
        if (it != null) vm.setTree(it)
    }
    val exportCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { if (it != null) vm.exportCsv(it) }

    LaunchedEffect(vm.report) {
        folderPath = ""
        selectedFile = null
        search = ""
    }

    val onChooseFolder: () -> Unit = { chooseTree.launch(null) }
    val onExport: () -> Unit = { if (vm.report != null) exportCsv.launch("treemivo_report.csv") }
    val onFolder: (String) -> Unit = {
        folderPath = it
        selectedFile = null
        section = Section.MAP
    }
    val filesInFolder = remember(vm.report, folderPath) {
        vm.report?.files?.filter {
            folderPath.isEmpty() || it.relativePath.startsWith(folderPath + "/")
        } ?: emptyList()
    }
    val filteredFiles = remember(filesInFolder, search) {
        if (search.isBlank()) filesInFolder
        else filesInFolder.filter {
            it.name.contains(search, ignoreCase = true) ||
                it.relativePath.contains(search, ignoreCase = true)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // This adapts to the current WINDOW size, not the phone model. DeX and
        // Android desktop windowing can resize the activity at runtime.
        val expanded = maxWidth >= 900.dp
        Scaffold(
            containerColor = TreemivoBackground,
            bottomBar = {
                if (!expanded) {
                    NavigationBar(containerColor = TreemivoPanel) {
                        Section.entries.forEach { destination ->
                            NavigationBarItem(
                                selected = section == destination,
                                onClick = { section = destination },
                                icon = { Icon(destination.icon, contentDescription = null) },
                                label = { Text(stringResource(destination.labelId), maxLines = 1) },
                                alwaysShowLabel = true,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TreemivoAccent,
                                    selectedTextColor = TreemivoAccent,
                                    indicatorColor = TreemivoSurface,
                                    unselectedIconColor = TreemivoMuted,
                                    unselectedTextColor = TreemivoMuted
                                )
                            )
                        }
                    }
                }
            }
        ) { insets ->
            if (expanded) {
                Row(Modifier.fillMaxSize().padding(insets)) {
                    DesktopRail(
                        selected = section,
                        onSelect = { section = it },
                        onChooseFolder = onChooseFolder,
                        onSettings = { settingsOpen = true }
                    )
                    Column(
                        Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        HeaderActions(
                            isCompact = false,
                            onChooseFolder = onChooseFolder,
                            onRefresh = vm::refresh,
                            onExport = onExport,
                            onSettings = { settingsOpen = true },
                            enabled = vm.report != null
                        )
                        SpaceOverview(vm.deviceSpace)
                        SelectedFolderCard(vm.report, vm.busy, folderPath, onChooseFolder, vm::refresh, { folderPath = "" })
                        if (vm.report == null) {
                            SelectFolderMessage(vm.busy, onChooseFolder)
                        } else {
                            MapCard(
                                filteredFiles,
                                height = 500.dp,
                                onSelect = { selectedFile = it }
                            )
                            if (vm.report?.partial == true) {
                                Text(stringResource(R.string.scan_limit), color = TreemivoMuted)
                            }
                            Text(stringResource(R.string.tap_map_hint), color = TreemivoMuted, fontSize = 13.sp)
                        }
                    }
                    Column(
                        Modifier.width(350.dp).fillMaxHeight().background(TreemivoPanel)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (selectedFile != null) {
                            FileDetailsCard(selectedFile!!, context, Modifier.fillMaxWidth())
                        }
                        Text(
                            stringResource(if (section == Section.MAP) R.string.folders_title else section.labelId),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 19.sp
                        )
                        SearchBox(search, { search = it })
                        ContentForSection(
                            section = if (section == Section.MAP) Section.FOLDERS else section,
                            vm = vm,
                            files = filteredFiles,
                            rootFilter = folderPath,
                            modifier = Modifier.fillMaxSize(),
                            onChooseFolder = onChooseFolder,
                            onSelectFile = { selectedFile = it },
                            onSelectFolder = onFolder
                        )
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    HeaderActions(
                        isCompact = true,
                        onChooseFolder = onChooseFolder,
                        onRefresh = vm::refresh,
                        onExport = onExport,
                        onSettings = { settingsOpen = true },
                        enabled = vm.report != null
                    )
                    SpaceOverview(vm.deviceSpace, Modifier.padding(horizontal = 16.dp))
                    Spacer(Modifier.height(10.dp))
                    SelectedFolderCard(
                        vm.report, vm.busy, folderPath, onChooseFolder, vm::refresh,
                        { folderPath = "" }, Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    if (vm.report == null) {
                        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                            SelectFolderMessage(vm.busy, onChooseFolder)
                        }
                    } else if (section == Section.MAP) {
                        LazyColumn(
                            Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                MapCard(filteredFiles, height = 390.dp, onSelect = { selectedFile = it })
                            }
                            if (selectedFile != null) {
                                item { FileDetailsCard(selectedFile!!, context, Modifier.fillMaxWidth()) }
                            }
                            if (vm.report?.partial == true) item {
                                Text(stringResource(R.string.scan_limit), color = TreemivoMuted)
                            }
                            item {
                                Text(stringResource(R.string.largest_files), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            }
                            items(filteredFiles.sortedByDescending { it.sizeBytes }.take(8), key = { it.uri.toString() }) {
                                FileRow(it, onClick = { selectedFile = it })
                            }
                            item { Spacer(Modifier.height(12.dp)) }
                        }
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            SearchBox(search, { search = it }, Modifier.padding(horizontal = 16.dp))
                            Spacer(Modifier.height(6.dp))
                            ContentForSection(
                                section, vm, filteredFiles, folderPath, Modifier.fillMaxSize(),
                                onChooseFolder, { selectedFile = it }, onFolder
                            )
                        }
                    }
                }
            }
        }
    }

    vm.errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = vm::clearError,
            title = { Text(stringResource(R.string.scan_error)) },
            text = { Text(error) },
            confirmButton = { TextButton(onClick = vm::clearError) { Text(stringResource(R.string.close)) } }
        )
    }
    if (settingsOpen) {
        AlertDialog(
            onDismissRequest = { settingsOpen = false },
            title = { Text(stringResource(R.string.about)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.privacy_details))
                    Text(stringResource(R.string.scan_restricted_details))
                    Text("v0.1.0 · Tivaroid Studio", color = TreemivoMuted)
                }
            },
            confirmButton = {
                TextButton(onClick = { settingsOpen = false }) { Text(stringResource(R.string.close)) }
            }
        )
    }
}

@Composable
private fun BrandTitle(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
        Surface(color = TreemivoSurface, shape = RoundedCornerShape(14.dp)) {
            Icon(
                Icons.Outlined.Storage, contentDescription = null,
                modifier = Modifier.padding(12.dp).size(27.dp), tint = TreemivoAccent
            )
        }
        Column {
            Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold, fontSize = 23.sp, lineHeight = 26.sp)
            Text(stringResource(R.string.app_tagline), color = TreemivoMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun HeaderActions(
    isCompact: Boolean,
    onChooseFolder: () -> Unit,
    onRefresh: () -> Unit,
    onExport: () -> Unit,
    onSettings: () -> Unit,
    enabled: Boolean
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = if (isCompact) 16.dp else 0.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BrandTitle(Modifier.weight(1f))
        if (!isCompact) {
            TextButton(onClick = onChooseFolder) {
                Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.select_folder))
            }
        }
        IconButton(onClick = onRefresh, enabled = enabled) {
            Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.refresh))
        }
        IconButton(onClick = onExport, enabled = enabled) {
            Icon(Icons.Outlined.SaveAlt, contentDescription = stringResource(R.string.export_csv))
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings))
        }
    }
}

@Composable
private fun SpaceOverview(space: DeviceSpace, modifier: Modifier = Modifier) {
    val ratio = if (space.totalBytes == 0L) 0f
        else (space.usedBytes.toFloat() / space.totalBytes).coerceIn(0f, 1f)
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = TreemivoPanel) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Storage, contentDescription = null, tint = TreemivoMuted)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.internal_storage), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text(
                    formatBytes(space.usedBytes) + " / " + formatBytes(space.totalBytes),
                    fontSize = 13.sp, color = TreemivoMuted
                )
            }
            LinearProgressIndicator(
                progress = { ratio }, modifier = Modifier.fillMaxWidth().height(8.dp),
                color = TreemivoAccent, trackColor = TreemivoSurface
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.used_space) + ": " + formatBytes(space.usedBytes), color = TreemivoMuted, fontSize = 12.sp)
                Text(stringResource(R.string.free_space) + ": " + formatBytes((space.totalBytes - space.usedBytes).coerceAtLeast(0)), color = TreemivoMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SelectedFolderCard(
    report: StorageReport?,
    busy: Boolean,
    path: String,
    onChoose: () -> Unit,
    onRefresh: () -> Unit,
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Folder, contentDescription = null, tint = Color(0xFFF8C84D))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.selected_area), color = TreemivoMuted, fontSize = 11.sp)
            Text(
                if (path.isNotEmpty()) path else report?.rootName ?: stringResource(R.string.no_selection),
                fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        if (busy) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        else if (path.isNotEmpty()) TextButton(onClick = onClearFilter) { Text(stringResource(R.string.all_files)) }
        else TextButton(onClick = onChoose) { Text(stringResource(R.string.change_folder)) }
        if (report != null && !busy) {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.refresh), tint = TreemivoMuted)
            }
        }
    }
}

@Composable
private fun SelectFolderMessage(scanning: Boolean, onChoose: () -> Unit) {
    Surface(shape = RoundedCornerShape(22.dp), color = TreemivoPanel) {
        Column(
            Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            if (scanning) CircularProgressIndicator()
            else Icon(Icons.Outlined.FolderOpen, null, modifier = Modifier.size(54.dp), tint = TreemivoAccent)
            Text(
                stringResource(if (scanning) R.string.scanning else R.string.choose_folder_title),
                fontSize = 20.sp, fontWeight = FontWeight.Bold
            )
            Text(stringResource(R.string.choose_folder_message), color = TreemivoMuted)
            if (!scanning) Button(onClick = onChoose) { Text(stringResource(R.string.scan_now)) }
            Text(stringResource(R.string.scan_restricted), color = TreemivoMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun MapCard(files: List<FileRecord>, height: Dp, onSelect: (FileRecord) -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = TreemivoPanel, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.GridView, null, tint = TreemivoAccent)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.map), fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                Spacer(Modifier.weight(1f))
                Text(files.size.toString() + " " + stringResource(R.string.files_count).lowercase(), color = TreemivoMuted, fontSize = 12.sp)
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = TreemivoBackground,
                modifier = Modifier.fillMaxWidth().height(height)
            ) {
                if (files.any { it.sizeBytes > 0 }) TreemapView(files, Modifier.fillMaxSize(), onSelect)
                else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_files), color = TreemivoMuted)
                }
            }
            Text(stringResource(R.string.tap_map_hint), color = TreemivoMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun FileDetailsCard(file: FileRecord, context: Context, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(18.dp), color = TreemivoSurface) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.file_details), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(file.name, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.size) + ": " + formatBytes(file.sizeBytes), color = TreemivoMuted)
            Text(stringResource(R.string.path) + ": " + file.relativePath, color = TreemivoMuted, fontSize = 12.sp)
            TextButton(onClick = { openDocument(context, file) }) {
                Icon(Icons.Outlined.OpenInNew, null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.open_file))
            }
        }
    }
}

@Composable
private fun SearchBox(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.search), fontSize = 13.sp) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = TreemivoMuted) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        textStyle = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun DesktopRail(
    selected: Section,
    onSelect: (Section) -> Unit,
    onChooseFolder: () -> Unit,
    onSettings: () -> Unit
) {
    Column(
        Modifier.width(210.dp).fillMaxHeight().background(TreemivoPanel).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BrandTitle()
        Spacer(Modifier.height(20.dp))
        Section.entries.forEach {
            val active = selected == it
            Row(
                Modifier.fillMaxWidth().background(
                    if (active) TreemivoSurface else Color.Transparent,
                    RoundedCornerShape(12.dp)
                ).clickable { onSelect(it) }.padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(it.icon, contentDescription = null, tint = if (active) TreemivoAccent else TreemivoMuted)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(it.labelId), color = if (active) Color.White else TreemivoMuted)
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onChooseFolder, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.FolderOpen, null)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.select_folder))
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onSettings) {
            Icon(Icons.Outlined.Info, null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.about))
        }
        Text(stringResource(R.string.offline_private), color = TreemivoMuted, fontSize = 12.sp)
    }
}

@Composable
private fun ContentForSection(
    section: Section,
    vm: StorageViewModel,
    files: List<FileRecord>,
    rootFilter: String,
    modifier: Modifier,
    onChooseFolder: () -> Unit,
    onSelectFile: (FileRecord) -> Unit,
    onSelectFolder: (String) -> Unit
) {
    val report = vm.report
    if (report == null) {
        Box(modifier, contentAlignment = Alignment.Center) { SelectFolderMessage(vm.busy, onChooseFolder) }
        return
    }
    when (section) {
        Section.MAP -> Unit
        Section.FOLDERS -> {
            val folders = report.folders.filter {
                rootFilter.isEmpty() || it.relativePath.startsWith(rootFilter + "/")
            }.sortedByDescending { it.sizeBytes }
            val maxBytes = folders.maxOfOrNull { it.sizeBytes } ?: 0L
            LazyColumn(
                modifier, contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Text(stringResource(R.string.folders_title), fontWeight = FontWeight.SemiBold) }
                items(folders, key = { it.relativePath }) { folder ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { onSelectFolder(folder.relativePath) },
                        color = TreemivoSurface, shape = RoundedCornerShape(13.dp)
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Folder, null, tint = Color(0xFFF5C44B))
                                Spacer(Modifier.width(8.dp))
                                Text(folder.name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(formatBytes(folder.sizeBytes), color = TreemivoMuted, fontSize = 12.sp)
                            }
                            LinearProgressIndicator(
                                progress = { if (maxBytes > 0L) folder.sizeBytes.toFloat() / maxBytes else 0f },
                                modifier = Modifier.fillMaxWidth().height(5.dp),
                                color = TreemivoAccent, trackColor = TreemivoPanel
                            )
                            Text(folder.fileCount.toString() + " " + stringResource(R.string.files_count).lowercase(),
                                color = TreemivoMuted, fontSize = 11.sp)
                        }
                    }
                }
                if (folders.isEmpty()) item { Text(stringResource(R.string.no_files), color = TreemivoMuted) }
            }
        }
        Section.TYPES -> {
            var chosenKind by remember { mutableStateOf<FileKind?>(null) }
            val totals = files.groupBy { it.kind }.mapValues { (_, ff) -> ff.sumOf { it.sizeBytes } }
            LazyColumn(
                modifier, contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.types_title), Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        if (chosenKind != null) TextButton(onClick = { chosenKind = null }) {
                            Text(stringResource(R.string.all_files))
                        }
                    }
                }
                if (chosenKind == null) {
                    items(FileKind.entries.toList()) { kind ->
                        val total = totals[kind] ?: 0L
                        Row(
                            Modifier.fillMaxWidth().background(TreemivoSurface, RoundedCornerShape(13.dp))
                                .clickable { chosenKind = kind }.padding(13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(16.dp).background(kind.tileColor(), RoundedCornerShape(4.dp)))
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(kind.labelId()), Modifier.weight(1f))
                            Text(formatBytes(total), color = TreemivoMuted)
                        }
                    }
                } else {
                    items(files.filter { it.kind == chosenKind }.sortedByDescending { it.sizeBytes }, key = { it.uri.toString() }) {
                        FileRow(it) { onSelectFile(it) }
                    }
                }
            }
        }
        Section.DUPLICATES -> {
            LazyColumn(
                modifier, contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(stringResource(R.string.duplicates_title), fontWeight = FontWeight.SemiBold)
                }
                item {
                    if (vm.checkingDuplicates) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(R.string.finding_duplicates), color = TreemivoMuted)
                        }
                    } else Button(
                        onClick = vm::verifyDuplicates,
                        enabled = !vm.busy
                    ) { Text(stringResource(R.string.find_duplicates)) }
                }
                if (vm.duplicatesChecked) {
                    item {
                        Text(
                            stringResource(R.string.duplicate_savings) + ": " +
                                formatBytes(vm.duplicates.sumOf { it.reclaimableBytes }),
                            fontWeight = FontWeight.SemiBold,
                            color = TreemivoAccent
                        )
                    }
                    if (vm.duplicates.isEmpty()) {
                        item { Text(stringResource(R.string.no_duplicates), color = TreemivoMuted) }
                    }
                    vm.duplicates.forEachIndexed { index, group ->
                        item { Text((index + 1).toString() + ". " + formatBytes(group.reclaimableBytes), color = TreemivoMuted) }
                        items(group.files, key = { it.uri.toString() }) { file ->
                            FileRow(file) { onSelectFile(file) }
                        }
                    }
                } else {
                    item { Text(stringResource(R.string.privacy_details), color = TreemivoMuted, fontSize = 13.sp) }
                }
            }
        }
    }
}

@Composable
private fun FileRow(file: FileRecord, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(TreemivoSurface, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(12.dp).background(file.kind.tileColor(), RoundedCornerShape(3.dp)))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
            Text(file.parentPath.ifBlank { "/" }, color = TreemivoMuted, fontSize = 11.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(6.dp))
        Text(formatBytes(file.sizeBytes), color = TreemivoMuted, fontSize = 12.sp)
    }
}

private fun FileKind.labelId(): Int = when (this) {
    FileKind.IMAGE -> R.string.category_images
    FileKind.VIDEO -> R.string.category_videos
    FileKind.AUDIO -> R.string.category_audio
    FileKind.DOCUMENT -> R.string.category_documents
    FileKind.ARCHIVE -> R.string.category_archives
    FileKind.INSTALLER -> R.string.category_apps
    FileKind.OTHER -> R.string.category_other
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return bytes.toString() + " B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = -1
    while (value >= 1024.0 && index < units.lastIndex) {
        value /= 1024.0
        index++
    }
    return String.format(Locale.getDefault(), "%.1f %s", value, units[index])
}

private fun openDocument(context: Context, file: FileRecord) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(file.uri, file.mimeType.ifBlank { "*/*" })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null))
    } catch (_: ActivityNotFoundException) {
        // No installed application supports the selected file type.
    }
}

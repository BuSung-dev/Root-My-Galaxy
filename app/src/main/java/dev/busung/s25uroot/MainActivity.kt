package dev.busung.s25uroot

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.busung.s25uroot.ui.theme.RootMyGalaxyTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val installViewModel by viewModels<InstallViewModel>()
    private var resumedOnce = false
    private var accentColor by mutableStateOf(AccentColor.Dynamic)
    private var themeMode by mutableStateOf(AppThemeMode.System)
    private var advancedMode by mutableStateOf(false)
    private var shizukuMode by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
        accentColor = AppPreferences.accentColor(this)
        themeMode = AppPreferences.themeMode(this)
        advancedMode = AppPreferences.advancedMode(this)
        shizukuMode = AppPreferences.shizukuMode(this)
        setContent {
            RootMyGalaxyTheme(accentColor = accentColor, themeMode = themeMode) {
                RootApp(
                    installViewModel = installViewModel,
                    accentColor = accentColor,
                    themeMode = themeMode,
                    advancedMode = advancedMode,
                    shizukuMode = shizukuMode,
                    onAccentColorChanged = { color ->
                        AppPreferences.setAccentColor(this, color)
                        accentColor = color
                    },
                    onThemeModeChanged = { mode ->
                        AppPreferences.setThemeMode(this, mode)
                        themeMode = mode
                    },
                    onAdvancedModeChanged = { enabled ->
                        AppPreferences.setAdvancedMode(this, enabled)
                        advancedMode = enabled
                    },
                    onShizukuModeChanged = { enabled ->
                        AppPreferences.setShizukuMode(this, enabled)
                        shizukuMode = enabled
                    },
                    openInstaller = { profileId ->
                        val installer = Intent(this, InstallActivity::class.java)
                            .putExtra(InstallActivity.EXTRA_INSTALL_REQUEST_ID, UUID.randomUUID().toString())
                        if (profileId != null) {
                            installer.putExtra(InstallActivity.EXTRA_PROFILE_ID, profileId)
                        }
                        startActivity(installer)
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (resumedOnce) installViewModel.refresh() else resumedOnce = true
    }
}

private enum class AppPage(@StringRes val label: Int, val icon: ImageVector) {
    Overview(R.string.nav_overview, Icons.Rounded.Home),
    History(R.string.nav_history, Icons.Rounded.History),
    Settings(R.string.nav_settings, Icons.Rounded.Settings),
}

private data class LanguageOption(@StringRes val label: Int, val tag: String)

private enum class CompatibilityWarning {
    Device,
    KernelVersion,
}

private val languageOptions = listOf(
    LanguageOption(R.string.language_system, ""),
    LanguageOption(R.string.language_korean, "ko"),
    LanguageOption(R.string.language_english, "en"),
    LanguageOption(R.string.language_japanese, "ja"),
    LanguageOption(R.string.language_chinese, "zh-CN"),
    LanguageOption(R.string.language_chinese_traditional, "zh-TW"),
    LanguageOption(R.string.language_turkish, "tr"),
    LanguageOption(R.string.language_brazillian_portuguese, "pt-BR"),
    LanguageOption(R.string.language_russian, "ru"),
    LanguageOption(R.string.language_vietnamese, "vi"),
    LanguageOption(R.string.language_uzbek, "uz"),
)

private const val KERNEL_SU_MANAGER_URL =
    "https://github.com/tiann/KernelSU/releases/download/v3.2.5/KernelSU_v3.2.5_32525-release.apk"
private const val KERNEL_SU_MANAGER_PACKAGE = "me.weishu.kernelsu"
private const val KERNEL_SU_HOME_URL = "https://kernelsu.org/"
private const val SHIZUKU_MANAGER_PACKAGE = "moe.shizuku.manager"
private const val SHIZUKU_MANAGER_URL = "https://github.com/thedjchi/Shizuku/releases/"

private fun isKernelSuManagerInstalled(context: Context): Boolean =
    context.packageManager.getLaunchIntentForPackage(KERNEL_SU_MANAGER_PACKAGE) != null

private fun openKernelSuManager(context: Context) {
    val launch = context.packageManager.getLaunchIntentForPackage(KERNEL_SU_MANAGER_PACKAGE)
    if (launch != null) {
        context.startActivity(launch)
    } else {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(KERNEL_SU_MANAGER_URL)))
    }
}

private fun openShizukuManager(context: Context) {
    val launch = context.packageManager.getLaunchIntentForPackage(SHIZUKU_MANAGER_PACKAGE)
    if (launch != null) {
        context.startActivity(launch)
    } else {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SHIZUKU_MANAGER_URL)))
    }
}

@Composable
private fun RootApp(
    installViewModel: InstallViewModel,
    accentColor: AccentColor,
    themeMode: AppThemeMode,
    advancedMode: Boolean,
    shizukuMode: Boolean,
    onAccentColorChanged: (AccentColor) -> Unit,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    onAdvancedModeChanged: (Boolean) -> Unit,
    onShizukuModeChanged: (Boolean) -> Unit,
    openInstaller: (String?) -> Unit,
) {
    val installState by installViewModel.state.collectAsStateWithLifecycle()
    val history by installViewModel.history.collectAsStateWithLifecycle()
    val targetCatalog by installViewModel.targetCatalog.collectAsStateWithLifecycle()
    var selectedPage by remember { mutableStateOf(AppPage.Overview) }
    var showInstallConfirmation by remember { mutableStateOf(false) }
    var showTargetPicker by remember { mutableStateOf(false) }
    var selectedProfile by remember { mutableStateOf<TargetProfile?>(null) }
    var compatibilityWarning by remember { mutableStateOf<CompatibilityWarning?>(null) }
    val device = remember { DeviceSnapshot.current() }
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var updateStatus by remember { mutableStateOf<UpdateStatus>(UpdateStatus.Idle) }
    var updateCardDismissed by remember { mutableStateOf(false) }
    val checkForUpdate: () -> Unit = {
        if (!updateStatus.busy) {
            updateStatus = UpdateStatus.Checking
            scope.launch {
                val info = AppUpdater.fetchLatestRelease()
                updateStatus = when {
                    info == null -> UpdateStatus.Failed
                    AppUpdater.isUpdateAvailable(info.versionName, BuildConfig.VERSION_NAME) ->
                        UpdateStatus.Available(info)
                    else -> UpdateStatus.UpToDate
                }
            }
        }
    }
    val startDownload: (UpdateInfo) -> Unit = { info ->
        val apkUrl = info.apkUrl
        if (apkUrl == null) {
            AppUpdater.openReleasesPage(context)
        } else {
            updateStatus = UpdateStatus.Downloading(info, 0f)
            scope.launch {
                val apk = AppUpdater.downloadApk(context, apkUrl) { progress ->
                    updateStatus = UpdateStatus.Downloading(info, progress)
                }
                if (apk == null || !AppUpdater.installApk(context, apk)) {
                    Toast.makeText(context, context.getString(R.string.updater_download_failed), Toast.LENGTH_SHORT).show()
                    AppUpdater.openReleasesPage(context)
                }
                updateStatus = UpdateStatus.Available(info)
            }
        }
    }
    LaunchedEffect(Unit) { checkForUpdate() }

    if (showTargetPicker) {
        TargetSelectionSheet(
            device = device,
            catalog = targetCatalog,
            onDismiss = { showTargetPicker = false },
            onRetry = installViewModel::loadTargetCatalog,
            onNext = { profile ->
                selectedProfile = profile
                showTargetPicker = false
                compatibilityWarning = when {
                    !profile.matchesDevice(device) -> CompatibilityWarning.Device
                    !profile.matchesKernelVersion(device) -> CompatibilityWarning.KernelVersion
                    else -> null
                }
                if (compatibilityWarning == null) showInstallConfirmation = true
            },
        )
    }

    compatibilityWarning?.let { warning ->
        val profile = selectedProfile ?: return@let
        AlertDialog(
            onDismissRequest = {
                compatibilityWarning = null
                showTargetPicker = true
            },
            icon = { Icon(Icons.Rounded.Warning, contentDescription = null) },
            title = {
                DialogDimAmount(0.34f)
                Text(
                    stringResource(when (warning) {
                        CompatibilityWarning.Device -> R.string.device_mismatch_title
                        CompatibilityWarning.KernelVersion -> R.string.kernel_version_mismatch_title
                    }),
                )
            },
            text = {
                Text(
                    when (warning) {
                        CompatibilityWarning.Device -> stringResource(
                            R.string.device_mismatch_body,
                            device.model,
                            profile.supportedModels,
                        )
                        CompatibilityWarning.KernelVersion -> stringResource(
                            R.string.kernel_version_mismatch_body,
                            device.kernelVersion,
                            profile.supportedKernelVersions,
                        )
                    },
                )
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        clickHaptic(view)
                        compatibilityWarning = when (warning) {
                            CompatibilityWarning.Device -> if (!profile.matchesKernelVersion(device)) {
                                CompatibilityWarning.KernelVersion
                            } else {
                                null
                            }
                            CompatibilityWarning.KernelVersion -> null
                        }
                        if (compatibilityWarning == null) {
                            showInstallConfirmation = true
                        }
                    },
                ) {
                    Text(stringResource(R.string.action_continue))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        clickHaptic(view)
                        compatibilityWarning = null
                        showTargetPicker = true
                    },
                ) {
                    Text(stringResource(R.string.action_back))
                }
            },
        )
    }

    if (showInstallConfirmation) {
        AlertDialog(
            onDismissRequest = { showInstallConfirmation = false },
            icon = { Icon(Icons.Rounded.Security, contentDescription = null) },
            title = {
                DialogDimAmount(0.34f)
                Text(stringResource(R.string.install_confirm_title))
            },
            text = { Text(stringResource(R.string.install_confirm_body)) },
            confirmButton = {
                FilledTonalButton(onClick = {
                    clickHaptic(view)
                    showInstallConfirmation = false
                    openInstaller(selectedProfile?.profileId)
                    selectedProfile = null
                }) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    clickHaptic(view)
                    showInstallConfirmation = false
                }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp,
            ) {
                AppPage.entries.forEach { page ->
                    NavigationBarItem(
                        selected = selectedPage == page,
                        onClick = {
                            clickHaptic(view)
                            selectedPage = page
                        },
                        modifier = Modifier.padding(top = 4.dp),
                        icon = { Icon(page.icon, contentDescription = null) },
                        label = { Text(stringResource(page.label)) },
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { padding ->
        AnimatedContent(targetState = selectedPage, label = "page") { page ->
            when (page) {
                AppPage.Overview -> OverviewPage(
                    padding = padding,
                    device = device,
                    installState = installState,
                    updateStatus = updateStatus,
                    updateCardDismissed = updateCardDismissed,
                    onDismissUpdateCard = { updateCardDismissed = true },
                    onStartDownload = startDownload,
                    onInstall = {
                        selectedProfile = null
                        if (advancedMode) {
                            showTargetPicker = true
                            installViewModel.loadTargetCatalog()
                        } else {
                            showInstallConfirmation = true
                        }
                    },
                )
                AppPage.History -> HistoryPage(
                    padding,
                    history,
                    onDeleteEntries = installViewModel::deleteHistoryEntries,
                )
                AppPage.Settings -> SettingsPage(
                    padding = padding,
                    accentColor = accentColor,
                    themeMode = themeMode,
                    advancedMode = advancedMode,
                    shizukuMode = shizukuMode,
                    updateStatus = updateStatus,
                    onCheckForUpdate = checkForUpdate,
                    onStartDownload = startDownload,
                    onAccentColorChanged = onAccentColorChanged,
                    onThemeModeChanged = onThemeModeChanged,
                    onAdvancedModeChanged = onAdvancedModeChanged,
                    onShizukuModeChanged = onShizukuModeChanged,
                )
            }
        }
    }
}

@Composable
private fun AppVersionText(
    style: TextStyle,
    color: Color,
) {
    Text(
        text = stringResource(
            R.string.version_format,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE,
        ),
        style = style,
        color = color,
    )
}

private fun clickHaptic(view: View) {
    view.performHapticFeedback(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.LONG_PRESS
        },
    )
}

@Composable
private fun DialogDimAmount(amount: Float) {
    val window = (LocalView.current.parent as DialogWindowProvider).window
    SideEffect { window.setDimAmount(amount) }
}

@Composable
private fun OverviewPage(
    padding: PaddingValues,
    device: DeviceSnapshot,
    installState: InstallUiState,
    updateStatus: UpdateStatus,
    updateCardDismissed: Boolean,
    onDismissUpdateCard: () -> Unit,
    onStartDownload: (UpdateInfo) -> Unit,
    onInstall: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 54.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_app_logo),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                )
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                )
                Spacer(modifier = Modifier.weight(1f))
                AppVersionText(
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                )
            }
        }
        if (
            !updateCardDismissed &&
            updateStatus.info != null
        ) {
            item {
                UpdateCard(
                    status = updateStatus,
                    onDismiss = onDismissUpdateCard,
                    onStartDownload = onStartDownload,
                )
            }
        }
        item { InstallStatusCard(installState, onInstall) }
        item { DeviceCard(device) }
        item { HowItWorksCard() }
    }
}

private sealed interface UpdateStatus {
    data object Idle : UpdateStatus
    data object Checking : UpdateStatus
    data class Available(val info: UpdateInfo) : UpdateStatus
    data class Downloading(val info: UpdateInfo, val progress: Float) : UpdateStatus
    data object UpToDate : UpdateStatus
    data object Failed : UpdateStatus
}

private val UpdateStatus.busy: Boolean
    get() = this is UpdateStatus.Checking || this is UpdateStatus.Downloading

private val UpdateStatus.info: UpdateInfo?
    get() = when (this) {
        is UpdateStatus.Available -> this.info
        is UpdateStatus.Downloading -> this.info
        else -> null
    }

@Composable
private fun UpdateCard(
    status: UpdateStatus,
    onDismiss: () -> Unit,
    onStartDownload: (UpdateInfo) -> Unit,
) {
    val view = LocalView.current
    val info = status.info
    if (info == null) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    Icons.Rounded.SystemUpdate,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = stringResource(R.string.updater_available_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        clickHaptic(view)
                        onDismiss()
                    },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.action_close),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Text(
                text = stringResource(R.string.updater_available_body, info.versionName),
                style = MaterialTheme.typography.bodyMedium,
            )
            when (status) {
                is UpdateStatus.Downloading -> {
                    LinearProgressIndicator(
                        progress = { status.progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = LocalContentColor.current,
                        trackColor = LocalContentColor.current.copy(alpha = 0.2f),
                        drawStopIndicator = {},
                    )
                    Text(
                        text = stringResource(R.string.updater_downloading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalContentColor.current.copy(alpha = 0.78f),
                    )
                }
                else -> {
                    FilledTonalButton(onClick = {
                        clickHaptic(view)
                        onStartDownload(info)
                    }) {
                        Text(stringResource(R.string.updater_button_download))
                    }
                }
            }
        }
    }
}

@Composable
private fun HowItWorksCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.how_it_works), style = MaterialTheme.typography.titleMedium)
            installerSteps.forEach { step ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(step.icon, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(step.title), style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(step.detail),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InstallStatusCard(installState: InstallUiState, onInstall: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val uriHandler = LocalUriHandler.current
    val managerInstalled = remember(installState) { isKernelSuManagerInstalled(context) }
    Card(
        onClick = {
            clickHaptic(view)
            when {
                installState.busy -> Unit
                installState.phase == InstallPhase.Installed -> {
                    if (managerInstalled) {
                        openKernelSuManager(context)
                    } else {
                        uriHandler.openUri(KERNEL_SU_MANAGER_URL)
                    }
                }
                else -> onInstall()
            }
        },
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = expressiveClickableCardShape(interactionSource),
        interactionSource = interactionSource,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when {
                installState.busy -> LoadingIndicator(
                    modifier = Modifier.size(44.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                installState.phase == InstallPhase.Installed -> Icon(
                    Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(44.dp),
                )
                installState.phase == InstallPhase.Failed -> Icon(
                    Icons.Rounded.Warning, contentDescription = null, modifier = Modifier.size(44.dp),
                )
                else -> Icon(
                    Icons.Rounded.Warning, contentDescription = null, modifier = Modifier.size(44.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                if (installState.phase == InstallPhase.Installed) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_kernelsu),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = stringResource(R.string.status_ksu_active),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                } else {
                    Text(
                        text = when (installState.phase) {
                            InstallPhase.Ready -> stringResource(R.string.status_not_installed)
                            else -> installState.message
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Text(
                    text = when (installState.phase) {
                        InstallPhase.Installed -> stringResource(
                            if (managerInstalled) {
                                R.string.install_tap_open_manager
                            } else {
                                R.string.install_tap_manager
                            },
                        )
                        InstallPhase.Failed -> stringResource(R.string.install_tap_retry)
                        else -> stringResource(R.string.install_tap_start)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.86f),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun DeviceCard(device: DeviceSnapshot) {
    val view = LocalView.current
    var kernelExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            InfoRow(Icons.Rounded.Memory, stringResource(R.string.device), "${device.manufacturer} ${device.model} (${device.device})")
            InfoRow(Icons.Rounded.Code, stringResource(R.string.firmware), device.buildId)
            InfoRow(Icons.Rounded.Info, stringResource(R.string.system), "Android ${device.androidRelease} (API ${device.sdk})")
            InfoRow(
                icon = Icons.Rounded.Info,
                label = stringResource(R.string.kernel),
                value = if (kernelExpanded) device.kernelVersionFull else device.kernelRelease,
                onClick = {
                    clickHaptic(view)
                    kernelExpanded = !kernelExpanded
                },
            )
            InfoRow(Icons.Rounded.Security, stringResource(R.string.system_abi), "${device.abi} (${device.pageSize / 1024}K)")
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = if (onClick != null) {
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClick = onClick)
        } else {
            Modifier
        },
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HistoryPage(
    padding: PaddingValues,
    history: List<InstallHistoryEntry>,
    onDeleteEntries: (Set<String>) -> Unit,
) {
    val view = LocalView.current
    var selectedHistoryId by remember { mutableStateOf<String?>(null) }
    var selectionIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingDeleteIds by remember { mutableStateOf<Set<String>?>(null) }
    val selectedEntry = history.firstOrNull { it.id == selectedHistoryId }
    val selectableIds = history
        .filter { it.result != InstallRunResult.Running }
        .map { it.id }
        .toSet()
    val selecting = selectionIds.isNotEmpty()
    BackHandler(enabled = selectedEntry != null || selecting) {
        if (selecting) {
            selectionIds = emptySet()
        } else {
            selectedHistoryId = null
        }
    }

    pendingDeleteIds?.let { ids ->
        AlertDialog(
            onDismissRequest = { pendingDeleteIds = null },
            icon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
            title = {
                DialogDimAmount(0.34f)
                Text(pluralStringResource(R.plurals.history_delete_selected_title, ids.size, ids.size))
            },
            text = { Text(pluralStringResource(R.plurals.history_delete_selected_body, ids.size, ids.size)) },
            confirmButton = {
                FilledTonalButton(onClick = {
                    clickHaptic(view)
                    onDeleteEntries(ids)
                    selectionIds = emptySet()
                    pendingDeleteIds = null
                }) {
                    Text(stringResource(R.string.history_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    clickHaptic(view)
                    pendingDeleteIds = null
                }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    AnimatedContent(
        targetState = selectedEntry,
        contentKey = { it?.id ?: "history-list" },
        label = "history-detail",
    ) { entry ->
        if (entry == null) {
            HistoryList(
                padding = padding,
                history = history,
                selectionIds = selectionIds,
                selectableIds = selectableIds,
                onToggleSelection = { id ->
                    selectionIds = if (id in selectionIds) {
                        selectionIds - id
                    } else {
                        selectionIds + id
                    }
                },
                onSelectAll = {
                    selectionIds = if (selectionIds.size == selectableIds.size) {
                        emptySet()
                    } else {
                        selectableIds
                    }
                },
                onClearSelection = { selectionIds = emptySet() },
                onEntryClick = { selectedHistoryId = it.id },
                onDeleteSelected = { pendingDeleteIds = selectionIds },
            )
        } else {
            HistoryDetail(
                padding = padding,
                entry = entry,
                onBack = { selectedHistoryId = null },
            )
        }
    }
}

@Composable
private fun HistoryList(
    padding: PaddingValues,
    history: List<InstallHistoryEntry>,
    selectionIds: Set<String>,
    selectableIds: Set<String>,
    onToggleSelection: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onEntryClick: (InstallHistoryEntry) -> Unit,
    onDeleteSelected: () -> Unit,
) {
    val view = LocalView.current
    val selecting = selectionIds.isNotEmpty()
    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = 20.dp,
                end = 20.dp,
                bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.weight(1f).height(48.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            text = stringResource(R.string.history_title),
                            style = MaterialTheme.typography.headlineLarge,
                        )
                    }
                    AnimatedVisibility(
                        visible = selecting,
                        enter = fadeIn() + scaleIn(initialScale = 0.9f),
                        exit = fadeOut() + scaleOut(targetScale = 0.9f),
                    ) {
                        Row {
                            IconButton(onClick = {
                                clickHaptic(view)
                                onSelectAll()
                            }) {
                                Icon(
                                    Icons.Rounded.SelectAll,
                                    contentDescription = stringResource(R.string.history_select_all),
                                )
                            }
                            IconButton(onClick = {
                                clickHaptic(view)
                                onClearSelection()
                            }) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.history_clear_selection),
                                )
                            }
                        }
                    }
                }
            }
            if (history.isEmpty()) {
                item { EmptyHistoryCard() }
            } else {
                itemsIndexed(history, key = { _, entry -> entry.id }) { _, entry ->
                    HistoryEntryCard(
                        entry = entry,
                        selectionMode = selecting,
                        isSelected = entry.id in selectionIds,
                        selectable = entry.id in selectableIds,
                        onClick = {
                            if (selecting) {
                                onToggleSelection(entry.id)
                            } else {
                                onEntryClick(entry)
                            }
                        },
                        onLongClick = {
                            if (entry.id in selectableIds) onToggleSelection(entry.id)
                        },
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = selecting,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut(targetScale = 0.85f),
        ) {
            ExtendedFloatingActionButton(
                onClick = {
                    clickHaptic(view)
                    onDeleteSelected()
                },
                icon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                text = { Text(stringResource(R.string.history_delete_selected, selectionIds.size)) },
            )
        }
    }
}

@Composable
private fun EmptyHistoryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(32.dp))
            Column {
                Text(stringResource(R.string.history_empty_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.history_empty_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HistoryEntryCard(
    entry: InstallHistoryEntry,
    selectionMode: Boolean,
    isSelected: Boolean,
    selectable: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val shape = expressiveClickableCardShape(interactionSource)
    val containerColor = historyResultContainerColor(entry.result)
    val contentColor = historyResultContentColor(entry.result)
    val borderWidth by animateDpAsState(
        targetValue = if (selectionMode && isSelected) 2.dp else 0.dp,
        label = "history-card-border",
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .combinedClickable(
                interactionSource = interactionSource,
                onClick = {
                    clickHaptic(view)
                    onClick()
                },
                onLongClick = {
                    clickHaptic(view)
                    onLongClick()
                },
            ),
        shape = shape,
        border = if (borderWidth > 0.dp) {
            BorderStroke(borderWidth, MaterialTheme.colorScheme.secondary)
        } else {
            null
        },
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 15.dp)
                .animateContentSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Crossfade(
                targetState = selectionMode,
                label = "history-leading",
                modifier = Modifier.size(48.dp),
            ) { selecting ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (selecting) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = null,
                            enabled = selectable,
                        )
                    } else {
                        Icon(historyResultIcon(entry.result), contentDescription = null, modifier = Modifier.size(30.dp))
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(historyResultLabel(entry.result), style = MaterialTheme.typography.titleMedium)
                Text(
                    formatHistoryTime(entry.startedAtMillis),
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.78f),
                )
            }
            if (!selectionMode) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = null)
            }
        }
    }
}

@Composable
private fun HistoryDetail(
    padding: PaddingValues,
    entry: InstallHistoryEntry,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val exportLogLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        result.data?.data?.let { uri -> saveRunLog(context, uri, entry) }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.padding(top = 12.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(onClick = {
                    clickHaptic(view)
                    onBack()
                }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
                Text(
                    stringResource(R.string.history_detail_title),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = {
                    clickHaptic(view)
                    exportLogLauncher.launch(
                        Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TITLE, runLogFileName(entry))
                        },
                    )
                }) {
                    Icon(Icons.Rounded.Save, contentDescription = stringResource(R.string.export_log))
                }
            }
        }
        item { HistoryResultCard(entry) }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
            ) {
                Text(
                    text = entry.log.ifBlank { stringResource(R.string.history_log_empty) },
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun HistoryResultCard(entry: InstallHistoryEntry) {
    val containerColor = historyResultContainerColor(entry.result)
    val contentColor = historyResultContentColor(entry.result)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(historyResultIcon(entry.result), contentDescription = null, modifier = Modifier.size(38.dp))
            Column {
                Text(historyResultLabel(entry.result), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.history_started, formatHistoryTime(entry.startedAtMillis)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.78f),
                )
                entry.completedAtMillis?.let { completedAt ->
                    Text(
                        stringResource(R.string.history_completed, formatHistoryTime(completedAt)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.78f),
                    )
                }
                entry.profileId?.let { profileId ->
                    Text(
                        stringResource(R.string.history_payload, profileId),
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.78f),
                    )
                }
                Text(
                    stringResource(
                        if (entry.usedShizuku) {
                            R.string.history_shizuku_used
                        } else {
                            R.string.history_shizuku_not_used
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.78f),
                )
            }
        }
    }
}

@Composable
private fun historyResultLabel(result: InstallRunResult): String = stringResource(
    when (result) {
        InstallRunResult.Running -> R.string.history_running
        InstallRunResult.Succeeded -> R.string.history_succeeded
        InstallRunResult.Failed -> R.string.history_failed
    },
)

private fun historyResultIcon(result: InstallRunResult): ImageVector = when (result) {
    InstallRunResult.Running -> Icons.Rounded.Schedule
    InstallRunResult.Succeeded -> Icons.Rounded.CheckCircle
    InstallRunResult.Failed -> Icons.Rounded.Error
}

@Composable
private fun historyResultContainerColor(result: InstallRunResult): Color = when (result) {
    InstallRunResult.Running -> MaterialTheme.colorScheme.tertiaryContainer
    InstallRunResult.Succeeded -> MaterialTheme.colorScheme.primaryContainer
    InstallRunResult.Failed -> MaterialTheme.colorScheme.errorContainer
}

@Composable
private fun historyResultContentColor(result: InstallRunResult): Color = when (result) {
    InstallRunResult.Running -> MaterialTheme.colorScheme.onTertiaryContainer
    InstallRunResult.Succeeded -> MaterialTheme.colorScheme.onPrimaryContainer
    InstallRunResult.Failed -> MaterialTheme.colorScheme.onErrorContainer
}

@Composable
private fun formatHistoryTime(timestamp: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(timestamp, locale) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale)
            .format(Date(timestamp))
    }
}

private fun runLogFileName(entry: InstallHistoryEntry): String =
    "RootMyGalaxy-" +
        SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(entry.startedAtMillis)) +
        "-${entry.result.name.lowercase(Locale.US)}.log"

private fun saveRunLog(context: Context, uri: Uri, entry: InstallHistoryEntry) {
    val content = entry.log.ifBlank { context.getString(R.string.history_log_empty) }
    val saved = runCatching {
        context.contentResolver.openOutputStream(uri)?.use { output ->
            output.write(content.toByteArray(Charsets.UTF_8))
        } ?: error("open failed")
        true
    }.getOrDefault(false)
    Toast.makeText(
        context,
        if (saved) {
            context.getString(R.string.export_log_saved)
        } else {
            context.getString(R.string.export_log_failed)
        },
        Toast.LENGTH_LONG,
    ).show()
}

@Composable
private fun SettingsPage(
    padding: PaddingValues,
    accentColor: AccentColor,
    themeMode: AppThemeMode,
    advancedMode: Boolean,
    shizukuMode: Boolean,
    updateStatus: UpdateStatus,
    onCheckForUpdate: () -> Unit,
    onStartDownload: (UpdateInfo) -> Unit,
    onAccentColorChanged: (AccentColor) -> Unit,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    onAdvancedModeChanged: (Boolean) -> Unit,
    onShizukuModeChanged: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showShizukuMissingDialog by remember { mutableStateOf(false) }
    var languageMenuTop by remember { mutableStateOf(32.dp) }
    var colorMenuTop by remember { mutableStateOf(32.dp) }
    val density = LocalDensity.current
    val currentLanguageTag = AppPreferences.languageTag(context)

    if (showShizukuMissingDialog) {
        AlertDialog(
            onDismissRequest = { showShizukuMissingDialog = false },
            icon = { Icon(Icons.Rounded.Info, contentDescription = null) },
            title = {
                DialogDimAmount(0.34f)
                Text(stringResource(R.string.shizuku_not_running_title))
            },
            text = { Text(stringResource(R.string.shizuku_not_running_body)) },
            confirmButton = {
                FilledTonalButton(onClick = {
                    clickHaptic(view)
                    showShizukuMissingDialog = false
                    openShizukuManager(context)
                }) {
                    Text(stringResource(R.string.action_download_shizuku))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    clickHaptic(view)
                    showShizukuMissingDialog = false
                }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showLanguageDialog) {
        SideChoiceMenu(
            choices = languageOptions.map { stringResource(it.label) },
            selectedIndex = languageOptions.indexOfFirst { languageMatches(it, currentLanguageTag) }
                .coerceAtLeast(0),
            topOffset = languageMenuTop,
            onSelected = { index ->
                showLanguageDialog = false
                AppPreferences.setLanguage(context, languageOptions[index].tag)
            },
            onDismiss = { showLanguageDialog = false },
        )
    }

    if (showColorDialog) {
        val colors = AccentColor.entries
        SideChoiceMenu(
            choices = colors.map { accentLabel(it) },
            selectedIndex = colors.indexOf(accentColor),
            topOffset = colorMenuTop,
            onSelected = { index ->
                showColorDialog = false
                onAccentColorChanged(colors[index])
            },
            onDismiss = { showColorDialog = false },
        )
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(top = 20.dp, bottom = 18.dp)) {
                Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineLarge)
                AppVersionText(
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { SectionLabel(stringResource(R.string.appearance)) }
        item {
            ThemeModeSelector(themeMode, onThemeModeChanged)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SettingsCard(
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        colorMenuTop = with(density) { coordinates.positionInWindow().y.toDp() }
                    },
                    icon = Icons.Rounded.Palette,
                    title = stringResource(R.string.material_color),
                    description = stringResource(R.string.material_color_description),
                    value = accentLabel(accentColor),
                    position = SettingsCardPosition.Top,
                    onClick = {
                        clickHaptic(view)
                        showColorDialog = true
                    },
                )
                SettingsCard(
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        languageMenuTop = with(density) { coordinates.positionInWindow().y.toDp() }
                    },
                    icon = Icons.Rounded.Language,
                    title = stringResource(R.string.language),
                    description = stringResource(R.string.language_description),
                    value = languageLabel(currentLanguageTag),
                    position = SettingsCardPosition.Middle,
                    onClick = {
                        clickHaptic(view)
                        showLanguageDialog = true
                    },
                )
                SettingsSwitchCard(
                    icon = Icons.Rounded.VerifiedUser,
                    title = stringResource(R.string.shizuku_mode),
                    description = stringResource(R.string.shizuku_mode_description),
                    checked = shizukuMode,
                    position = SettingsCardPosition.Bottom,
                    onCheckedChange = { enabled ->
                        clickHaptic(view)
                        if (!enabled) {
                            onShizukuModeChanged(false)
                        } else {
                            scope.launch {
                                ShizukuController.pingUntilRunning()
                                if (ShizukuController.isRunning()) {
                                    onShizukuModeChanged(true)
                                    if (!ShizukuController.isGranted()) {
                                        ShizukuController.requestPermission()
                                    }
                                } else {
                                    showShizukuMissingDialog = true
                                }
                            }
                        }
                    },
                )
            }
        }
        item { SectionLabel(stringResource(R.string.advanced)) }
        item {
            SettingsSwitchCard(
                icon = Icons.Rounded.Memory,
                title = stringResource(R.string.advanced_mode),
                description = stringResource(R.string.advanced_mode_description),
                checked = advancedMode,
                onCheckedChange = {
                    clickHaptic(view)
                    onAdvancedModeChanged(it)
                },
            )
        }
        item { SectionLabel(stringResource(R.string.about)) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                UpdateSettingsCard(
                    status = updateStatus,
                    position = SettingsCardPosition.Top,
                    onCheckForUpdate = onCheckForUpdate,
                    onStartDownload = onStartDownload,
                )
                SettingsCard(
                    icon = Icons.Rounded.Info,
                    title = stringResource(R.string.about),
                    description = stringResource(R.string.about_description),
                    value = "",
                    position = SettingsCardPosition.Bottom,
                    onClick = {
                        clickHaptic(view)
                        showAboutDialog = true
                    },
                )
            }
        }
    }
}

@Composable
private fun UpdateSettingsCard(
    status: UpdateStatus,
    position: SettingsCardPosition,
    onCheckForUpdate: () -> Unit,
    onStartDownload: (UpdateInfo) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val view = LocalView.current
    val busy = status.busy
    Card(
        onClick = {
            clickHaptic(view)
            when {
                busy -> Unit
                status is UpdateStatus.Available -> onStartDownload(status.info)
                else -> onCheckForUpdate()
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = expressiveClickableCardShape(interactionSource, position),
        interactionSource = interactionSource,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                status is UpdateStatus.Checking -> LoadingIndicator(modifier = Modifier.size(28.dp))
                status is UpdateStatus.Downloading -> CircularProgressIndicator(
                    progress = { status.progress },
                    modifier = Modifier.size(28.dp),
                )
                else -> Icon(
                    Icons.Rounded.SystemUpdate,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (status) {
                        is UpdateStatus.Available, is UpdateStatus.Downloading ->
                            stringResource(R.string.updater_available_title)
                        else -> stringResource(R.string.updater_check)
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = when {
                        status is UpdateStatus.Downloading -> stringResource(R.string.updater_downloading)
                        status is UpdateStatus.Checking -> stringResource(R.string.updater_checking)
                        status is UpdateStatus.Available ->
                            stringResource(R.string.updater_available_body_short, status.info.versionName)
                        status is UpdateStatus.UpToDate -> stringResource(R.string.updater_up_to_date)
                        status is UpdateStatus.Failed -> stringResource(R.string.updater_failed)
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (status is UpdateStatus.Available) {
                Text(
                    text = stringResource(R.string.updater_button_download),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TargetSelectionSheet(
    device: DeviceSnapshot,
    catalog: TargetCatalogUiState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onNext: (TargetProfile) -> Unit,
) {
    var showOnlyMyDevice by remember { mutableStateOf(true) }
    var selectedProfileId by remember { mutableStateOf<String?>(null) }
    val view = LocalView.current
    val visibleProfiles = remember(catalog.profiles, showOnlyMyDevice, device) {
        if (showOnlyMyDevice) {
            catalog.profiles.filter { it.matches(device) }
        } else {
            catalog.profiles
        }
    }
    val selectedProfile = catalog.profiles.firstOrNull { it.profileId == selectedProfileId }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.select_device_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    stringResource(R.string.select_device_description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = showOnlyMyDevice,
                        role = Role.Checkbox,
                        onValueChange = { enabled ->
                            clickHaptic(view)
                            showOnlyMyDevice = enabled
                            if (enabled && selectedProfile?.matches(device) == false) {
                                selectedProfileId = null
                            }
                        },
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Checkbox(checked = showOnlyMyDevice, onCheckedChange = null)
                Text(stringResource(R.string.show_my_device_only), style = MaterialTheme.typography.titleMedium)
            }

            when {
                catalog.loading -> Box(
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingIndicator(color = MaterialTheme.colorScheme.onSurface)
                }
                catalog.error != null -> Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(catalog.error, color = MaterialTheme.colorScheme.error)
                    FilledTonalButton(onClick = onRetry) {
                        Text(stringResource(R.string.action_retry))
                    }
                }
                visibleProfiles.isEmpty() -> Text(
                    stringResource(R.string.no_matching_devices),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(visibleProfiles, key = TargetProfile::profileId) { profile ->
                        val selected = selectedProfileId == profile.profileId
                        val matchingModel = profile.models.firstOrNull {
                            it.equals(device.model, ignoreCase = true)
                        }
                        val modelLabel = matchingModel ?: profile.models.take(3).joinToString().let {
                            if (profile.models.size > 3) "$it +${profile.models.size - 3}" else it
                        }
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = selected,
                                        role = Role.RadioButton,
                                        onClick = {
                                            clickHaptic(view)
                                            selectedProfileId = profile.profileId
                                        },
                                    )
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                RadioButton(selected = selected, onClick = null)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        profile.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        modelLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(onClick = {
                    clickHaptic(view)
                    onDismiss()
                }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(
                    onClick = {
                        clickHaptic(view)
                        selectedProfile?.let(onNext)
                    },
                    enabled = selectedProfile != null,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.action_next))
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 18.dp, top = 6.dp, bottom = 2.dp),
    )
}

private enum class SettingsCardPosition {
    Single,
    GroupedSingle,
    Top,
    Middle,
    Bottom,
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    description: String,
    value: String,
    position: SettingsCardPosition = SettingsCardPosition.Single,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val view = LocalView.current
    Card(
        onClick = {
            clickHaptic(view)
            onClick()
        },
        modifier = modifier.fillMaxWidth(),
        shape = expressiveClickableCardShape(interactionSource, position),
        interactionSource = interactionSource,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SettingsSwitchCard(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    position: SettingsCardPosition = SettingsCardPosition.Single,
    onCheckedChange: (Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val view = LocalView.current
    Card(
        onClick = {
            clickHaptic(view)
            onCheckedChange(!checked)
        },
        modifier = Modifier.fillMaxWidth(),
        shape = expressiveClickableCardShape(interactionSource, position),
        interactionSource = interactionSource,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

@Composable
private fun ThemeModeSelector(
    themeMode: AppThemeMode,
    onThemeModeChanged: (AppThemeMode) -> Unit,
) {
    val view = LocalView.current
    val themeModes = AppThemeMode.entries
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        themeModes.forEachIndexed { index, mode ->
            ToggleButton(
                checked = themeMode == mode,
                onCheckedChange = {
                    clickHaptic(view)
                    onThemeModeChanged(mode)
                },
                modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                colors = ToggleButtonDefaults.toggleButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    themeModes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                contentPadding = PaddingValues(horizontal = 10.dp),
            ) {
                Icon(
                    imageVector = when (mode) {
                        AppThemeMode.System -> Icons.Rounded.BrightnessAuto
                        AppThemeMode.Light -> Icons.Rounded.LightMode
                        AppThemeMode.Dark -> Icons.Rounded.DarkMode
                    },
                    contentDescription = null,
                )
                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                Text(themeModeLabel(mode), maxLines = 1)
            }
        }
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val view = LocalView.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            DialogDimAmount(0.34f)
            Text(stringResource(R.string.about_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(stringResource(R.string.about_body))
                AppVersionText(
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider()
                Surface(
                    onClick = {
                        clickHaptic(view)
                        uriHandler.openUri(KERNEL_SU_HOME_URL)
                    },
                    color = Color.Transparent,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(painterResource(R.drawable.ic_kernelsu), contentDescription = null)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.kernelsu_card_title),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                stringResource(R.string.kernelsu_card_description),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(Icons.Rounded.Link, contentDescription = stringResource(R.string.open_github))
                    }
                }
                Surface(
                    onClick = {
                        clickHaptic(view)
                        uriHandler.openUri(ROOT_MY_GALAXY_URL)
                    },
                    color = Color.Transparent,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(painterResource(R.drawable.ic_github), contentDescription = null)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.github_card_title),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                stringResource(R.string.github_card_description),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(Icons.Rounded.Link, contentDescription = stringResource(R.string.open_github))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                clickHaptic(view)
                onDismiss()
            }) {
                Text(stringResource(R.string.action_close))
            }
        },
    )
}

@Composable
private fun expressiveClickableCardShape(
    interactionSource: MutableInteractionSource,
    position: SettingsCardPosition = SettingsCardPosition.Single,
): RoundedCornerShape {
    val pressed by interactionSource.collectIsPressedAsState()
    val topRadius by animateDpAsState(
        targetValue = when {
            pressed -> 28.dp
            position == SettingsCardPosition.Single -> 16.dp
            position in setOf(SettingsCardPosition.GroupedSingle, SettingsCardPosition.Top) -> 24.dp
            else -> 6.dp
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "clickable-card-top-corner",
    )
    val bottomRadius by animateDpAsState(
        targetValue = when {
            pressed -> 28.dp
            position == SettingsCardPosition.Single -> 16.dp
            position in setOf(SettingsCardPosition.GroupedSingle, SettingsCardPosition.Bottom) -> 24.dp
            else -> 6.dp
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "clickable-card-bottom-corner",
    )
    return RoundedCornerShape(
        topStart = topRadius,
        topEnd = topRadius,
        bottomStart = bottomRadius,
        bottomEnd = bottomRadius,
    )
}

@Composable
private fun SideChoiceMenu(
    choices: List<String>,
    selectedIndex: Int,
    topOffset: Dp,
    onSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current
    val scrimAlpha by animateFloatAsState(
        targetValue = if (visible) 0.34f else 0f,
        animationSpec = tween(durationMillis = if (visible) 160 else 180),
        label = "menu-scrim",
    )

    fun closeMenu(afterAnimation: () -> Unit) {
        if (closing) return
        closing = true
        visible = false
        coroutineScope.launch {
            delay(MENU_EXIT_WAIT_MILLIS)
            afterAnimation()
        }
    }

    LaunchedEffect(Unit) {
        visible = true
    }

    Popup(
        onDismissRequest = { closeMenu(onDismiss) },
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            clippingEnabled = false,
        ),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val estimatedHeight = 16.dp + 56.dp * choices.size
            val constrainedTop = minOf(
                topOffset,
                maxHeight - estimatedHeight - 24.dp,
            ).coerceAtLeast(16.dp)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { closeMenu(onDismiss) },
                    ),
            )
            AnimatedVisibility(
                visible = visible,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = constrainedTop, end = 18.dp),
                enter = scaleIn(
                    animationSpec = keyframes {
                        durationMillis = 200
                        1.025f at 95
                        0.995f at 155
                    },
                    initialScale = 0.94f,
                    transformOrigin = TransformOrigin(1f, 0f),
                ),
                exit = scaleOut(
                    animationSpec = tween(durationMillis = MENU_EXIT_ANIMATION_MILLIS),
                    targetScale = 0.86f,
                    transformOrigin = TransformOrigin(1f, 0.5f),
                ) + fadeOut(
                    animationSpec = tween(
                        durationMillis = 160,
                        delayMillis = 20,
                    ),
                ),
            ) {
                Surface(
                    modifier = Modifier
                        .width(196.dp)
                        .heightIn(max = 620.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        ),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        itemsIndexed(choices) { index, choice ->
                            val selected = index == selectedIndex
                            Surface(
                                onClick = {
                                    clickHaptic(view)
                                    closeMenu { onSelected(index) }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = if (selected) {
                                    MaterialTheme.shapes.extraLarge
                                } else {
                                    MaterialTheme.shapes.medium
                                },
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    Color.Transparent
                                },
                                contentColor = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    if (selected) {
                                        Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                    Text(
                                        text = choice,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val MENU_EXIT_ANIMATION_MILLIS = 180
private const val MENU_EXIT_WAIT_MILLIS = 200L

@Composable
private fun languageLabel(tag: String): String =
    languageOptions.firstOrNull { languageMatches(it, tag) }
        ?.let { stringResource(it.label) }
        ?: stringResource(R.string.language_system)

private fun languageMatches(option: LanguageOption, currentTag: String): Boolean {
    if (option.tag.isEmpty()) return currentTag.isEmpty()
    return currentTag == option.tag || currentTag.startsWith("$option.tag-")
}

@Composable
private fun accentLabel(color: AccentColor): String = when (color) {
    AccentColor.Dynamic -> stringResource(R.string.color_dynamic)
    AccentColor.Blue -> stringResource(R.string.color_blue)
    AccentColor.Violet -> stringResource(R.string.color_violet)
    AccentColor.Green -> stringResource(R.string.color_green)
    AccentColor.Orange -> stringResource(R.string.color_orange)
}

@Composable
private fun themeModeLabel(themeMode: AppThemeMode): String = when (themeMode) {
    AppThemeMode.System -> stringResource(R.string.theme_system)
    AppThemeMode.Light -> stringResource(R.string.theme_light)
    AppThemeMode.Dark -> stringResource(R.string.theme_dark)
}

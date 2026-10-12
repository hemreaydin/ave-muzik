package com.avemuzik.player.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avemuzik.player.BuildConfig
import com.avemuzik.player.R
import com.avemuzik.player.data.repository.LibraryState
import com.avemuzik.player.model.ThemeMode
import com.avemuzik.player.navigation.AppNavigator
import com.avemuzik.player.ui.components.LocalAppContainer
import com.avemuzik.player.ui.components.LocalMessenger
import com.avemuzik.player.ui.viewmodel.LibraryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(libraryViewModel: LibraryViewModel, navigator: AppNavigator) {
    val settings = LocalAppContainer.current.settingsRepository
    val themeMode by settings.themeMode.collectAsStateWithLifecycle()
    val libraryState by libraryViewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val messenger = LocalMessenger.current
    val resources = LocalResources.current
    val songCount = (libraryState as? LibraryState.Ready)?.library?.songs?.size ?: 0
    val itemColors = ListItemDefaults.colors(containerColor = Color.Transparent)

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = navigator::back) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                title = { Text(stringResource(R.string.settings)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            SectionTitle(stringResource(R.string.settings_appearance))
            Column(Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { mode ->
                    val selected = (themeMode ?: ThemeMode.Default) == mode
                    ListItem(
                        headlineContent = { Text(stringResource(mode.label())) },
                        leadingContent = { RadioButton(selected = selected, onClick = null) },
                        colors = itemColors,
                        modifier = Modifier.selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { scope.launch { settings.setThemeMode(mode) } },
                        ),
                    )
                }
            }

            SectionTitle(stringResource(R.string.settings_library))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_rescan)) },
                supportingContent = { Text(pluralStringResource(R.plurals.settings_rescan_summary, songCount, songCount)) },
                leadingContent = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
                colors = itemColors,
                modifier = Modifier.clickable {
                    libraryViewModel.refresh()
                    messenger.show(resources.getString(R.string.msg_library_refreshing))
                },
            )

            SectionTitle(stringResource(R.string.settings_about))
            ListItem(
                headlineContent = { Text(stringResource(R.string.app_name)) },
                supportingContent = {
                    Text(
                        stringResource(R.string.settings_version, BuildConfig.VERSION_NAME) + "\n" +
                            stringResource(R.string.app_motto),
                    )
                },
                leadingContent = { Icon(Icons.Rounded.Info, contentDescription = null) },
                colors = itemColors,
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_privacy)) },
                supportingContent = { Text(stringResource(R.string.settings_privacy_summary)) },
                leadingContent = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                colors = itemColors,
            )
        }
    }
}

private fun ThemeMode.label(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.DARK -> R.string.theme_dark
    ThemeMode.LIGHT -> R.string.theme_light
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

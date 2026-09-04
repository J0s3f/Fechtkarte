package at.j0s.meyercard.app.adapter.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import at.j0s.meyercard.app.R
import at.j0s.meyercard.app.adapter.ui.configure.ConfigureScreen
import at.j0s.meyercard.app.adapter.ui.configure.ConfigureScreenState
import at.j0s.meyercard.app.adapter.ui.learn.LearnScreen
import at.j0s.meyercard.app.adapter.ui.library.LibraryScreen
import at.j0s.meyercard.app.adapter.ui.notices.NoticesScreen
import at.j0s.meyercard.app.adapter.ui.notices.RUNTIME_DEPENDENCY_NOTICES
import at.j0s.meyercard.app.adapter.ui.notices.readFontLicenceAsset
import at.j0s.meyercard.app.adapter.ui.sources.SourcesScreen
import at.j0s.meyercard.app.adapter.ui.sources.readSourcesScanAsset
import at.j0s.meyercard.app.adapter.ui.train.ShakeToGenerate
import at.j0s.meyercard.app.adapter.ui.train.TrainScreen
import at.j0s.meyercard.app.application.port.api.BrowseHistoricalCards
import at.j0s.meyercard.app.application.port.api.ExportCard
import at.j0s.meyercard.app.application.port.api.ShareCard
import at.j0s.meyercard.app.application.port.spi.ExportResult
import at.j0s.meyercard.app.application.port.spi.PreferencesStore
import at.j0s.meyercard.app.domain.CardLineStyle
import at.j0s.meyercard.app.domain.DrillGenerator
import at.j0s.meyercard.app.domain.Hand
import at.j0s.meyercard.app.domain.HistoricalDrill
import at.j0s.meyercard.app.domain.MeyerCard
import kotlinx.coroutines.launch

private object Route {
    const val LIBRARY = "library"
    const val TRAIN = "train"
    const val CONFIGURE = "configure"
    const val LEARN = "learn"
    const val NOTICES = "notices"
    const val SOURCES = "sources"
}

/**
 * Three peer destinations (Library, Train, Learn) in a bottom nav bar, with Configure pushed
 * on top of Train rather than being a peer of its own. There's also a fourth, standalone
 * "Home" screen with its own title treatment; that's T7.1's job (original artwork),
 * not this task's — until it exists, Library is the start destination, same as before Learn
 * was added.
 */
@Composable
fun FechtkarteApp(
    browseHistoricalCards: BrowseHistoricalCards,
    preferencesStore: PreferencesStore,
    exportCard: ExportCard,
    shareCard: ShareCard,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Hoisted above TrainRoute itself (not `remember`ed inside it) so the generated card
    // survives leaving and returning to Train — TrainRoute's own composition is torn down and
    // rebuilt by NavHost on every visit, the same way Library's content reloads on every visit,
    // which is correct there (a static list) but wasn't here (a card the user was mid-drill
    // with). Found on a real device: visiting Library or Learn and coming back silently
    // replaced the active card with a freshly generated one, with no tap on "Generate".
    var trainCard by remember { mutableStateOf<MeyerCard?>(null) }
    var trainLineStyle by remember { mutableStateOf(CardLineStyle.COMPASS) }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == Route.LIBRARY,
                    onClick = { navController.navigate(Route.LIBRARY) { launchSingleTop = true } },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_library)) },
                )
                NavigationBarItem(
                    selected = currentRoute == Route.TRAIN || currentRoute == Route.CONFIGURE,
                    onClick = { navController.navigate(Route.TRAIN) { launchSingleTop = true } },
                    icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_train)) },
                )
                NavigationBarItem(
                    selected = currentRoute == Route.LEARN,
                    onClick = { navController.navigate(Route.LEARN) { launchSingleTop = true } },
                    icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_learn)) },
                )
            }
        },
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = Route.LIBRARY,
            modifier = Modifier.padding(contentPadding),
        ) {
            composable(Route.LIBRARY) { LibraryRoute(browseHistoricalCards, preferencesStore) }
            composable(Route.TRAIN) {
                TrainRoute(
                    preferencesStore,
                    exportCard,
                    shareCard,
                    card = trainCard,
                    onCardChange = { trainCard = it },
                    lineStyle = trainLineStyle,
                    onLineStyleChange = { trainLineStyle = it },
                    onConfigure = { navController.navigate(Route.CONFIGURE) },
                )
            }
            composable(Route.CONFIGURE) { ConfigureRoute(preferencesStore) }
            composable(Route.LEARN) {
                LearnScreen(
                    onNoticesClick = { navController.navigate(Route.NOTICES) },
                    onSourcesClick = { navController.navigate(Route.SOURCES) },
                )
            }
            composable(Route.NOTICES) { NoticesRoute() }
            composable(Route.SOURCES) { SourcesRoute() }
        }
    }
}

private data class LibraryContent(val drills: List<HistoricalDrill>, val techniqueCards: List<MeyerCard>)

@Composable
private fun LibraryRoute(
    browseHistoricalCards: BrowseHistoricalCards,
    preferencesStore: PreferencesStore,
    modifier: Modifier = Modifier,
) {
    val content by produceState<LibraryContent?>(initialValue = null, browseHistoricalCards) {
        value = LibraryContent(browseHistoricalCards.drills(), browseHistoricalCards.techniqueCards())
    }
    // Reloaded on every entry into this route (Unit-keyed, same as TrainRoute's regenerate()),
    // so a change made on the Configure screen is reflected on returning to the Library without
    // needing its own live-update mechanism.
    val lineStyle by produceState(initialValue = CardLineStyle.COMPASS, preferencesStore) {
        value = preferencesStore.load().cardLineStyle
    }

    val loaded = content
    if (loaded == null) {
        CircularProgressIndicator(modifier = modifier.wrapContentSize(Alignment.Center))
    } else {
        LibraryScreen(
            drills = loaded.drills,
            techniqueCards = loaded.techniqueCards,
            modifier = modifier,
            lineStyle = lineStyle,
        )
    }
}

@Composable
private fun TrainRoute(
    preferencesStore: PreferencesStore,
    exportCard: ExportCard,
    shareCard: ShareCard,
    card: MeyerCard?,
    onCardChange: (MeyerCard?) -> Unit,
    lineStyle: CardLineStyle,
    onLineStyleChange: (CardLineStyle) -> Unit,
    onConfigure: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    suspend fun regenerate() {
        val preferences = preferencesStore.load()
        val (actionCount, thrustCount) = preferences.resolveCounts()
        val outcome = DrillGenerator.generateWithRules(
            actionCount = actionCount,
            thrustCount = thrustCount,
            rules = preferences.enabledRules,
        )
        val palette = if (outcome.card.hand == Hand.RIGHT) preferences.rightHandPalette else preferences.leftHandPalette
        onCardChange(outcome.card.copy(palette = palette))
        onLineStyleChange(preferences.cardLineStyle)
    }

    suspend fun save(cardToSave: MeyerCard, export: suspend (MeyerCard, CardLineStyle) -> ExportResult) {
        val messageRes = try {
            export(cardToSave, lineStyle)
            R.string.card_saved
        } catch (_: Exception) {
            R.string.error_saving
        }
        Toast.makeText(context, messageRes, Toast.LENGTH_SHORT).show()
    }

    suspend fun share(cardToShare: MeyerCard) {
        val shareable = try {
            shareCard.prepare(cardToShare, lineStyle)
        } catch (_: Exception) {
            Toast.makeText(context, R.string.error_sharing, Toast.LENGTH_SHORT).show()
            return
        }
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = shareable.mimeType
            putExtra(Intent.EXTRA_STREAM, shareable.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(sendIntent, null))
    }

    // Only the very first time Train is ever entered — card is hoisted to FechtkarteApp
    // specifically so it survives leaving and returning (see that state's own comment); a plain
    // LaunchedEffect(Unit) here would regenerate a fresh card on every re-entry instead.
    LaunchedEffect(Unit) { if (card == null) regenerate() }
    ShakeToGenerate(onShake = { scope.launch { regenerate() } })

    val current = card
    if (current == null) {
        CircularProgressIndicator(modifier = modifier.wrapContentSize(Alignment.Center))
    } else {
        TrainScreen(
            card = current,
            onGenerate = { scope.launch { regenerate() } },
            onConfigure = onConfigure,
            onSavePng = { scope.launch { save(current, exportCard::asPng) } },
            onSavePdf = { scope.launch { save(current, exportCard::asPdf) } },
            onShare = { scope.launch { share(current) } },
            modifier = modifier,
            lineStyle = lineStyle,
        )
    }
}

@Composable
private fun NoticesRoute(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val fontLicenceText by produceState<String?>(initialValue = null) {
        value = context.readFontLicenceAsset()
    }

    val loaded = fontLicenceText
    if (loaded == null) {
        CircularProgressIndicator(modifier = modifier.wrapContentSize(Alignment.Center))
    } else {
        NoticesScreen(entries = RUNTIME_DEPENDENCY_NOTICES, fontLicenceText = loaded, modifier = modifier)
    }
}

@Composable
private fun SourcesRoute(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scan by produceState<ImageBitmap?>(initialValue = null) {
        value = context.readSourcesScanAsset()
    }

    val loaded = scan
    if (loaded == null) {
        CircularProgressIndicator(modifier = modifier.wrapContentSize(Alignment.Center))
    } else {
        SourcesScreen(scan = loaded, modifier = modifier)
    }
}

@Composable
private fun ConfigureRoute(preferencesStore: PreferencesStore, modifier: Modifier = Modifier) {
    var state by remember { mutableStateOf<ConfigureScreenState?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { state = ConfigureScreenState(preferencesStore.load()) }

    val current = state
    if (current == null) {
        CircularProgressIndicator(modifier = modifier.wrapContentSize(Alignment.Center))
    } else {
        ConfigureScreen(
            state = current,
            onStateChange = { newState ->
                state = newState
                scope.launch { preferencesStore.save(newState.preferences) }
            },
            modifier = modifier,
        )
    }
}

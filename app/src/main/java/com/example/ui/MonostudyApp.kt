package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.StudyAnalytics
import com.example.data.repository.CatalogAssetLoader
import com.example.data.repository.StudyRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ErrorLogScreen
import com.example.ui.screens.FlashcardsScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.PlannerScreen
import com.example.ui.screens.PyqVaultScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SyllabusScreen
import com.example.ui.theme.MonostudyPalette
import com.example.ui.viewmodel.StudyViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

sealed interface StudyRoute {
    @Serializable data object Dashboard : StudyRoute
    @Serializable data object Syllabus : StudyRoute
    @Serializable data object PyqVault : StudyRoute
    @Serializable data object Flashcards : StudyRoute
    @Serializable data object ErrorLog : StudyRoute
    @Serializable data object Notes : StudyRoute
    @Serializable data object Planner : StudyRoute
    @Serializable data object Analytics : StudyRoute
    @Serializable data object Settings : StudyRoute
}

data class NavDestinationItem(
    val route: StudyRoute,
    val label: String,
    val shortLabel: String,
    val icon: ImageVector,
    val isPrimaryBottom: Boolean
)

private val ALL_DESTINATIONS = listOf(
    NavDestinationItem(StudyRoute.Dashboard, "Dashboard", "Dashboard", Icons.Outlined.Dashboard, true),
    NavDestinationItem(StudyRoute.Syllabus, "Syllabus", "Syllabus", Icons.Outlined.ViewList, true),
    NavDestinationItem(StudyRoute.PyqVault, "PYQ vault", "PYQ", Icons.Outlined.EditNote, true),
    NavDestinationItem(StudyRoute.Flashcards, "Flashcards", "Cards", Icons.Outlined.Style, true),
    NavDestinationItem(StudyRoute.ErrorLog, "Error log", "Errors", Icons.Outlined.Flag, false),
    NavDestinationItem(StudyRoute.Notes, "Notes", "Notes", Icons.Outlined.Description, false),
    NavDestinationItem(StudyRoute.Planner, "Planner", "Planner", Icons.Outlined.CalendarToday, true),
    NavDestinationItem(StudyRoute.Analytics, "Analytics", "Analytics", Icons.Outlined.Analytics, false),
    NavDestinationItem(StudyRoute.Settings, "Settings", "Settings", Icons.Outlined.Settings, false)
)

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun MonostudyApp(
    widthSizeClass: WindowWidthSizeClass,
    auth: FirebaseAuth = Firebase.auth
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedStudyShell(
                currentUserId = user.uid,
                initialDisplayName = user.displayName.orEmpty(),
                userEmail = user.email,
                widthSizeClass = widthSizeClass
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedStudyShell(
    currentUserId: String,
    initialDisplayName: String,
    userEmail: String?,
    widthSizeClass: WindowWidthSizeClass
) {
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val scope = rememberCoroutineScope()

    val viewModel: StudyViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                StudyViewModel(
                    repository = StudyRepository(app),
                    assetLoader = CatalogAssetLoader(app),
                    currentUserId = currentUserId,
                    initialDisplayName = initialDisplayName
                )
            }
        }
    )

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pomodoro by viewModel.pomodoro.collectAsStateWithLifecycle()
    val answeredPyqs by viewModel.answeredPyqs.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.toastEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val activeNavItem = ALL_DESTINATIONS.find { item ->
        currentDestination?.hasRoute(item.route::class) == true
    } ?: ALL_DESTINATIONS.first()

    val isExpanded = widthSizeClass == WindowWidthSizeClass.Expanded
    val isMediumOrExpanded = widthSizeClass != WindowWidthSizeClass.Compact
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val streakInfo = remember(state.sessions, state.attempts, state.tasks) {
        StudyAnalytics.streak(state.sessions, state.attempts, state.tasks)
    }

    fun navigateTo(route: StudyRoute) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
        scope.launch { drawerState.close() }
    }

    fun performSignOut() {
        signOutUser(credentialManager = credentialManager, scope = scope)
    }

    val drawerContent: @Composable () -> Unit = {
        ModalDrawerSheet(
            drawerContainerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.width(276.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "m",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    Column {
                        Text(
                            text = buildAnnotatedString {
                                append("mono")
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                                    append("study")
                                }
                            },
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "JEE • NEET • CLASS 11–12",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val name = state.profile.name.trim().ifEmpty { "Student" }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MonostudyPalette.PanelElevated
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = MonostudyPalette.PrimaryDeep
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = name.first().uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Column {
                            Text(text = name, style = MaterialTheme.typography.titleMedium)
                            val examLabel = if (state.profile.exam == "Both") "JEE + NEET" else state.profile.exam
                            Text(
                                text = "$examLabel · Class ${state.profile.cls}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                ALL_DESTINATIONS.forEach { item ->
                    val selected = currentDestination?.hasRoute(item.route::class) == true
                    NavigationDrawerItem(
                        label = { Text(item.label) },
                        selected = selected,
                        onClick = { navigateTo(item.route) },
                        icon = { Icon(item.icon, contentDescription = null) },
                        modifier = Modifier.testTag("drawer_nav_${item.shortLabel.lowercase()}")
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                NavigationDrawerItem(
                    label = { Text("Log out", color = MonostudyPalette.AccentRed) },
                    selected = false,
                    onClick = { performSignOut() },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Logout,
                            contentDescription = "Log out",
                            tint = MonostudyPalette.AccentRed
                        )
                    },
                    modifier = Modifier.testTag("drawer_logout_button")
                )
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = drawerContent,
        gesturesEnabled = !isMediumOrExpanded
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    navigationIcon = {
                        if (!isMediumOrExpanded) {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("open_drawer_button")
                            ) {
                                Icon(Icons.Outlined.Menu, contentDescription = "Open navigation menu")
                            }
                        }
                    },
                    title = {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                                    append("monostudy / ")
                                }
                                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                                    append(activeNavItem.label)
                                }
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    actions = {
                        Surface(
                            shape = RoundedCornerShape(99.dp),
                            color = MonostudyPalette.PanelElevated,
                            border = BorderStroke(1.dp, MonostudyPalette.AccentOrange),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text(
                                text = "🔥 ${streakInfo.days} day${if (streakInfo.days == 1) "" else "s"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MonostudyPalette.AccentOrange,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!isMediumOrExpanded) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        ALL_DESTINATIONS.filter { it.isPrimaryBottom }.forEach { item ->
                            val selected = currentDestination?.hasRoute(item.route::class) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.shortLabel, maxLines = 1) },
                                modifier = Modifier.testTag("bottom_nav_${item.shortLabel.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isMediumOrExpanded) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        ALL_DESTINATIONS.forEach { item ->
                            val selected = currentDestination?.hasRoute(item.route::class) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.shortLabel) },
                                modifier = Modifier.testTag("rail_nav_${item.shortLabel.lowercase()}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (state.isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        Box(modifier = Modifier.widthIn(max = 1080.dp).fillMaxSize()) {
                            NavHost(
                                navController = navController,
                                startDestination = StudyRoute.Dashboard
                            ) {
                                composable<StudyRoute.Dashboard> {
                                    DashboardScreen(
                                        state = state,
                                        isWideScreen = isExpanded,
                                        onNavigateToAnalytics = { navigateTo(StudyRoute.Analytics) },
                                        onNavigateToSyllabus = { navigateTo(StudyRoute.Syllabus) },
                                        onNavigateToPlanner = { navigateTo(StudyRoute.Planner) },
                                        onToggleTask = viewModel::toggleTask,
                                        onDeleteTask = viewModel::deleteTask
                                    )
                                }
                                composable<StudyRoute.Syllabus> {
                                    SyllabusScreen(
                                        state = state,
                                        onClassExamChanged = viewModel::updateProfileSelection,
                                        onChapterStatusChanged = viewModel::setChapterStatus
                                    )
                                }
                                composable<StudyRoute.PyqVault> {
                                    PyqVaultScreen(
                                        state = state,
                                        answeredPyqs = answeredPyqs,
                                        onQuestionVisible = viewModel::markQuestionVisible,
                                        onAnswerSelected = viewModel::answerPyq,
                                        onErrorCategorySelected = viewModel::setPyqErrorCategory
                                    )
                                }
                                composable<StudyRoute.Flashcards> {
                                    FlashcardsScreen(
                                        state = state,
                                        onMarkCard = viewModel::setFlashcardStatus
                                    )
                                }
                                composable<StudyRoute.ErrorLog> {
                                    ErrorLogScreen(
                                        state = state,
                                        onToggleResolved = viewModel::toggleErrorResolved,
                                        onDeleteError = viewModel::deleteErrorEntry
                                    )
                                }
                                composable<StudyRoute.Notes> {
                                    NotesScreen(
                                        state = state,
                                        isWideScreen = isExpanded,
                                        onCreateNote = viewModel::createNote,
                                        onUpdateNote = viewModel::updateNote,
                                        onDeleteNote = viewModel::deleteNote,
                                        onAttachImage = viewModel::attachNoteImage,
                                        onRemoveImage = viewModel::removeNoteImage
                                    )
                                }
                                composable<StudyRoute.Planner> {
                                    PlannerScreen(
                                        state = state,
                                        pomodoro = pomodoro,
                                        onAddTask = viewModel::addTask,
                                        onToggleTask = viewModel::toggleTask,
                                        onDeleteTask = viewModel::deleteTask,
                                        onClearCompletedTasks = viewModel::clearCompletedTasks,
                                        onPomodoroTagChange = viewModel::setPomodoroTag,
                                        onTogglePomodoroRun = viewModel::togglePomodoroRun,
                                        onResetPomodoro = viewModel::resetPomodoro,
                                        onSelectPreset = viewModel::selectPomodoroPreset,
                                        onToggleSound = viewModel::setPomodoroSound,
                                        onSaveGoal = { goal ->
                                            viewModel.saveProfile(
                                                state.profile.name,
                                                state.profile.cls,
                                                state.profile.exam,
                                                goal
                                            )
                                        },
                                        onLog25Minutes = { tag -> viewModel.logStudyMinutes(25, tag) }
                                    )
                                }
                                composable<StudyRoute.Analytics> {
                                    AnalyticsScreen(
                                        state = state,
                                        onGenerateWeeklyCard = viewModel::generateWeeklySummaryPng
                                    )
                                }
                                composable<StudyRoute.Settings> {
                                    SettingsScreen(
                                        state = state,
                                        userEmail = userEmail,
                                        onSaveProfile = viewModel::saveProfile,
                                        onBuildBackupJson = viewModel::buildBackupJson,
                                        onImportBackupJson = viewModel::importBackupJson,
                                        onBuildQuestionTemplateJson = viewModel::buildQuestionTemplateJson,
                                        onImportQuestionsJson = viewModel::importQuestionsFromJson,
                                        onClearCustomQuestions = viewModel::clearCustomQuestions,
                                        onResetAllData = viewModel::resetAllData,
                                        onSignOut = { performSignOut() }
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

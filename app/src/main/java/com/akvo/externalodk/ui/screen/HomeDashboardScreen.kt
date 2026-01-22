package com.akvo.externalodk.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.akvo.externalodk.ui.component.SubmissionListItem
import com.akvo.externalodk.ui.theme.ExternalODKTheme
import com.akvo.externalodk.ui.viewmodel.HomeUiState
import com.akvo.externalodk.ui.viewmodel.HomeViewModel

@Composable
fun HomeDashboardScreen(
    onResyncClick: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeDashboardContent(
        uiState = uiState,
        onResyncClick = onResyncClick,
        onLogout = {
            viewModel.logout { onLogout() }
        },
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onSearchActiveChange = viewModel::onSearchActiveChange,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeDashboardContent(
    uiState: HomeUiState,
    onResyncClick: () -> Unit,
    onLogout: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.isSearchActive) {
        if (uiState.isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = {
                Text("All local data will be erased. This action cannot be undone. Are you sure you want to logout?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Logout", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            if (uiState.isSearchActive) {
                TopAppBar(
                    title = {
                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text("Search submissions...") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { onSearchActiveChange(false) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close search"
                            )
                        }
                    },
                    actions = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            } else {
                TopAppBar(
                    title = { Text("Submissions") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    actions = {
                        IconButton(onClick = { onSearchActiveChange(true) }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options"
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Logout") },
                                onClick = {
                                    showMenu = false
                                    showLogoutDialog = true
                                }
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!uiState.isSearchActive) {
                FloatingActionButton(onClick = onResyncClick) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Resync Data"
                    )
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.filteredSubmissions.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (uiState.searchQuery.isNotEmpty()) {
                            "No results found"
                        } else {
                            "No submissions yet"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                SubmissionList(
                    submissions = uiState.filteredSubmissions,
                    listState = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun SubmissionList(
    submissions: List<com.akvo.externalodk.ui.model.SubmissionUiModel>,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = submissions,
            key = { it.uuid }
        ) { submission ->
            SubmissionListItem(submission = submission)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeDashboardPreview() {
    ExternalODKTheme {
        HomeDashboardContent(
            uiState = HomeUiState(
                submissions = previewSubmissions,
                filteredSubmissions = previewSubmissions
            ),
            onResyncClick = {},
            onLogout = {},
            onSearchQueryChange = {},
            onSearchActiveChange = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeDashboardLoadingPreview() {
    ExternalODKTheme {
        HomeDashboardContent(
            uiState = HomeUiState(isLoading = true),
            onResyncClick = {},
            onLogout = {},
            onSearchQueryChange = {},
            onSearchActiveChange = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeDashboardSearchActivePreview() {
    ExternalODKTheme {
        HomeDashboardContent(
            uiState = HomeUiState(
                submissions = previewSubmissions,
                filteredSubmissions = previewSubmissions,
                isSearchActive = true,
                searchQuery = "ifir"
            ),
            onResyncClick = {},
            onLogout = {},
            onSearchQueryChange = {},
            onSearchActiveChange = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeDashboardEmptyPreview() {
    ExternalODKTheme {
        HomeDashboardContent(
            uiState = HomeUiState(
                submissions = emptyList(),
                filteredSubmissions = emptyList()
            ),
            onResyncClick = {},
            onLogout = {},
            onSearchQueryChange = {},
            onSearchActiveChange = {}
        )
    }
}

private val previewSubmissions = listOf(
    com.akvo.externalodk.ui.model.SubmissionUiModel(
        uuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        submittedBy = "ifirmawan",
        submissionTime = "2026-01-21 09:30",
        isSynced = true
    ),
    com.akvo.externalodk.ui.model.SubmissionUiModel(
        uuid = "b2c3d4e5-f6a7-8901-bcde-f12345678901",
        submittedBy = "john.doe",
        submissionTime = "2026-01-21 08:15",
        isSynced = true
    ),
    com.akvo.externalodk.ui.model.SubmissionUiModel(
        uuid = "c3d4e5f6-a7b8-9012-cdef-123456789012",
        submittedBy = "maria.santos",
        submissionTime = "2026-01-20 16:45",
        isSynced = true
    )
)

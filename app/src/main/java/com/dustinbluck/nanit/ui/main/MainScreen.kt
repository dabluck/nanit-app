package com.dustinbluck.nanit.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.dustinbluck.nanit.R
import com.dustinbluck.nanit.data.Baby
import com.dustinbluck.nanit.deps.NanitDeps
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScreen(
    onBirthdayClick: () -> Unit,
    viewModel: MainViewModel = viewModel {
        MainViewModel(
            babyRepository = NanitDeps.instance.babyRepository,
            logger = NanitDeps.instance.logger
        )
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(stringResource(R.string.app_name))
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            MainUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator()
            }

            MainUiState.Error -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.baby_load_error),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            is MainUiState.Loaded -> {
                BabyDetails(
                    baby = state.baby,
                    isBirthdayEnabled = state.isBirthdayEnabled,
                    onEditNameClick = viewModel::editName,
                    onBirthdayClick = onBirthdayClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
                val nameEditState = state.nameEditState
                if (nameEditState is NameEditState.Open) {
                    EditNameDialog(
                        name = state.baby.name,
                        editState = nameEditState,
                        onSave = viewModel::saveName,
                        onDismiss = viewModel::cancelNameEdit
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BabyDetails(
    baby: Baby,
    isBirthdayEnabled: Boolean,
    onEditNameClick: () -> Unit,
    onBirthdayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val birthdayFormatter = remember {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
    }
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = 24.dp,
                vertical = 16.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        AsyncImage(
            model = baby.photo,
            contentDescription = if (baby.photo == null) {
                null
            } else {
                stringResource(R.string.baby_photo)
            },
            modifier = Modifier
                .size(200.dp)
                .clip(MaterialShapes.Cookie9Sided.toShape())
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentScale = ContentScale.Crop
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BabyField(
                    label = stringResource(R.string.baby_name),
                    value = baby.name?.takeUnless(String::isBlank)
                        ?: stringResource(R.string.not_set),
                    action = {
                        IconButton(onClick = onEditNameClick) {
                            Icon(
                                painter = painterResource(R.drawable.ic_edit),
                                contentDescription = stringResource(R.string.edit_name)
                            )
                        }
                    }
                )
                BabyField(
                    label = stringResource(R.string.baby_birthday),
                    value = baby.birthday?.format(birthdayFormatter)
                        ?: stringResource(R.string.not_set)
                )
            }
        }
        Button(
            onClick = onBirthdayClick,
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.heightIn(ButtonDefaults.MediumContainerHeight),
            enabled = isBirthdayEnabled,
            contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight)
        ) {
            Text(
                text = stringResource(R.string.show_birthday_screen),
                style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight)
            )
        }
    }
}

@Composable
private fun BabyField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLargeEmphasized
            )
        }
        action?.invoke()
    }
}

@Composable
private fun EditNameDialog(
    name: String?,
    editState: NameEditState.Open,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val nameState = rememberTextFieldState(initialText = name.orEmpty())
    val focusRequester = remember {
        FocusRequester()
    }
    val canSave = nameState.text.isNotBlank() && !editState.isSaving
    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.edit_name))
        },
        text = {
            OutlinedTextField(
                state = nameState,
                modifier = Modifier.focusRequester(focusRequester),
                label = {
                    Text(stringResource(R.string.baby_name))
                },
                supportingText = if (editState.saveFailed) {
                    {
                        Text(stringResource(R.string.name_save_error))
                    }
                } else {
                    null
                },
                isError = editState.saveFailed,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                onKeyboardAction = {
                    if (canSave) {
                        onSave(nameState.text.toString())
                    }
                },
                lineLimits = TextFieldLineLimits.SingleLine
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(nameState.text.toString())
                },
                enabled = canSave
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

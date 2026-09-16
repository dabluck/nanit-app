package com.dustinbluck.nanit.ui.main

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.getSelectedDate
import androidx.compose.material3.rememberDatePickerState
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
import com.dustinbluck.nanit.ui.photo.EditPhotoSheet
import com.dustinbluck.nanit.ui.photo.PhotoEditState
import com.dustinbluck.nanit.ui.photo.rememberPhotoPicker
import com.dustinbluck.nanit.ui.theme.NanitButtonDefaults
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScreen(
    onBirthdayClick: () -> Unit,
    clock: Clock = NanitDeps.instance.clock,
    viewModel: MainViewModel = viewModel {
        MainViewModel(
            babyRepository = NanitDeps.instance.babyRepository,
            clock = NanitDeps.instance.clock,
            logger = NanitDeps.instance.logger
        )
    }
) {
    val today = remember(clock) {
        LocalDate.now(clock)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val photoEditState by viewModel.photoEditState.collectAsStateWithLifecycle()
    val photoPicker = rememberPhotoPicker(onPhotoPicked = viewModel::savePhoto)
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
                    onEditBirthdayClick = viewModel::editBirthday,
                    onEditPhotoClick = viewModel::editPhoto,
                    onBirthdayClick = onBirthdayClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
                val editState = state.editState
                if (editState is EditState.Open) {
                    when (editState.field) {
                        EditField.NAME -> EditNameDialog(
                            name = state.baby.name,
                            editState = editState,
                            onSave = viewModel::saveName,
                            onDismiss = viewModel::cancelEdit
                        )

                        EditField.BIRTHDAY -> EditBirthdayDialog(
                            birthday = state.baby.birthday,
                            today = today,
                            editState = editState,
                            onSave = viewModel::saveBirthday,
                            onDismiss = viewModel::cancelEdit
                        )

                    }
                }
                val photoEdit = photoEditState
                if (photoEdit is PhotoEditState.Open && !photoEdit.isSaving) {
                    EditPhotoSheet(
                        photoPicker = photoPicker,
                        canRemovePhoto = state.baby.photo != null,
                        saveFailed = photoEdit.saveFailed,
                        onRemovePhotoClick = viewModel::clearPhoto,
                        onDismiss = viewModel::cancelPhotoEdit
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
    onEditBirthdayClick: () -> Unit,
    onEditPhotoClick: () -> Unit,
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
        Box(contentAlignment = Alignment.Center) {
            AsyncImage(
                model = baby.photo,
                contentDescription = if (baby.photo == null) {
                    null
                } else {
                    stringResource(R.string.baby_photo)
                },
                modifier = Modifier
                    .size(BabyPhotoSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentScale = ContentScale.Crop
            )
            FilledIconButton(
                onClick = onEditPhotoClick,
                modifier = Modifier.offset(
                    x = EditPhotoOffset,
                    y = EditPhotoOffset
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = stringResource(R.string.edit_photo)
                )
            }
        }
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
                        ?: stringResource(R.string.not_set),
                    action = {
                        IconButton(onClick = onEditBirthdayClick) {
                            Icon(
                                painter = painterResource(R.drawable.ic_edit),
                                contentDescription = stringResource(R.string.edit_birthday)
                            )
                        }
                    }
                )
            }
        }
        Button(
            onClick = onBirthdayClick,
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.heightIn(NanitButtonDefaults.ContainerHeight),
            enabled = isBirthdayEnabled,
            contentPadding = NanitButtonDefaults.ContentPadding
        ) {
            Text(
                text = stringResource(R.string.show_birthday_screen),
                style = NanitButtonDefaults.TextStyle
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
                style = MaterialTheme.typography.titleLargeEmphasized,
                maxLines = 3
            )
        }
        action?.invoke()
    }
}

@Composable
private fun EditNameDialog(
    name: String?,
    editState: EditState.Open,
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
                inputTransformation = InputTransformation.maxLength(MaxNameLength),
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

@Composable
private fun EditBirthdayDialog(
    birthday: LocalDate?,
    today: LocalDate,
    editState: EditState.Open,
    onSave: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val selectableDates = remember(today) {
        PastDates(today)
    }

    @SuppressLint("NewApi")
    val datePickerState = rememberDatePickerState(
        initialSelectedDate = birthday,
        selectableDates = selectableDates
    )

    @SuppressLint("NewApi")
    val selectedDate = datePickerState.getSelectedDate()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedDate != null) {
                        onSave(selectedDate)
                    }
                },
                enabled = selectedDate != null && !editState.isSaving
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
        if (editState.saveFailed) {
            Text(
                text = stringResource(R.string.birthday_save_error),
                modifier = Modifier.padding(horizontal = 24.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// just a sane default to accommodate any reasonable name
private const val MaxNameLength = 255

private val BabyPhotoSize = 200.dp

private val EditPhotoOffset = BabyPhotoSize / 2 / sqrt(2f)

private class PastDates(private val today: LocalDate) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        val date = Instant.ofEpochMilli(utcTimeMillis)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
        return !date.isAfter(today)
    }

    override fun isSelectableYear(year: Int): Boolean {
        return year <= today.year
    }
}

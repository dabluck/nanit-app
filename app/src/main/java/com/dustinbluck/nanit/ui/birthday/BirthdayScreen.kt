package com.dustinbluck.nanit.ui.birthday

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.size.Dimension
import coil3.size.Precision
import coil3.size.Size
import com.dustinbluck.nanit.R
import com.dustinbluck.nanit.deps.NanitDeps
import com.dustinbluck.nanit.ui.photo.EditPhotoSheet
import com.dustinbluck.nanit.ui.photo.PhotoEditState
import com.dustinbluck.nanit.ui.photo.rememberPhotoPicker
import com.dustinbluck.nanit.ui.theme.NanitButtonDefaults
import java.io.File
import kotlin.math.sqrt

@Composable
fun BirthdayScreen(
    mode: BirthdayMode,
    onCloseClick: () -> Unit,
    viewModel: BirthdayViewModel = viewModel {
        BirthdayViewModel(
            babyRepository = NanitDeps.instance.babyRepository,
            clock = NanitDeps.instance.clock,
            logger = NanitDeps.instance.logger
        )
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val photoEditState by viewModel.photoEditState.collectAsStateWithLifecycle()
    val photoPicker = rememberPhotoPicker(onPhotoPicked = viewModel::savePhoto)
    val modeResources = BirthdayModeResources.of(mode)
    val context = LocalPlatformContext.current
    val displayWidth = LocalResources.current.displayMetrics.widthPixels
    val backgroundRequest = remember(
        context,
        mode,
        displayWidth
    ) {
        // exact size since our images are pretty large and we only want to use the memory we need
        ImageRequest.Builder(context)
            .data(modeResources.background)
            .size(
                Size(
                    width = Dimension(displayWidth),
                    height = Dimension.Undefined
                )
            )
            .precision(Precision.EXACT)
            .build()
    }
    val backgroundPainter = rememberAsyncImagePainter(
        model = backgroundRequest,
        contentScale = ContentScale.FillWidth
    )
    val backgroundState by backgroundPainter.state.collectAsStateWithLifecycle()
    val isBackgroundLoading = backgroundState is AsyncImagePainter.State.Empty ||
            backgroundState is AsyncImagePainter.State.Loading
    val contentState = if (uiState is BirthdayUiState.Loaded && isBackgroundLoading) {
        BirthdayUiState.Loading
    } else {
        uiState
    }
    BirthdayContent(
        modeResources = modeResources,
        uiState = contentState,
        backgroundPainter = backgroundPainter,
        onCloseClick = onCloseClick,
        onEditPhotoClick = viewModel::editPhoto,
        onShareClick = {}
    )
    val photoEdit = photoEditState
    if (photoEdit is PhotoEditState.Open && !photoEdit.isSaving) {
        EditPhotoSheet(
            photoPicker = photoPicker,
            canRemovePhoto = (uiState as? BirthdayUiState.Loaded)?.photo != null,
            saveFailed = photoEdit.saveFailed,
            onRemovePhotoClick = viewModel::clearPhoto,
            onDismiss = viewModel::cancelPhotoEdit
        )
    }
}

@Composable
private fun BirthdayContent(
    modeResources: BirthdayModeResources,
    uiState: BirthdayUiState,
    backgroundPainter: Painter,
    onCloseClick: () -> Unit,
    onEditPhotoClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val backgroundColor = colorResource(modeResources.backgroundColor)
    Scaffold(containerColor = backgroundColor) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            BirthdayStates(
                modeResources = modeResources,
                uiState = uiState,
                backgroundPainter = backgroundPainter,
                innerPadding = innerPadding,
                onEditPhotoClick = onEditPhotoClick,
                onShareClick = onShareClick
            )
            IconButton(
                onClick = onCloseClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(innerPadding)
                    .padding(start = 4.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.close)
                )
            }
        }
    }
}

@Composable
private fun BirthdayStates(
    modeResources: BirthdayModeResources,
    uiState: BirthdayUiState,
    backgroundPainter: Painter,
    innerPadding: PaddingValues,
    onEditPhotoClick: () -> Unit,
    onShareClick: () -> Unit
) {
    AnimatedContent(
        targetState = uiState,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        contentKey = { state ->
            state::class
        }
    ) { state ->
        when (state) {
            BirthdayUiState.Loading -> {
                BirthdayLoading(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            BirthdayUiState.Error -> {
                BirthdayError(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            is BirthdayUiState.Loaded -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = backgroundPainter,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        alignment = Alignment.BottomCenter,
                        contentScale = ContentScale.FillWidth
                    )
                    BirthdayDetails(
                        modeResources = modeResources,
                        uiState = state,
                        onEditPhotoClick = onEditPhotoClick,
                        onShareClick = onShareClick,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 50.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BirthdayLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator()
    }
}

@Composable
private fun BirthdayError(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.baby_load_error),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun BirthdayDetails(
    modeResources: BirthdayModeResources,
    uiState: BirthdayUiState.Loaded,
    onEditPhotoClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(
                    top = 20.dp,
                    bottom = 15.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            BirthdayTitle(
                uiState = uiState,
                modifier = Modifier.wrapContentHeight(
                    align = Alignment.Bottom,
                    unbounded = true
                )
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            BabyPhoto(
                modeResources = modeResources,
                photo = uiState.photo,
                onEditPhotoClick = onEditPhotoClick
            )
            Image(
                painter = painterResource(R.drawable.nanit_logo),
                contentDescription = stringResource(R.string.app_name)
            )
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            ShareButton(onClick = onShareClick)
        }
    }
}

@Composable
private fun ShareButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        shapes = ButtonDefaults.shapes(),
        modifier = modifier.heightIn(NanitButtonDefaults.ContainerHeight),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = colorResource(R.color.nanit_coral),
            contentColor = Color.White
        ),
        contentPadding = NanitButtonDefaults.ContentPadding
    ) {
        Text(
            text = stringResource(R.string.share_birthday),
            style = NanitButtonDefaults.TextStyle
        )
        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
        Icon(
            painter = painterResource(R.drawable.ic_share),
            contentDescription = null,
            modifier = Modifier.size(NanitButtonDefaults.IconSize)
        )
    }
}

@Composable
private fun BirthdayTitle(
    uiState: BirthdayUiState.Loaded,
    modifier: Modifier = Modifier
) {
    val locale = Locale.current
    val age = uiState.age.value
    val ageUnitPlurals = when (uiState.age.unit) {
        AgeUnit.MONTHS -> R.plurals.birthday_months_old
        AgeUnit.YEARS -> R.plurals.birthday_years_old
    }
    val textStyle = TextStyle(
        color = colorResource(R.color.nanit_blue),
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        lineHeight = 1.5.em
    )
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(
                R.string.birthday_today_name_is,
                uiState.name
            ).toUpperCase(locale),
            fontSize = 21.sp,
            overflow = TextOverflow.Ellipsis,
            maxLines = 2,
            style = textStyle
        )
        Row(
            modifier = Modifier.padding(
                top = 13.dp,
                bottom = 14.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.swirls_left),
                contentDescription = null
            )
            AgeNumber(age = age)
            Image(
                painter = painterResource(R.drawable.swirls_right),
                contentDescription = null
            )
        }
        Text(
            pluralStringResource(
                ageUnitPlurals,
                age
            ).toUpperCase(locale),
            fontSize = 18.sp,
            maxLines = 1,
            style = textStyle
        )
    }
}

@Composable
private fun BabyPhoto(
    modeResources: BirthdayModeResources,
    photo: File?,
    onEditPhotoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val defaultBaby = painterResource(modeResources.defaultBaby)
    val ringPainter = painterResource(modeResources.photoRing)
    val addPhotoPainter = painterResource(modeResources.addPhoto)
    val ringSize = with(LocalDensity.current) {
        ringPainter.intrinsicSize.width.toDp()
    }
    val photoSize = ringSize - RingStrokeWidth
    val addPhotoOffset = calcRingOffsetFortyFiveDegrees(
        ringSize = ringSize,
        ringStrokeWidth = RingStrokeWidth
    )
    Box(
        modifier = modifier.size(ringSize),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = photo,
            contentDescription = if (photo == null) {
                null
            } else {
                stringResource(R.string.baby_photo)
            },
            modifier = Modifier
                .size(photoSize)
                .clip(CircleShape),
            fallback = defaultBaby,
            error = defaultBaby,
            contentScale = ContentScale.Crop
        )
        Image(
            painter = ringPainter,
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
        IconButton(
            onClick = onEditPhotoClick,
            modifier = Modifier
                .size(AddPhotoButtonSize)
                .offset(
                    x = addPhotoOffset,
                    y = -addPhotoOffset
                )
        ) {
            Icon(
                painter = addPhotoPainter,
                contentDescription = stringResource(R.string.edit_photo),
                tint = Color.Unspecified
            )
        }
    }
}

private val RingStrokeWidth = 7.dp

private val AddPhotoButtonSize = 48.dp

private fun calcRingOffsetFortyFiveDegrees(
    ringSize: Dp,
    ringStrokeWidth: Dp
): Dp {
    val ringRadius = ringSize / 2                              // 103.5, the box edge
    val strokeCenterRadius = ringRadius - ringStrokeWidth / 2  // 100.0, middle of the stroke
    return strokeCenterRadius / sqrt(2f)                       // 70.71, x/y split at 45 degrees
}

private val NumberDrawables = listOf(
    R.drawable.number_0,
    R.drawable.number_1,
    R.drawable.number_2,
    R.drawable.number_3,
    R.drawable.number_4,
    R.drawable.number_5,
    R.drawable.number_6,
    R.drawable.number_7,
    R.drawable.number_8,
    R.drawable.number_9,
    R.drawable.number_10,
    R.drawable.number_11,
    R.drawable.number_12
)

@Composable
private fun AgeNumber(age: Int) {
    val numberDrawable = NumberDrawables.getOrNull(age)
    if (numberDrawable == null) {
        Text(
            text = age.toString(),
            color = colorResource(R.color.nanit_blue)
        )
    } else {
        Image(
            painter = painterResource(numberDrawable),
            contentDescription = age.toString()
        )
    }
}

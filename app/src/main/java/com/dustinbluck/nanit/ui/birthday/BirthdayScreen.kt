package com.dustinbluck.nanit.ui.birthday

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.size.Dimension
import coil3.size.Precision
import coil3.size.Size
import com.dustinbluck.nanit.R
import com.dustinbluck.nanit.deps.NanitDeps
import com.dustinbluck.nanit.ui.theme.ElephantBackground
import com.dustinbluck.nanit.ui.theme.FoxBackground
import com.dustinbluck.nanit.ui.theme.PelicanBackground

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
    val context = LocalPlatformContext.current
    val displayWidth = LocalResources.current.displayMetrics.widthPixels
    val backgroundRequest = remember(
        context,
        mode
    ) {
        // exact size since our images are pretty large and we only want to use the memory we need
        ImageRequest.Builder(context)
            .data(backgroundDrawableOf(mode))
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
        mode = mode,
        uiState = contentState,
        backgroundPainter = backgroundPainter,
        onCloseClick = onCloseClick
    )
}

private fun backgroundDrawableOf(mode: BirthdayMode): Int {
    return when (mode) {
        BirthdayMode.FOX -> R.drawable.bg_fox
        BirthdayMode.ELEPHANT -> R.drawable.bg_elephant
        BirthdayMode.PELICAN -> R.drawable.bg_pelican
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthdayContent(
    mode: BirthdayMode,
    uiState: BirthdayUiState,
    backgroundPainter: Painter,
    onCloseClick: () -> Unit
) {
    val backgroundColor = when (mode) {
        BirthdayMode.FOX -> FoxBackground
        BirthdayMode.ELEPHANT -> ElephantBackground
        BirthdayMode.PELICAN -> PelicanBackground
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                },
                navigationIcon = {
                    IconButton(onClick = onCloseClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.close)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = backgroundColor
    ) { innerPadding ->
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
                            name = state.name,
                            age = state.age.value,
                            ageUnit = state.age.unit,
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
    name: String,
    age: Int,
    ageUnit: AgeUnit,
    modifier: Modifier = Modifier
) {
    val locale = Locale.current
    val ageUnitPlurals = when (ageUnit) {
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
                name
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

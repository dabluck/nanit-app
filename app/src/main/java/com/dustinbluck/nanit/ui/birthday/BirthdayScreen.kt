package com.dustinbluck.nanit.ui.birthday

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.dustinbluck.nanit.R
import com.dustinbluck.nanit.deps.NanitDeps

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun BirthdayScreen(
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
                }
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            BirthdayUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator()
            }

            BirthdayUiState.Error -> Box(
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

            is BirthdayUiState.Loaded -> BirthdayDetails(
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

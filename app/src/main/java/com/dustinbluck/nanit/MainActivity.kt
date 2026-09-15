package com.dustinbluck.nanit

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.dustinbluck.nanit.ui.birthday.BirthdayMode
import com.dustinbluck.nanit.ui.birthday.BirthdayScreen
import com.dustinbluck.nanit.ui.main.MainScreen
import com.dustinbluck.nanit.ui.theme.NanitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            )
        )
        setContent {
            NanitTheme {
                val backStack = remember {
                    mutableStateListOf<Screen>(Screen.Main)
                }
                NavDisplay(
                    backStack = backStack,
                    onBack = {
                        backStack.removeLastOrNull()
                    },
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator()
                    ),
                    transitionSpec = {
                        fadeIn() togetherWith ExitTransition.KeepUntilTransitionsFinished
                    },
                    popTransitionSpec = {
                        EnterTransition.None togetherWith fadeOut()
                    },
                    predictivePopTransitionSpec = {
                        EnterTransition.None togetherWith fadeOut()
                    }
                ) { key ->
                    NavEntry(key) {
                        when (key) {
                            Screen.Main -> MainScreen(
                                onBirthdayClick = {
                                    backStack.add(Screen.Birthday(BirthdayMode.entries.random()))
                                }
                            )

                            is Screen.Birthday -> BirthdayScreen(
                                mode = key.mode,
                                onCloseClick = {
                                    backStack.removeLastOrNull()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

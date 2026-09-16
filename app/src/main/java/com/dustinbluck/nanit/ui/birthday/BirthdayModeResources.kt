package com.dustinbluck.nanit.ui.birthday

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.dustinbluck.nanit.R

data class BirthdayModeResources(
    @param:DrawableRes val background: Int,
    @param:DrawableRes val defaultBaby: Int,
    @param:DrawableRes val addPhoto: Int,
    @param:ColorRes val backgroundColor: Int
) {
    companion object {
        fun of(mode: BirthdayMode): BirthdayModeResources {
            return when (mode) {
                BirthdayMode.FOX -> BirthdayModeResources(
                    background = R.drawable.bg_fox,
                    defaultBaby = R.drawable.default_baby_fox,
                    addPhoto = R.drawable.ic_add_photo_fox,
                    backgroundColor = R.color.fox_background
                )

                BirthdayMode.ELEPHANT -> BirthdayModeResources(
                    background = R.drawable.bg_elephant,
                    defaultBaby = R.drawable.default_baby_elephant,
                    addPhoto = R.drawable.ic_add_photo_elephant,
                    backgroundColor = R.color.elephant_background
                )

                BirthdayMode.PELICAN -> BirthdayModeResources(
                    background = R.drawable.bg_pelican,
                    defaultBaby = R.drawable.default_baby_pelican,
                    addPhoto = R.drawable.ic_add_photo_pelican,
                    backgroundColor = R.color.pelican_background
                )
            }
        }
    }
}

package com.leninasto.ruleapp.ui

import com.leninasto.ruleapp.R
import com.leninasto.ruleapp.core.t
import com.leninasto.ruleapp.domain.*
import com.leninasto.ruleapp.data.local.ThemeMode

fun Rating.uiLabel(): String = t(when (this) {
    Rating.GENERAL -> R.string.ui_general
    Rating.SENSITIVE -> R.string.ui_sensitive
    Rating.QUESTIONABLE -> R.string.ui_questionable
    Rating.EXPLICIT -> R.string.ui_explicit
    Rating.UNKNOWN -> R.string.ui_unrated
})

fun MediaKind.uiLabel(): String = t(when (this) {
    MediaKind.IMAGE -> R.string.ui_image
    MediaKind.GIF -> R.string.ui_gif
    MediaKind.VIDEO -> R.string.ui_video
    MediaKind.UNKNOWN -> R.string.ui_file
})

fun SortOrder.uiLabel(): String = t(when (this) {
    SortOrder.NEWEST -> R.string.ui_recent
    SortOrder.SCORE -> R.string.ui_top_rated
    SortOrder.OLDEST -> R.string.ui_oldest
})

fun ThemeMode.uiLabel(): String = t(when (this) {
    ThemeMode.SYSTEM -> R.string.ui_system
    ThemeMode.LIGHT -> R.string.ui_light
    ThemeMode.DARK -> R.string.ui_dark
})

internal fun Throwable.localizedError(): String = t(when ((this as? BooruException)?.kind) {
    ErrorKind.CREDENTIALS -> R.string.ui_check_this_source_s_user_and_api_key
    ErrorKind.DENIED -> R.string.ui_access_denied_check_your_account_permissions_no_automatic
    ErrorKind.RATE_LIMIT -> R.string.ui_the_server_requested_a_pause_wait_before_retrying
    ErrorKind.CONNECTION -> R.string.ui_could_not_connect_check_your_connection_and_try
    ErrorKind.SERVER -> R.string.ui_the_server_is_currently_unavailable
    ErrorKind.INVALID_RESPONSE -> R.string.ui_the_server_response_is_not_in_the_expected
    ErrorKind.UNSUPPORTED -> R.string.ui_this_source_does_not_provide_the_required_api
    else -> R.string.ui_the_operation_could_not_be_completed_try_again
})

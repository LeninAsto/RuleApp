package com.leninasto.ruleapp.domain

data class AppVersion(val major: Int, val minor: Int, val patch: Int, val beta: Boolean) : Comparable<AppVersion> {
    override fun compareTo(other: AppVersion): Int = compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch }, { if (it.beta) 0 else 1 })
    companion object {
        fun parse(text: String): AppVersion? {
            val match = Regex("(\\d{1,3})\\.(\\d{1,3})(?:\\.(\\d{1,3}))?(_beta)?").matchEntire(text.trim()) ?: return null
            return AppVersion(match.groupValues[1].toInt(), match.groupValues[2].toInt(), match.groupValues[3].toIntOrNull() ?: 0, match.groupValues[4].isNotEmpty())
        }
    }
}

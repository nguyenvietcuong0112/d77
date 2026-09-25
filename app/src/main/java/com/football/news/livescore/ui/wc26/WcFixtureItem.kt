package com.football.news.livescore.ui.wc26

import com.football.news.livescore.data.remote.model.MatchItemDto

sealed class WcFixtureItem {
    data class HeaderItem(val dateText: String) : WcFixtureItem()
    data class MatchItem(val match: MatchItemDto) : WcFixtureItem()
    data class AdItem(val adId: String) : WcFixtureItem()
    object EmptyItem : WcFixtureItem()
}

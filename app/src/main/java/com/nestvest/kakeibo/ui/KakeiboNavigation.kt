package com.nestvest.kakeibo.ui

sealed interface KakeiboDestination {
    data object Home : KakeiboDestination
    data object Learning : KakeiboDestination
    data object MonthlyPlan : KakeiboDestination
    data object WeeklyPacing : KakeiboDestination
    data object PlanReview : KakeiboDestination
    data object Progress : KakeiboDestination
    data class CategoryDetail(val categoryCode: String) : KakeiboDestination
    data object Reflection : KakeiboDestination
    data object SavedReflection : KakeiboDestination
}

data class KakeiboNavigationState(
    val backStack: List<KakeiboDestination> = listOf(KakeiboDestination.Home),
) {
    val current: KakeiboDestination get() = backStack.last()

    fun navigate(destination: KakeiboDestination): KakeiboNavigationState =
        copy(backStack = backStack + destination)

    fun back(): KakeiboNavigationState =
        if (backStack.size > 1) copy(backStack = backStack.dropLast(1)) else this

    fun replace(destination: KakeiboDestination): KakeiboNavigationState =
        copy(backStack = backStack.dropLast(1) + destination)
}

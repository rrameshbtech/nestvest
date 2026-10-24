package com.nestvest.kakeibo.domain

typealias KakeiboCategory = com.nestvest.expense.domain.KakeiboCategory

val canonicalKakeiboCategories: List<KakeiboCategory> =
    com.nestvest.expense.domain.KakeiboCategory.entries.toList()

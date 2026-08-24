package com.moneytracker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Lightweight, Reusable Cascading Orchestrator State for Hierarchical Comboboxes.
 * Encapsulates:
 * 1. 3-Tier State Management (Category -> SubCategory -> Detail / Item)
 * 2. Active Parent Matching & Identity Resolution
 * 3. Dynamic Filtering Predicates
 * 4. Autonomous Cascading Parent Tracking Keys
 */
class CascadingComboboxOrchestrator<TCat, TSub, TDet>(
    val categories: List<TCat>,
    val subCategories: List<TSub>,
    val details: List<TDet>,
    val catToText: (TCat) -> String,
    val subToText: (TSub) -> String,
    val detToText: (TDet) -> String,
    val subBelongsToCat: (TSub, TCat?, String) -> Boolean,
    val detBelongsToSub: (TDet, TSub?, String, String) -> Boolean,
    initialCategory: String = "",
    initialSubCategory: String = "",
    initialDetail: String = ""
) {
    var categoryText by mutableStateOf(initialCategory)
    var subCategoryText by mutableStateOf(initialSubCategory)
    var detailText by mutableStateOf(initialDetail)

    val matchedCategory: TCat?
        get() = categories.firstOrNull { catToText(it).equals(categoryText.trim(), ignoreCase = true) }

    val matchedSubCategory: TSub?
        get() = subCategories.firstOrNull {
            subToText(it).equals(subCategoryText.trim(), ignoreCase = true) &&
            subBelongsToCat(it, matchedCategory, categoryText)
        }

    val matchedDetail: TDet?
        get() = details.firstOrNull {
            detToText(it).equals(detailText.trim(), ignoreCase = true) &&
            detBelongsToSub(it, matchedSubCategory, subCategoryText, categoryText)
        }

    fun isSubCategoryValid(sub: TSub): Boolean = subBelongsToCat(sub, matchedCategory, categoryText)
    fun isDetailValid(det: TDet): Boolean = detBelongsToSub(det, matchedSubCategory, subCategoryText, categoryText)
}

/**
 * Factory composable to create and remember a CascadingComboboxOrchestrator.
 */
@Composable
fun <TCat, TSub, TDet> rememberCascadingComboboxOrchestrator(
    categories: List<TCat>,
    subCategories: List<TSub>,
    details: List<TDet>,
    catToText: (TCat) -> String,
    subToText: (TSub) -> String,
    detToText: (TDet) -> String,
    subBelongsToCat: (TSub, TCat?, String) -> Boolean,
    detBelongsToSub: (TDet, TSub?, String, String) -> Boolean,
    initialCategory: String = "",
    initialSubCategory: String = "",
    initialDetail: String = ""
): CascadingComboboxOrchestrator<TCat, TSub, TDet> {
    return remember(categories, subCategories, details) {
        CascadingComboboxOrchestrator(
            categories = categories,
            subCategories = subCategories,
            details = details,
            catToText = catToText,
            subToText = subToText,
            detToText = detToText,
            subBelongsToCat = subBelongsToCat,
            detBelongsToSub = detBelongsToSub,
            initialCategory = initialCategory,
            initialSubCategory = initialSubCategory,
            initialDetail = initialDetail
        )
    }
}

/**
 * Simplified String-based Orchestration State for screens dealing directly with String items.
 */
class SimpleStringComboboxOrchestrator(
    val categories: List<String>,
    val subCategories: List<String>,
    val details: List<String>,
    val subCatFilterMap: Map<String, List<String>> = emptyMap(),
    val detailFilterMap: Map<String, List<String>> = emptyMap(),
    initialCategory: String = "",
    initialSubCategory: String = "",
    initialDetail: String = ""
) {
    var categoryText by mutableStateOf(initialCategory)
    var subCategoryText by mutableStateOf(initialSubCategory)
    var detailText by mutableStateOf(initialDetail)

    fun isSubCategoryValid(sub: String): Boolean {
        if (categoryText.isBlank()) return true
        val allowed = subCatFilterMap[categoryText] ?: return true
        return allowed.contains(sub)
    }

    fun isDetailValid(det: String): Boolean {
        if (subCategoryText.isBlank()) return true
        val allowed = detailFilterMap[subCategoryText] ?: return true
        return allowed.contains(det)
    }
}

@Composable
fun rememberSimpleStringComboboxOrchestrator(
    categories: List<String>,
    subCategories: List<String>,
    details: List<String>,
    subCatFilterMap: Map<String, List<String>> = emptyMap(),
    detailFilterMap: Map<String, List<String>> = emptyMap(),
    initialCategory: String = "",
    initialSubCategory: String = "",
    initialDetail: String = ""
): SimpleStringComboboxOrchestrator {
    return remember(categories, subCategories, details, subCatFilterMap, detailFilterMap) {
        SimpleStringComboboxOrchestrator(
            categories = categories,
            subCategories = subCategories,
            details = details,
            subCatFilterMap = subCatFilterMap,
            detailFilterMap = detailFilterMap,
            initialCategory = initialCategory,
            initialSubCategory = initialSubCategory,
            initialDetail = initialDetail
        )
    }
}

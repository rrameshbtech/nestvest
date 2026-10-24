package com.nestvest.kakeibo.domain

data class PlanAllocationSummary(
    val categoryTotalMinorUnits: Long,
    val savingsIntentionMinorUnits: Long,
    val totalAllocationMinorUnits: Long,
    val availableMinorUnits: Long?,
    val unassignedRemainderMinorUnits: Long?,
)

fun allocationSummary(plan: MonthlyPlan): PlanAllocationSummary {
    val categoryTotal = plan.categoryTotalMinorUnits
    val total = Math.addExact(categoryTotal, plan.savingsIntentionMinorUnits)
    val remainder = plan.availableMinorUnits?.let { Math.subtractExact(it, total) }
    return PlanAllocationSummary(
        categoryTotalMinorUnits = categoryTotal,
        savingsIntentionMinorUnits = plan.savingsIntentionMinorUnits,
        totalAllocationMinorUnits = total,
        availableMinorUnits = plan.availableMinorUnits,
        unassignedRemainderMinorUnits = remainder,
    )
}

package com.example.tdminsight

enum class Workflow { PRE, POST, PRE_POST }
enum class Sex { MALE, FEMALE }

data class TdmInput(
    val sex: Sex,
    val weightKg: Double,
    val ageYears: Double,
    val serumCreatinineMgDl: Double,
    val doseMg: Double,
    val intervalHours: Double,
    val infusionHours: Double,
    val preMgL: Double? = null,
    val postMgL: Double? = null,
    val postSamplingHoursAfterInfusion: Double? = null,
    val preToPostHours: Double? = null,
    val desiredCmaxMgL: Double? = null
)

data class TdmResult(
    val workflow: Workflow,
    val crclMlMin: Double,
    val kePerHour: Double,
    val halfLifeHours: Double,
    val vdL: Double,
    val cmaxMgL: Double,
    val cminMgL: Double,
    val auc24MgHrL: Double?,
    val newDoseMg: Double?,
    val expectedCmaxMgL: Double?,
    val expectedCminMgL: Double?,
    val explanation: List<String>
)

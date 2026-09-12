package com.example.tdminsight

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UiState(
    val workflow: Workflow = Workflow.PRE_POST,
    val sex: Sex = Sex.MALE,
    val age: String = "19",
    val weight: String = "70",
    val scr: String = "0.72",
    val dose: String = "750",
    val interval: String = "12",
    val infusion: String = "1",
    val pre: String = "15.9",
    val post: String = "29.3",
    val postTime: String = "1",
    val preToPost: String = "2.5",
    val targetCmax: String = "",
    val result: TdmResult? = null,
    val error: String? = null
)

class TdmViewModel : ViewModel() {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()
    private fun update(f: UiState.() -> UiState) { _state.value = _state.value.f().copy(error = null) }

    fun setWorkflow(v: Workflow) = update { copy(workflow = v, result = null) }
    fun sex(v: Sex) = update { copy(sex = v) }
    fun age(v: String) = update { copy(age = v) }
    fun weight(v: String) = update { copy(weight = v) }
    fun scr(v: String) = update { copy(scr = v) }
    fun dose(v: String) = update { copy(dose = v) }
    fun interval(v: String) = update { copy(interval = v) }
    fun infusion(v: String) = update { copy(infusion = v) }
    fun pre(v: String) = update { copy(pre = v) }
    fun post(v: String) = update { copy(post = v) }
    fun postTime(v: String) = update { copy(postTime = v) }
    fun preToPost(v: String) = update { copy(preToPost = v) }
    fun targetCmax(v: String) = update { copy(targetCmax = v) }

    fun reset() {
        _state.value = UiState()
    }

    fun calculate() {
        try {
            val s = _state.value
            fun d(x: String) = x.trim().toDouble()
            val input = TdmInput(
                sex = s.sex,
                ageYears = d(s.age),
                weightKg = d(s.weight),
                serumCreatinineMgDl = d(s.scr),
                doseMg = d(s.dose),
                intervalHours = d(s.interval),
                infusionHours = d(s.infusion),
                preMgL = s.pre.takeIf { it.isNotBlank() }?.let(::d),
                postMgL = s.post.takeIf { it.isNotBlank() }?.let(::d),
                postSamplingHoursAfterInfusion = s.postTime.takeIf { it.isNotBlank() }?.let(::d),
                preToPostHours = s.preToPost.takeIf { it.isNotBlank() }?.let(::d),
                desiredCmaxMgL = s.targetCmax.takeIf { it.isNotBlank() }?.let(::d)
            )
            val result = CalculationEngine.calculate(input, s.workflow)
            _state.value = s.copy(result = result, error = null)
        } catch (e: Exception) {
            _state.value = _state.value.copy(result = null, error = e.message ?: "Invalid input.")
        }
    }
}

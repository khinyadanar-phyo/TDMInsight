package com.example.tdminsight

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

object CalculationEngine {
    private fun positive(name: String, x: Double) {
        require(x.isFinite() && x > 0.0) { "$name must be greater than 0." }
    }

    private fun ibw(input: TdmInput): Double {
        // Devine IBW; used only when weight selection is extended.
        return if (input.sex == Sex.MALE) {
            50.0 + 2.3 * max(0.0, (170.0 - 152.4) / 2.54)
        } else {
            45.5 + 2.3 * max(0.0, (170.0 - 152.4) / 2.54)
        }
    }

    private fun crcl(input: TdmInput): Double {
        // Cockcroft-Gault with actual body weight for this academic prototype.
        // SCr is in mg/dL. This follows the common drug-dosing form.
        val base = ((140.0 - input.ageYears) * input.weightKg) /
                (72.0 * input.serumCreatinineMgDl)
        return if (input.sex == Sex.FEMALE) base * 0.85 else base
    }

    private fun populationKe(crcl: Double): Double =
        0.0044 + (crcl * 0.00083)

    fun calculate(input: TdmInput, workflow: Workflow): TdmResult {
        require(input.ageYears >= 18) { "This prototype is restricted to adults (18+)." }
        positive("Weight", input.weightKg)
        positive("Age", input.ageYears)
        positive("Serum creatinine", input.serumCreatinineMgDl)
        positive("Dose", input.doseMg)
        positive("Interval", input.intervalHours)
        positive("Infusion time", input.infusionHours)
        require(input.infusionHours < input.intervalHours) {
            "Infusion time must be shorter than the dosing interval."
        }
        val crcl = crcl(input)
        require(crcl.isFinite() && crcl > 0) { "Calculated creatinine clearance is invalid." }
        val vd = 0.7 * input.weightKg

        return when (workflow) {
            Workflow.PRE -> pre(input, crcl, vd)
            Workflow.POST -> post(input, crcl, vd)
            Workflow.PRE_POST -> prePost(input, crcl, vd)
        }
    }

    private fun pre(input: TdmInput, crcl: Double, vd: Double): TdmResult {
        val cmin = requireNotNull(input.preMgL) { "Pre concentration is required." }
        positive("Pre concentration", cmin)
        val ke = populationKe(crcl)
        val cmax = cmin / exp(-ke * input.intervalHours)
        val half = 0.693 / ke
        return TdmResult(
            Workflow.PRE, crcl, ke, half, vd, cmax, cmin, null, null, cmax, cmin,
            listOf(
                "CrCl = ${fmt(crcl)} mL/min.",
                "Ke = 0.0044 + (0.00083 × CrCl) = ${fmt(ke)} h⁻¹.",
                "Vd = 0.7 L/kg × ${fmt(input.weightKg)} kg = ${fmt(vd)} L.",
                "Cmax = Cmin / e⁻(Ke×T) = ${fmt(cmax)} mg/L.",
                "Cmin = pre-dose concentration = ${fmt(cmin)} mg/L."
            )
        )
    }

    private fun post(input: TdmInput, crcl: Double, vd: Double): TdmResult {
        val cpost = requireNotNull(input.postMgL) { "Post concentration is required." }
        val t = requireNotNull(input.postSamplingHoursAfterInfusion) {
            "Post sampling time after complete infusion is required."
        }
        positive("Post concentration", cpost)
        require(t >= 0) { "Post sampling time cannot be negative." }
        val ke = populationKe(crcl)
        val cmax = cpost * exp(ke * t)
        val cmin = cmax * exp(-ke * input.intervalHours)
        val half = 0.693 / ke
        val newDose = input.desiredCmaxMgL?.let {
            positive("Desired Cmax", it)
            it * vd * (1 - exp(-ke * input.intervalHours))
        }
        val expMax = newDose?.let { it / (vd * (1 - exp(-ke * input.intervalHours))) }
        val expMin = expMax?.let { it * exp(-ke * input.intervalHours) }

        return TdmResult(
            Workflow.POST, crcl, ke, half, vd, cmax, cmin, null, newDose, expMax, expMin,
            listOf(
                "CrCl = ${fmt(crcl)} mL/min.",
                "Ke = 0.0044 + (0.00083 × CrCl) = ${fmt(ke)} h⁻¹.",
                "Vd = 0.7 L/kg × ${fmt(input.weightKg)} kg = ${fmt(vd)} L.",
                "Cmax = Cpost × e^(Ke×t) = ${fmt(cmax)} mg/L.",
                "Cmin = Cmax × e⁻(Ke×T) = ${fmt(cmin)} mg/L."
            ) + listOfNotNull(
                newDose?.let { "New dose = Desired Cmax × Vd × (1 − e⁻(Ke×T)) = ${fmt(it)} mg." }
            )
        )
    }

    private fun prePost(input: TdmInput, crcl: Double, vd: Double): TdmResult {
        val pre = requireNotNull(input.preMgL) { "Pre concentration is required." }
        val post = requireNotNull(input.postMgL) { "Post concentration is required." }
        val tPrime = requireNotNull(input.postSamplingHoursAfterInfusion) {
            "Post sampling time after complete infusion is required."
        }
        val delta = requireNotNull(input.preToPostHours) {
            "Time from pre sampling to post sampling is required."
        }
        positive("Pre concentration", pre)
        positive("Post concentration", post)
        require(tPrime >= 0) { "Post sampling time cannot be negative." }
        require(delta > 0 && delta < input.intervalHours) {
            "Pre-to-post time must be > 0 and < dosing interval."
        }
        val ke = (ln(post) - ln(pre)) / (input.intervalHours - delta)
        require(ke.isFinite() && ke > 0) {
            "Calculated Ke is invalid. Check concentration values and sampling times."
        }
        val half = 0.693 / ke
        val cmax = post * exp(ke * tPrime)
        val cmin = cmax * exp(-ke * input.intervalHours)
        val aucSingle = (cmax - cmin) / ke
        val auc24 = aucSingle * (24.0 / input.intervalHours)

        val newDose = input.desiredCmaxMgL?.let {
            positive("Desired Cmax", it)
            it * vd * (1 - exp(-ke * input.intervalHours))
        }
        val expMax = newDose?.let { it / (vd * (1 - exp(-ke * input.intervalHours))) }
        val expMin = expMax?.let { it * exp(-ke * input.intervalHours) }

        return TdmResult(
            Workflow.PRE_POST, crcl, ke, half, vd, cmax, cmin, auc24, newDose, expMax, expMin,
            listOf(
                "CrCl = ${fmt(crcl)} mL/min.",
                "Ke = [ln(Cpost) − ln(Cpre)] / [T − (t2 − t1)] = ${fmt(ke)} h⁻¹.",
                "Half-life = 0.693 / Ke = ${fmt(half)} h.",
                "Cmax = Cpost × e^(Ke×t′) = ${fmt(cmax)} mg/L.",
                "Cmin = Cmax × e⁻(Ke×T) = ${fmt(cmin)} mg/L.",
                "Vd = 0.7 L/kg × ${fmt(input.weightKg)} kg = ${fmt(vd)} L.",
                "AUC24 = [(Cmax − Cmin) / Ke] × (24/T) = ${fmt(auc24)} mg·h/L."
            ) + listOfNotNull(
                newDose?.let { "New dose = Desired Cmax × Vd × (1 − e⁻(Ke×T)) = ${fmt(it)} mg." }
            )
        )
    }

    private fun fmt(x: Double) = "%.3f".format(x)
}

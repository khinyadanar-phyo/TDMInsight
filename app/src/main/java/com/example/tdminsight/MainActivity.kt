package com.example.tdminsight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private enum class AppPage { HOME, METHOD, INPUT, RESULT }

private val HospitalBlue = androidx.compose.ui.graphics.Color(0xFF1565C0)
private val HospitalTeal = androidx.compose.ui.graphics.Color(0xFF00897B)
private val SoftBlue = androidx.compose.ui.graphics.Color(0xFFEAF3FF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TdmApp() }
    }
}

@Composable
fun TdmApp(vm: TdmViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var page by remember { mutableStateOf(AppPage.HOME) }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = HospitalBlue,
            secondary = HospitalTeal,
            background = androidx.compose.ui.graphics.Color(0xFFF7F9FC),
            surface = androidx.compose.ui.graphics.Color.White
        )
    ) {
        Scaffold(
            topBar = {
                HospitalTopBar(
                    page = page,
                    onBack = { page = when (page) {
                        AppPage.HOME -> AppPage.HOME
                        AppPage.METHOD -> AppPage.HOME
                        AppPage.INPUT -> AppPage.METHOD
                        AppPage.RESULT -> AppPage.INPUT
                    }}
                )
            }
        ) { pad ->
            AnimatedContent(
                targetState = page,
                modifier = Modifier.padding(pad).fillMaxSize(),
                transitionSpec = {
                    (slideInHorizontally { it } + fadeIn()).togetherWith(
                        slideOutHorizontally { -it / 3 } + fadeOut()
                    )
                },
                label = "pageTransition"
            ) { current ->
                when (current) {
                    AppPage.HOME -> HomePage { page = AppPage.METHOD }
                    AppPage.METHOD -> MethodPage(
                        workflow = state.workflow,
                        setWorkflow = vm::setWorkflow,
                        onNext = { page = AppPage.INPUT }
                    )
                    AppPage.INPUT -> InputPage(
                        state = state,
                        vm = vm,
                        onCalculate = {
                            vm.calculate()
                            page = AppPage.RESULT
                        },
                        onBack = { page = AppPage.METHOD }
                    )
                    AppPage.RESULT -> ResultPage(
                        result = state.result,
                        error = state.error,
                        onStartOver = {
                            vm.reset()
                            page = AppPage.HOME
                        },
                        onBack = { page = AppPage.INPUT }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HospitalTopBar(page: AppPage, onBack: () -> Unit) {
    TopAppBar(
        title = {
            Column {
                Text("TDM Insight", fontWeight = FontWeight.Bold)
                Text("Vancomycin Therapeutic Drug Monitoring",
                    style = MaterialTheme.typography.labelSmall)
            }
        },
        navigationIcon = {
            if (page != AppPage.HOME) {
                IconButton(onClick = onBack) { Text("‹", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimary) }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
private fun HomePage(onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(38.dp))
        Box(
            Modifier.size(104.dp).clip(RoundedCornerShape(28.dp))
                .background(SoftBlue),
            contentAlignment = Alignment.Center
        ) {
            Text("RX", color = HospitalBlue, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
        Text("TDM Insight", style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold, color = HospitalBlue)
        Text("Vancomycin Calculation Assistant",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        Text(
            "A four-step academic workflow for entering patient data, selecting a sampling method, and reviewing vancomycin TDM calculations.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(30.dp))
        InfoCard(
            icon = "✚",
            title = "Hospital-style workflow",
            text = "Method → patient & medication data → sampling data → calculation result"
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Go to Vancomycin Calculation Method",
                fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Academic prototype • Fictional cases only",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MethodPage(
    workflow: Workflow,
    setWorkflow: (Workflow) -> Unit,
    onNext: () -> Unit
) {
    PageContainer {
        StepHeader(2, "Select Calculation Method",
            "Choose the vancomycin sampling approach used for this calculation.")

        MethodCard(
            selected = workflow == Workflow.PRE,
            title = "Pre-dose (Trough)",
            subtitle = "Uses a pre-dose concentration.",
            icon = "PRE",
            onClick = { setWorkflow(Workflow.PRE) }
        )
        MethodCard(
            selected = workflow == Workflow.POST,
            title = "Post-dose",
            subtitle = "Uses a concentration sampled after infusion.",
            icon = "POST",
            onClick = { setWorkflow(Workflow.POST) }
        )
        MethodCard(
            selected = workflow == Workflow.PRE_POST,
            title = "Pre + Post",
            subtitle = "Uses both pre-dose and post-dose concentrations.",
            icon = "TDM",
            onClick = { setWorkflow(Workflow.PRE_POST) }
        )

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Next", fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text("→")
        }
    }
}

@Composable
private fun InputPage(
    state: UiState,
    vm: TdmViewModel,
    onCalculate: () -> Unit,
    onBack: () -> Unit
) {
    PageContainer {
        StepHeader(3, "Patient & Sampling Data",
            "Enter all required information before calculating the result.")

        InputSection("Patient Parameters", "PATIENT") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppField(state.age, vm::age, "Age (years)", Modifier.weight(1f))
                AppField(state.weight, vm::weight, "Weight (kg)", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppField(state.scr, vm::scr, "Serum creatinine (mg/dL)", Modifier.weight(1f))
                SexSelector(state.sex, vm::sex)
            }
        }

        InputSection("Medication", "RX") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppField(state.dose, vm::dose, "Dose (mg)", Modifier.weight(1f))
                AppField(state.interval, vm::interval, "Interval (h)", Modifier.weight(1f))
            }
            AppField(state.infusion, vm::infusion, "Infusion time (h)", Modifier.fillMaxWidth())
        }

        InputSection("Sampling Data", "LAB") {
            if (state.workflow != Workflow.POST) {
                AppField(state.pre, vm::pre, "Pre concentration (mg/L)", Modifier.fillMaxWidth())
            }
            if (state.workflow != Workflow.PRE) {
                AppField(state.post, vm::post, "Post concentration (mg/L)", Modifier.fillMaxWidth())
                AppField(state.postTime, vm::postTime,
                    "Post sample after complete infusion (h)", Modifier.fillMaxWidth())
            }
            if (state.workflow == Workflow.PRE_POST) {
                AppField(state.preToPost, vm::preToPost,
                    "Time from pre sample to post sample (h)", Modifier.fillMaxWidth())
            }
            if (state.workflow != Workflow.PRE) {
                AppField(state.targetCmax, vm::targetCmax,
                    "Desired Cmax (mg/L), optional", Modifier.fillMaxWidth())
            }
        }

        state.error?.let {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(it, modifier = Modifier.padding(14.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }

        Button(
            onClick = onCalculate,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Calculation Result", fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Back to Method") }

        Disclaimer()
    }
}

@Composable
private fun ResultPage(
    result: TdmResult?,
    error: String?,
    onStartOver: () -> Unit,
    onBack: () -> Unit
) {
    PageContainer {
        StepHeader(4, "Calculation Result",
            "Review the calculated parameters and the calculation pathway.")

        if (result == null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text(error ?: "No result available.",
                        color = MaterialTheme.colorScheme.error)
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SoftBlue)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Calculated Results", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, color = HospitalBlue)
                    Spacer(Modifier.height(10.dp))
                    Metric("Creatinine clearance", result.crclMlMin, "mL/min")
                    Metric("Elimination rate (Ke)", result.kePerHour, "h⁻¹")
                    Metric("Half-life", result.halfLifeHours, "h")
                    Metric("Volume of distribution", result.vdL, "L")
                    Metric("Cmax", result.cmaxMgL, "mg/L")
                    Metric("Cmin", result.cminMgL, "mg/L")
                    result.auc24MgHrL?.let { Metric("AUC24", it, "mg·h/L") }
                    result.newDoseMg?.let {
                        Spacer(Modifier.height(8.dp))
                        Text("Calculated dose adjustment", fontWeight = FontWeight.Bold)
                        Metric("Suggested new dose", it, "mg")
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text("Calculation Explanation",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            result.explanation.forEachIndexed { index, text ->
                ExplanationRow(index + 1, text)
            }

            Spacer(Modifier.height(12.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Method used", fontWeight = FontWeight.Bold)
                    Text(
                        when (result.workflow) {
                            Workflow.PRE -> "Pre-dose concentration"
                            Workflow.POST -> "Post-dose concentration"
                            Workflow.PRE_POST -> "Pre + post concentrations"
                        },
                        color = HospitalBlue
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onStartOver,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("⌂")
            Spacer(Modifier.width(10.dp))
            Text("Go back to the Start", fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Back to Patient Data") }

        Disclaimer()
    }
}

@Composable
private fun PageContainer(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
private fun StepHeader(step: Int, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(HospitalBlue),
            contentAlignment = Alignment.Center
        ) {
            Text("$step", color = androidx.compose.ui.graphics.Color.White,
                fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun InfoCard(icon: String,
                     title: String, text: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, color = HospitalTeal, fontWeight = FontWeight.Bold, modifier = Modifier.widthIn(min = 42.dp))
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(text, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun MethodCard(selected: Boolean, title: String, subtitle: String,
                       icon: String,
                       onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) SoftBlue else MaterialTheme.colorScheme.surface
        ),
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, HospitalBlue) else null
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = null)
            Text(icon, color = HospitalBlue, fontWeight = FontWeight.Bold, modifier = Modifier.widthIn(min = 48.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun InputSection(title: String, icon: String,
                         content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, color = HospitalTeal, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}

@Composable
private fun AppField(value: String, onValueChange: (String) -> Unit,
                    label: String, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun SexSelector(sex: Sex, set: (Sex) -> Unit) {
    Column(Modifier.widthIn(min = 135.dp)) {
        Text("Sex", style = MaterialTheme.typography.labelMedium)
        Row {
            FilterChip(selected = sex == Sex.MALE,
                onClick = { set(Sex.MALE) }, label = { Text("Male") })
            Spacer(Modifier.width(5.dp))
            FilterChip(selected = sex == Sex.FEMALE,
                onClick = { set(Sex.FEMALE) }, label = { Text("Female") })
        }
    }
}

@Composable
private fun ExplanationRow(number: Int, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(27.dp).clip(RoundedCornerShape(8.dp)).background(SoftBlue),
            contentAlignment = Alignment.Center
        ) { Text("$number", color = HospitalBlue, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.width(10.dp))
        Text(text, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Metric(name: String, value: Double, unit: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(name)
        Text("%.3f %s".format(value, unit), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Disclaimer() {
    Text(
        "Educational prototype only. Use fictional cases and lecturer-approved equations. Not a clinically validated prescribing or treatment-decision system.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    )
}

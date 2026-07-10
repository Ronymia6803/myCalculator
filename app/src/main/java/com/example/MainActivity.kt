package com.example

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.HistoryEntry
import com.example.ui.CalculatorViewModel
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    CalculatorScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun CalculatorScreen(
    modifier: Modifier = Modifier,
    viewModel: CalculatorViewModel = viewModel()
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    val context = LocalContext.current

    val historyList by viewModel.historyList.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF11141A),
                        Color(0xFF1E2330)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Display Area (Top Portion)
            CalculatorDisplay(
                expression = viewModel.expression,
                previewResult = viewModel.previewResult,
                isDegree = viewModel.isDegree,
                onToggleDegree = {
                    triggerVibration(context)
                    viewModel.onToggleDegree()
                },
                onHistoryClick = {
                    triggerVibration(context)
                    viewModel.showHistory = true
                },
                modifier = Modifier
                    .weight(if (isLandscape) 1.2f else 1.5f)
                    .fillMaxWidth()
            )

            HorizontalDivider(color = Color(0xFF2C3545), thickness = 1.dp)

            // Keypad Area (Bottom Portion)
            if (isLandscape) {
                // Wide / Tablet Layout: Standard and Scientific Side-by-Side
                Row(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ScientificKeypad(
                        onFunctionClick = { viewModel.onFunctionClick(it) },
                        onConstantClick = { viewModel.onConstantClick(it) },
                        isDegree = viewModel.isDegree,
                        modifier = Modifier.weight(1f)
                    )
                    StandardKeypad(
                        onDigitClick = { viewModel.onDigitClick(it) },
                        onDecimalClick = { viewModel.onDecimalClick() },
                        onOperatorClick = { viewModel.onOperatorClick(it) },
                        onParenthesisClick = { viewModel.onParenthesisClick(it) },
                        onClearClick = { viewModel.onClearClick() },
                        onBackspaceClick = { viewModel.onBackspaceClick() },
                        onEqualClick = { viewModel.onEqualClick() },
                        isScientificExpanded = true,
                        onToggleScientific = { /* Hidden in landscape since both are visible */ },
                        modifier = Modifier.weight(1.3f)
                    )
                }
            } else {
                // Portrait Layout: Stacked, with Scientific panel collapsible
                Column(
                    modifier = Modifier
                        .weight(3f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Quick Action Row for Expand/Collapse scientific panel and Backspace
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                triggerVibration(context)
                                viewModel.isScientificExpanded = !viewModel.isScientificExpanded
                            },
                            modifier = Modifier.testTag("scientific_toggle")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .background(
                                        color = if (viewModel.isScientificExpanded) Color(0xFF00ADB5).copy(alpha = 0.2f) else Color(0xFF222831),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Science,
                                    contentDescription = "Scientific Functions",
                                    tint = if (viewModel.isScientificExpanded) Color(0xFF00ADB5) else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "SCI",
                                    color = if (viewModel.isScientificExpanded) Color(0xFF00ADB5) else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = if (viewModel.isScientificExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                    contentDescription = null,
                                    tint = if (viewModel.isScientificExpanded) Color(0xFF00ADB5) else Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                triggerVibration(context)
                                viewModel.onBackspaceClick()
                            },
                            modifier = Modifier.testTag("backspace_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Delete",
                                tint = Color(0xFFFF5722),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = viewModel.isScientificExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        ScientificKeypad(
                            onFunctionClick = { viewModel.onFunctionClick(it) },
                            onConstantClick = { viewModel.onConstantClick(it) },
                            isDegree = viewModel.isDegree,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                    }

                    StandardKeypad(
                        onDigitClick = { viewModel.onDigitClick(it) },
                        onDecimalClick = { viewModel.onDecimalClick() },
                        onOperatorClick = { viewModel.onOperatorClick(it) },
                        onParenthesisClick = { viewModel.onParenthesisClick(it) },
                        onClearClick = { viewModel.onClearClick() },
                        onBackspaceClick = { viewModel.onBackspaceClick() },
                        onEqualClick = { viewModel.onEqualClick() },
                        isScientificExpanded = viewModel.isScientificExpanded,
                        onToggleScientific = { viewModel.isScientificExpanded = !viewModel.isScientificExpanded },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Calculation History Overlay Panel
        AnimatedVisibility(
            visible = viewModel.showHistory,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            HistoryPanel(
                historyList = historyList,
                onItemClick = { entry ->
                    triggerVibration(context)
                    viewModel.onHistoryItemClick(entry)
                },
                onClearHistory = {
                    triggerVibration(context)
                    viewModel.onClearHistory()
                },
                onDeleteEntry = { id ->
                    triggerVibration(context)
                    viewModel.onDeleteHistoryEntry(id)
                },
                onClose = {
                    triggerVibration(context)
                    viewModel.showHistory = false
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun CalculatorDisplay(
    expression: String,
    previewResult: String,
    isDegree: Boolean,
    onToggleDegree: () -> Unit,
    onHistoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Mode & Action Bar (DEG/RAD toggle and history)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // DEG / RAD Toggle Pill
            Surface(
                onClick = onToggleDegree,
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF222831),
                modifier = Modifier
                    .height(36.dp)
                    .testTag("deg_rad_toggle")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isDegree) Color(0xFF00ADB5) else Color(0xFFFF5722))
                    )
                    Text(
                        text = if (isDegree) "DEG" else "RAD",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // History Button
            IconButton(
                onClick = onHistoryClick,
                modifier = Modifier
                    .background(Color(0xFF222831), shape = CircleShape)
                    .size(36.dp)
                    .testTag("history_toggle_button")
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Show History",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Active Calculation Readout
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Expression Text (grows/shrinks text size dynamically to avoid truncation)
            Text(
                text = expression.ifEmpty { "0" },
                color = Color.White,
                fontSize = if (expression.length > 15) 28.sp else 42.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.End,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = if (expression.length > 15) 36.sp else 48.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calculation_expression_text")
            )

            // Real-time Preview Result Text
            AnimatedVisibility(
                visible = previewResult.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Text(
                    text = "= $previewResult",
                    color = Color(0xFF00ADB5),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calculation_preview_text")
                )
            }
        }
    }
}

@Composable
fun StandardKeypad(
    onDigitClick: (String) -> Unit,
    onDecimalClick: () -> Unit,
    onOperatorClick: (String) -> Unit,
    onParenthesisClick: (String) -> Unit,
    onClearClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    onEqualClick: () -> Unit,
    isScientificExpanded: Boolean,
    onToggleScientific: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val rowModifier = Modifier
            .weight(1f)
            .fillMaxWidth()

        // Row 1: AC, (, ), ÷
        Row(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CalculatorButton(
                text = "AC",
                category = ButtonCategory.Action,
                onClick = onClearClick,
                modifier = Modifier
                    .weight(1f)
                    .testTag("key_ac")
            )
            CalculatorButton(
                text = "(",
                category = ButtonCategory.Utility,
                onClick = { onParenthesisClick("(") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("key_paren_open")
            )
            CalculatorButton(
                text = ")",
                category = ButtonCategory.Utility,
                onClick = { onParenthesisClick(")") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("key_paren_close")
            )
            CalculatorButton(
                text = "÷",
                category = ButtonCategory.Operator,
                onClick = { onOperatorClick("÷") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("key_divide")
            )
        }

        // Row 2: 7, 8, 9, ×
        Row(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CalculatorButton(
                text = "7",
                category = ButtonCategory.Digit,
                onClick = { onDigitClick("7") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("key_7")
            )
            CalculatorButton(
                text = "8",
                category = ButtonCategory.Digit,
                onClick = { onDigitClick("8") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("key_8")
            )
            CalculatorButton(
                text = "9",
                category = ButtonCategory.Digit,
                onClick = { onDigitClick("9") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("key_9")
            )
            CalculatorButton(
                text = "×",
                category = ButtonCategory.Operator,
                onClick = { onOperatorClick("×") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("key_multiply")
            )
        }

        // Row 3: 4, 5, 6, -
        Row(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CalculatorButton(
                text = "4",
                category = ButtonCategory.Digit,
                onClick = { onDigitClick("4") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("key_4")
            )
            CalculatorButton(
                text = "5",
                category = ButtonCategory.Digit,
                onClick = { onDigitClick("5") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("key_5")
            )
            CalculatorButton(
                text = "6",
                category = ButtonCategory.Digit,
                onClick = { onDigitClick("6") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("key_6")
            )
            CalculatorButton(
                text = "-",
                category = ButtonCategory.Operator,
                onClick = { onOperatorClick("-") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("key_subtract")
            )
        }

        // Row 4: 1, 2, 3, +
        Row(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CalculatorButton(
                text = "1",
                category = ButtonCategory.Digit,
                onClick = { onDigitClick("1") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("key_1")
            )
            CalculatorButton(
                text = "2",
                category = ButtonCategory.Digit,
                onClick = { onDigitClick("2") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("key_2")
            )
            CalculatorButton(
                text = "3",
                category = ButtonCategory.Digit,
                onClick = { onDigitClick("3") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("key_3")
            )
            CalculatorButton(
                text = "+",
                category = ButtonCategory.Operator,
                onClick = { onOperatorClick("+") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("key_add")
            )
        }

        // Row 5: %, 0, ., =
        Row(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CalculatorButton(
                text = "%",
                category = ButtonCategory.Utility,
                onClick = { onOperatorClick("%") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("key_percent")
            )
            CalculatorButton(
                text = "0",
                category = ButtonCategory.Digit,
                onClick = { onDigitClick("0") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("key_0")
            )
            CalculatorButton(
                text = ".",
                category = ButtonCategory.Digit,
                onClick = onDecimalClick,
                modifier = Modifier
                    .weight(1f)
                    .testTag("key_dot")
            )
            CalculatorButton(
                text = "=",
                category = ButtonCategory.Equal,
                onClick = onEqualClick,
                modifier = Modifier
                    .weight(1f)
                    .testTag("key_equal")
            )
        }
    }
}

@Composable
fun ScientificKeypad(
    onFunctionClick: (String) -> Unit,
    onConstantClick: (String) -> Unit,
    isDegree: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val rowModifier = Modifier
            .weight(1f)
            .fillMaxWidth()

        // Scientific Row 1: sin, cos, tan, ^
        Row(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CalculatorButton(
                text = "sin",
                category = ButtonCategory.Scientific,
                onClick = { onFunctionClick("sin") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("key_sin")
            )
            CalculatorButton(
                text = "cos",
                category = ButtonCategory.Scientific,
                onClick = { onFunctionClick("cos") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("key_cos")
            )
            CalculatorButton(
                text = "tan",
                category = ButtonCategory.Scientific,
                onClick = { onFunctionClick("tan") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("key_tan")
            )
            CalculatorButton(
                text = "x^y",
                category = ButtonCategory.Scientific,
                onClick = { onConstantClick("^") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("key_power")
            )
        }

        // Scientific Row 2: ln, log, sqrt, π
        Row(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CalculatorButton(
                text = "ln",
                category = ButtonCategory.Scientific,
                onClick = { onFunctionClick("ln") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("key_ln")
            )
            CalculatorButton(
                text = "log",
                category = ButtonCategory.Scientific,
                onClick = { onFunctionClick("log") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("key_log")
            )
            CalculatorButton(
                text = "√",
                category = ButtonCategory.Scientific,
                onClick = { onFunctionClick("sqrt") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("key_sqrt")
            )
            CalculatorButton(
                text = "π",
                category = ButtonCategory.Scientific,
                onClick = { onConstantClick("π") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("key_pi")
            )
        }

        // Scientific Row 3: e, abs, asin, acos (Only shown in full landscape, or nested inside sci keypad)
        Row(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CalculatorButton(
                text = "e",
                category = ButtonCategory.Scientific,
                onClick = { onConstantClick("e") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("key_e")
            )
            CalculatorButton(
                text = "abs",
                category = ButtonCategory.Scientific,
                onClick = { onFunctionClick("abs") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("key_abs")
            )
            CalculatorButton(
                text = "asin",
                category = ButtonCategory.Scientific,
                onClick = { onFunctionClick("asin") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("key_asin")
            )
            CalculatorButton(
                text = "acos",
                category = ButtonCategory.Scientific,
                onClick = { onFunctionClick("acos") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("key_acos")
            )
        }
    }
}

enum class ButtonCategory {
    Digit, Operator, Scientific, Utility, Action, Equal
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalculatorButton(
    text: String,
    category: ButtonCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Smooth scaling press animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        label = "ButtonPressScale"
    )

    // Determine colors based on category
    val backgroundColor = when (category) {
        ButtonCategory.Digit -> Color(0xFF222831)
        ButtonCategory.Operator -> Color(0xFFFF9F1C)
        ButtonCategory.Scientific -> Color(0xFF1E2638)
        ButtonCategory.Utility -> Color(0xFF393E46)
        ButtonCategory.Action -> Color(0xFFFF5722)
        ButtonCategory.Equal -> Color(0xFF00ADB5)
    }

    val contentColor = when (category) {
        ButtonCategory.Scientific -> Color(0xFF00ADB5)
        else -> Color.White
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .scale(scale)
            .fillMaxHeight()
            .clip(RoundedCornerShape(22.dp))
            .background(backgroundColor)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = {
                    triggerVibration(context)
                    onClick()
                }
            )
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = if (text.length > 3) 14.sp else 22.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun HistoryPanel(
    historyList: List<HistoryEntry>,
    onItemClick: (HistoryEntry) -> Unit,
    onClearHistory: () -> Unit,
    onDeleteEntry: (Long) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sdf = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(onClick = onClose) // Click backdrop to close
    ) {
        Surface(
            color = Color(0xFF1E2330),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .clickable(enabled = false) { /* Prevent click through */ }
                .testTag("history_panel_container")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // History Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Calculation History",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (historyList.isNotEmpty()) {
                            IconButton(
                                onClick = onClearHistory,
                                modifier = Modifier.testTag("clear_history_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clear All History",
                                    tint = Color(0xFFFF5722)
                                )
                            }
                        }

                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .background(Color(0xFF2C3545), shape = CircleShape)
                                .size(32.dp)
                                .testTag("close_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close history panel",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (historyList.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = Color(0xFF5E6D82),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No history records yet",
                            color = Color(0xFF8E9EB2),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Calculations will show up here.",
                            color = Color(0xFF5E6D82),
                            fontSize = 12.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(historyList, key = { it.id }) { entry ->
                            HistoryItemRow(
                                entry = entry,
                                formattedTime = sdf.format(Date(entry.timestamp)),
                                onItemClick = { onItemClick(entry) },
                                onDeleteClick = { onDeleteEntry(entry.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItemRow(
    entry: HistoryEntry,
    formattedTime: String,
    onItemClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C3545).copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
            .testTag("history_item_${entry.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = formattedTime,
                    color = Color(0xFF8E9EB2),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = entry.expression,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "= ${entry.result}",
                    color = Color(0xFF00ADB5),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.testTag("delete_history_item_${entry.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete item",
                    tint = Color(0xFFFF5722).copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// Utility vibration method for tactile press responses
fun triggerVibration(context: Context) {
    try {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(15)
            }
        }
    } catch (e: Exception) {
        // Safe-guard against missing permission or platform restriction
    }
}

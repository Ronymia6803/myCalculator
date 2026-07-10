package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.CalculatorDatabase
import com.example.model.CalculatorRepository
import com.example.model.ExpressionEvaluator
import com.example.model.HistoryEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: CalculatorRepository

    init {
        val database = CalculatorDatabase.getDatabase(application)
        repository = CalculatorRepository(database.historyDao())
    }

    val historyList: StateFlow<List<HistoryEntry>> = repository.history.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    var expression by mutableStateOf("")
        private set

    var previewResult by mutableStateOf("")
        private set

    var isDegree by mutableStateOf(true)
        private set

    var isScientificExpanded by mutableStateOf(false)

    var showHistory by mutableStateOf(false)

    private val decimalFormat = DecimalFormat("#.##########", DecimalFormatSymbols(Locale.US))

    fun onDigitClick(digit: String) {
        if (expression == "Error" || expression == "0") {
            expression = digit
        } else {
            expression += digit
        }
        updatePreview()
    }

    fun onDecimalClick() {
        if (expression == "Error" || expression.isEmpty()) {
            expression = "0."
            updatePreview()
            return
        }

        // Simple validation to check if the last number segment already has a decimal point
        val lastNumberSegment = expression.split('+', '-', '×', '÷', '%', '^', '(', ')').lastOrNull()
        if (lastNumberSegment != null && !lastNumberSegment.contains('.')) {
            expression += "."
        }
        updatePreview()
    }

    fun onOperatorClick(op: String) {
        if (expression == "Error") {
            expression = "0$op"
            updatePreview()
            return
        }

        if (expression.isEmpty()) {
            if (op == "-") {
                expression = "-"
                updatePreview()
            }
            return
        }

        val lastChar = expression.lastOrNull()
        if (lastChar != null && isOperator(lastChar.toString())) {
            // Replace the last operator with the new one
            expression = expression.dropLast(1) + op
        } else {
            expression += op
        }
        updatePreview()
    }

    fun onFunctionClick(func: String) {
        if (expression == "Error" || expression == "0") {
            expression = "$func("
        } else {
            expression += "$func("
        }
        updatePreview()
    }

    fun onConstantClick(const: String) {
        if (expression == "Error" || expression == "0") {
            expression = const
        } else {
            expression += const
        }
        updatePreview()
    }

    fun onParenthesisClick(paren: String) {
        if (expression == "Error" || expression == "0") {
            expression = paren
        } else {
            expression += paren
        }
        updatePreview()
    }

    fun onClearClick() {
        expression = ""
        previewResult = ""
    }

    fun onBackspaceClick() {
        if (expression == "Error" || expression.isEmpty()) {
            expression = ""
            previewResult = ""
            return
        }

        // Check if we are deleting a full function keyword like "sin(", "cos(", etc.
        val keywords = listOf("sin(", "cos(", "tan(", "asin(", "acos(", "atan(", "sqrt(", "log(", "ln(", "abs(")
        var deletedKeyword = false
        for (kw in keywords) {
            if (expression.endsWith(kw)) {
                expression = expression.dropLast(kw.length)
                deletedKeyword = true
                break
            }
        }

        if (!deletedKeyword) {
            expression = expression.dropLast(1)
        }

        updatePreview()
    }

    fun onToggleDegree() {
        isDegree = !isDegree
        updatePreview()
    }

    fun onEqualClick() {
        if (expression.isBlank()) return

        try {
            val rawResult = ExpressionEvaluator.evaluate(expression, isDegree)
            if (rawResult.isNaN() || rawResult.isInfinite()) {
                expression = "Error"
                previewResult = ""
                return
            }

            val formattedResult = decimalFormat.format(rawResult)
            
            // Save to database
            val entry = HistoryEntry(expression = expression, result = formattedResult)
            viewModelScope.launch {
                repository.insert(entry)
            }

            expression = formattedResult
            previewResult = ""
        } catch (e: Exception) {
            expression = "Error"
            previewResult = ""
        }
    }

    fun onHistoryItemClick(entry: HistoryEntry) {
        expression = entry.expression
        showHistory = false
        updatePreview()
    }

    fun onClearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun onDeleteHistoryEntry(id: Long) {
        viewModelScope.launch {
            repository.deleteEntry(id)
        }
    }

    private fun updatePreview() {
        if (expression.isBlank()) {
            previewResult = ""
            return
        }

        // Don't show preview if it's just a number
        if (expression.toDoubleOrNull() != null) {
            previewResult = ""
            return
        }

        try {
            // Count open vs close parentheses.
            // If they are unclosed, we temporarily append closed parentheses for preview calculation!
            // This is super neat as it shows live preview even during typing: sin(45 -> sin(45)
            val openCount = expression.count { it == '(' }
            val closeCount = expression.count { it == ')' }
            var virtualExpr = expression
            if (openCount > closeCount) {
                virtualExpr += ")".repeat(openCount - closeCount)
            }

            val rawResult = ExpressionEvaluator.evaluate(virtualExpr, isDegree)
            if (!rawResult.isNaN() && !rawResult.isInfinite()) {
                val formattedResult = decimalFormat.format(rawResult)
                previewResult = if (formattedResult != expression) formattedResult else ""
            } else {
                previewResult = ""
            }
        } catch (e: Exception) {
            previewResult = ""
        }
    }

    private fun isOperator(s: String): Boolean {
        return s == "+" || s == "-" || s == "×" || s == "÷" || s == "%" || s == "^"
    }
}

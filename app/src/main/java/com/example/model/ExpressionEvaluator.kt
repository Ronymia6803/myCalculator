package com.example.model

import kotlin.math.*

object ExpressionEvaluator {

    fun evaluate(expression: String, isDegree: Boolean = true): Double {
        if (expression.isBlank()) return 0.0
        val sanitized = sanitize(expression)
        return Parser(sanitized, isDegree).parse()
    }

    private fun sanitize(expr: String): String {
        // Prepare expression for parsing
        return expr
            .replace("×", "*")
            .replace("÷", "/")
            .replace("π", "pi")
            // Resolve implicit multiplication like:
            // "2(3)" -> "2*(3)"
            // "2pi" -> "2*pi"
            // "2sin" -> "2*sin"
            // "pi(" -> "pi*("
            .insertImplicitMultiplication()
    }

    private fun String.insertImplicitMultiplication(): String {
        val result = StringBuilder()
        for (i in 0 until this.length) {
            val curr = this[i]
            result.append(curr)
            if (i < this.length - 1) {
                val next = this[i + 1]
                val isCurrDigitOrRightParenOrConst = curr.isDigit() || curr == ')' || curr == 'i' || curr == 'e'
                val isNextDigitOrLeftParenOrConstOrFunc = next.isDigit() || next == '(' || next == 'p' || next == 'e' || (next in 'a'..'z' && next != 'i')
                
                // Exclude characters inside word boundaries like "p" followed by "i" or functions
                val isSpecialCase = (curr == 'p' && next == 'i') || 
                                    (curr == 'l' && next == 'n') ||
                                    (curr == 's' && next == 'i' && i < this.length - 2 && this[i+2] == 'n') ||
                                    (curr == 'c' && next == 'o' && i < this.length - 2 && this[i+2] == 's') ||
                                    (curr == 't' && next == 'a' && i < this.length - 2 && this[i+2] == 'n') ||
                                    (curr == 'l' && next == 'o' && i < this.length - 2 && this[i+2] == 'g') ||
                                    (curr == 's' && next == 'q' && i < this.length - 2 && this[i+2] == 'r') ||
                                    (curr == 'a' && next == 'b' && i < this.length - 2 && this[i+2] == 's')

                if (isCurrDigitOrRightParenOrConst && isNextDigitOrLeftParenOrConstOrFunc && !isSpecialCase) {
                    result.append('*')
                }
            }
        }
        return result.toString()
    }

    private class Parser(val input: String, val isDegree: Boolean) {
        var pos = -1
        var ch = ' '

        fun nextChar() {
            pos++
            ch = if (pos < input.length) input[pos] else '\u0000'
        }

        fun eat(charToEat: Char): Boolean {
            while (ch == ' ') nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            while (ch == ' ') nextChar()
            if (pos < input.length) throw IllegalArgumentException("Unexpected: '$ch'")
            return x
        }

        // expression = term | expression `+` term | expression `-` term
        fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                if (eat('+')) x += parseTerm()
                else if (eat('-')) x -= parseTerm()
                else return x
            }
        }

        // term = factor | term `*` factor | term `/` factor | term `%` factor
        fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                if (eat('*')) x *= parseFactor()
                else if (eat('/')) {
                    val divisor = parseFactor()
                    if (divisor == 0.0) throw ArithmeticException("Divide by zero")
                    x /= divisor
                } else if (eat('%')) {
                    val divisor = parseFactor()
                    if (divisor == 0.0) throw ArithmeticException("Modulo by zero")
                    x %= divisor
                } else return x
            }
        }

        // factor = base | base `^` factor
        fun parseFactor(): Double {
            var x = parseBase()
            if (eat('^')) {
                x = x.pow(parseFactor())
            }
            return x
        }

        fun parseBase(): Double {
            val negate = eat('-')
            if (eat('+')) { /* no-op unary plus */ }

            var x: Double
            val startPos = this.pos
            if (eat('(')) {
                x = parseExpression()
                if (!eat(')')) throw IllegalArgumentException("Unclosed (")
            } else if ((ch in '0'..'9') || ch == '.') {
                while ((ch in '0'..'9') || ch == '.') nextChar()
                val numStr = input.substring(startPos, this.pos)
                x = numStr.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid number: $numStr")
            } else if (ch in 'a'..'z') {
                while (ch in 'a'..'z') nextChar()
                val funcOrConst = input.substring(startPos, this.pos)
                
                if (eat('(')) {
                    val arg = parseExpression()
                    if (!eat(')')) throw IllegalArgumentException("Unclosed ( for $funcOrConst")
                    
                    x = when (funcOrConst) {
                        "sin" -> if (isDegree) sin(Math.toRadians(arg)) else sin(arg)
                        "cos" -> if (isDegree) cos(Math.toRadians(arg)) else cos(arg)
                        "tan" -> if (isDegree) tan(Math.toRadians(arg)) else tan(arg)
                        "asin" -> {
                            val res = asin(arg)
                            if (isDegree) Math.toDegrees(res) else res
                        }
                        "acos" -> {
                            val res = acos(arg)
                            if (isDegree) Math.toDegrees(res) else res
                        }
                        "atan" -> {
                            val res = atan(arg)
                            if (isDegree) Math.toDegrees(res) else res
                        }
                        "sqrt" -> {
                            if (arg < 0) throw IllegalArgumentException("Square root of negative number")
                            sqrt(arg)
                        }
                        "log" -> {
                            if (arg <= 0) throw IllegalArgumentException("Log of non-positive number")
                            log10(arg)
                        }
                        "ln" -> {
                            if (arg <= 0) throw IllegalArgumentException("Ln of non-positive number")
                            ln(arg)
                        }
                        "abs" -> abs(arg)
                        else -> throw IllegalArgumentException("Unknown function: $funcOrConst")
                    }
                } else {
                    x = when (funcOrConst) {
                        "pi" -> Math.PI
                        "e" -> Math.E
                        else -> throw IllegalArgumentException("Unknown: $funcOrConst")
                    }
                }
            } else {
                throw IllegalArgumentException("Unexpected: '$ch'")
            }

            return if (negate) -x else x
        }
    }
}

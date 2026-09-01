package com.moneytracker.util

/**
 * Robust mathematical formula / expression evaluator for calculated amount inputs.
 * Supports formulas preceded by an '=' sign (e.g. '= 2*5*30.00', '= 100 + (50 * 2)').
 */
object FormulaEvaluator {

    fun isFormula(text: String): Boolean = text.trim().startsWith("=")

    /**
     * Evaluates a mathematical expression or raw number.
     * Returns the calculated Double or null if invalid.
     */
    fun evaluate(input: String): Double? {
        val clean = input.trim().removePrefix("=").trim()
        if (clean.isBlank()) return null

        return try {
            val tokens = tokenize(clean)
            if (tokens.isEmpty()) return null
            val parser = ExpressionParser(tokens)
            val result = parser.parse()
            if (result.isFinite()) result else null
        } catch (e: Exception) {
            null
        }
    }

    private fun tokenize(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            if (c.isWhitespace()) {
                i++
                continue
            }
            if (c in "+-*/()^%") {
                tokens.add(c.toString())
                i++
            } else if (c.isDigit() || c == '.') {
                val sb = StringBuilder()
                while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                    sb.append(expr[i])
                    i++
                }
                tokens.add(sb.toString())
            } else {
                i++
            }
        }
        return tokens
    }

    private class ExpressionParser(private val tokens: List<String>) {
        private var pos = 0

        fun parse(): Double {
            val res = parseExpression()
            if (pos < tokens.size) {
                // Unexpected extra tokens
            }
            return res
        }

        // expression = term { ("+" | "-") term }
        private fun parseExpression(): Double {
            var result = parseTerm()
            while (pos < tokens.size && (tokens[pos] == "+" || tokens[pos] == "-")) {
                val op = tokens[pos++]
                val nextTerm = parseTerm()
                if (op == "+") result += nextTerm else result -= nextTerm
            }
            return result
        }

        // term = factor { ("*" | "/" | "%") factor }
        private fun parseTerm(): Double {
            var result = parseFactor()
            while (pos < tokens.size && (tokens[pos] == "*" || tokens[pos] == "/" || tokens[pos] == "%")) {
                val op = tokens[pos++]
                val nextFactor = parseFactor()
                when (op) {
                    "*" -> result *= nextFactor
                    "/" -> {
                        if (nextFactor == 0.0) throw ArithmeticException("Division by zero")
                        result /= nextFactor
                    }
                    "%" -> result %= nextFactor
                }
            }
            return result
        }

        // factor = [ "+" | "-" ] primary
        private fun parseFactor(): Double {
            if (pos >= tokens.size) return 0.0
            if (tokens[pos] == "+") {
                pos++
                return parseFactor()
            }
            if (tokens[pos] == "-") {
                pos++
                return -parseFactor()
            }
            return parsePrimary()
        }

        // primary = number | "(" expression ")"
        private fun parsePrimary(): Double {
            if (pos >= tokens.size) return 0.0
            val token = tokens[pos++]
            if (token == "(") {
                val result = parseExpression()
                if (pos < tokens.size && tokens[pos] == ")") {
                    pos++
                }
                return result
            }
            return token.toDoubleOrNull() ?: 0.0
        }
    }
}

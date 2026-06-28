package com.example.solver

import java.util.*
import kotlin.math.*

data class SolveStep(
    val title: String,
    val formula: String,
    val explanation: String
)

data class SolveResult(
    val originalEquation: String,
    val finalAnswer: String,
    val type: String, // Arithmetic, Fractions, Linear Equation, Quadratic, System of Equations, Calculus
    val steps: List<SolveStep>,
    val supportsGraph: Boolean = false,
    val graphParams: Map<String, Double> = emptyMap() // e.g. "a" to 1.0, "b" to -5.0, "c" to 6.0 for quadratic
)

object MathSolver {

    fun solve(rawInput: String): SolveResult {
        // Sanitize input
        val input = rawInput.trim().replace("\\s+".toRegex(), "")
        
        try {
            // 1. Try System of Equations (e.g. 2x+y=5, x-y=1 or 2x+y=5; x-y=1)
            if (input.contains(",") || input.contains(";")) {
                val parts = if (input.contains(";")) input.split(";") else input.split(",")
                if (parts.size == 2) {
                    val res = trySolveSystem(parts[0].trim(), parts[1].trim())
                    if (res != null) return res
                }
            }

            // 2. Try Calculus (e.g. d/dx(x^2 + 2x))
            if (input.startsWith("d/dx") || input.contains("d/dx")) {
                val res = trySolveCalculus(input)
                if (res != null) return res
            }

            // 3. Try Quadratic Equation (e.g. x^2 - 5x + 6 = 0 or 2x^2 + 4x - 6 = 0)
            if (input.contains("x^2") || input.contains("x²")) {
                val res = trySolveQuadratic(input)
                if (res != null) return res
            }

            // 4. Try Linear Equation with single variable x (e.g. 2x + 5 = 15)
            if (input.contains("x") && input.contains("=")) {
                val res = trySolveLinear(input)
                if (res != null) return res
            }

            // 5. Try Fractions arithmetic (e.g. 1/2 + 3/4)
            if (input.contains("/") && (input.contains("+") || input.contains("-") || input.contains("*") || input.contains("·"))) {
                val res = trySolveFractions(input)
                if (res != null) return res
            }

            // 6. Default to standard arithmetic evaluation
            return evaluateArithmetic(input)

        } catch (e: Exception) {
            return fallbackResult(rawInput, "Unable to parse formula. Please check syntax.")
        }
    }

    private fun trySolveLinear(input: String): SolveResult? {
        // Linear equation of form: left = right
        val parts = input.split("=")
        if (parts.size != 2) return null
        val left = parts[0]
        val right = parts[1]

        // Parse linear coefficients: ax + b = cx + d
        // We can isolate terms by parsing both sides
        val leftCoeffs = parseLinearExpression(left) ?: return null
        val rightCoeffs = parseLinearExpression(right) ?: return null

        val a = leftCoeffs.first // x coefficient on left
        val b = leftCoeffs.second // constant term on left
        val c = rightCoeffs.first // x coefficient on right
        val d = rightCoeffs.second // constant term on right

        val steps = mutableListOf<SolveStep>()
        steps.add(
            SolveStep(
                title = "Original Equation",
                formula = "$left = $right",
                explanation = "We start with the given linear equation."
            )
        )

        // Step 1: Move variable terms to left side, constant terms to right side
        // ax - cx = d - b
        val newA = a - c
        val newB = d - b

        steps.add(
            SolveStep(
                title = "Group Like Terms",
                formula = "${formatTerm(a, "x")} - ${formatTerm(c, "x")} = $d - ${formatConstant(b)}",
                explanation = "Move all terms containing x to the left side and constant values to the right side by subtracting or adding them."
            )
        )

        steps.add(
            SolveStep(
                title = "Simplify Both Sides",
                formula = "${formatTerm(newA, "x")} = $newB",
                explanation = "Combine like terms on both sides of the equation."
            )
        )

        if (newA == 0.0) {
            return if (newB == 0.0) {
                steps.add(
                    SolveStep(
                        title = "Final Solution",
                        formula = "Infinite Solutions",
                        explanation = "Both sides are equivalent. The equation is true for all real values of x."
                    )
                )
                SolveResult(input, "Infinite Solutions", "Linear Equation", steps, supportsGraph = false)
            } else {
                steps.add(
                    SolveStep(
                        title = "Final Solution",
                        formula = "No Solution",
                        explanation = "This is a contradiction, so there are no real solutions."
                    )
                )
                SolveResult(input, "No Solution", "Linear Equation", steps, supportsGraph = false)
            }
        }

        val resultVal = newB / newA
        steps.add(
            SolveStep(
                title = "Isolate x",
                formula = "x = $newB / $newA",
                explanation = "Divide both sides by ${newA} to solve for x."
            )
        )

        val formattedResult = roundToDisplay(resultVal)
        steps.add(
            SolveStep(
                title = "Calculate Result",
                formula = "x = $formattedResult",
                explanation = "The solution is simplified."
            )
        )

        return SolveResult(
            originalEquation = "$left = $right",
            finalAnswer = "x = $formattedResult",
            type = "Linear Equation",
            steps = steps,
            supportsGraph = true,
            graphParams = mapOf("type" to 1.0, "m" to newA, "c" to -newB) // plot y = newA * x - newB to see roots
        )
    }

    private fun trySolveQuadratic(input: String): SolveResult? {
        // Supports: ax^2 + bx + c = 0 or ax^2 + bx = d
        var equation = input.replace("x²", "x^2")
        val parts = equation.split("=")
        if (parts.size != 2) return null

        val leftStr = parts[0]
        val rightStr = parts[1]

        // Parse coefficients for quadratic: Ax^2 + Bx + C = 0
        // Move everything to the left side
        val leftCoeffs = parseQuadraticExpression(leftStr) ?: return null
        val rightCoeffs = parseQuadraticExpression(rightStr) ?: return null

        val a = leftCoeffs.a - rightCoeffs.a
        val b = leftCoeffs.b - rightCoeffs.b
        val c = leftCoeffs.c - rightCoeffs.c

        if (a == 0.0) {
            // Not quadratic, try linear instead
            return trySolveLinear(input)
        }

        val steps = mutableListOf<SolveStep>()
        val standardForm = "${formatTerm(a, "x^2")} ${formatSignedTerm(b, "x")} ${formatSignedConstant(c)} = 0"
        steps.add(
            SolveStep(
                title = "Write in Standard Form",
                formula = standardForm,
                explanation = "We rearrange the equation into standard quadratic form: ax² + bx + c = 0."
            )
        )

        steps.add(
            SolveStep(
                title = "Identify Coefficients",
                formula = "a = $a, \\; b = $b, \\; c = $c",
                explanation = "Identify the quadratic coefficient (a), linear coefficient (b), and constant term (c)."
            )
        )

        // Discriminant calculation
        val disc = b * b - 4 * a * c
        steps.add(
            SolveStep(
                title = "Calculate Discriminant",
                formula = "D = b^2 - 4ac = ($b)^2 - 4($a)($c)",
                explanation = "The discriminant determines the number and type of roots. Let's calculate D."
            )
        )

        steps.add(
            SolveStep(
                title = "Simplify Discriminant",
                formula = "D = $disc",
                explanation = "Our discriminant is $disc. Since " + when {
                    disc > 0 -> "D > 0, we have two distinct real roots."
                    disc == 0.0 -> "D = 0, we have one repeated real root."
                    else -> "D < 0, we have two complex conjugate roots."
                }
            )
        )

        val quadraticFormula = "x = \\frac{-b \\pm \\sqrt{D}}{2a}"
        steps.add(
            SolveStep(
                title = "Apply Quadratic Formula",
                formula = "x = \\frac{-($b) \\pm \\sqrt{$disc}}{2($a)}",
                explanation = "Substitute the values of a, b, and D into the quadratic formula."
            )
        )

        val finalAnswer: String
        if (disc >= 0) {
            val sqrtDisc = sqrt(disc)
            val root1 = (-b + sqrtDisc) / (2 * a)
            val root2 = (-b - sqrtDisc) / (2 * a)

            val r1Formatted = roundToDisplay(root1)
            val r2Formatted = roundToDisplay(root2)

            if (disc == 0.0) {
                steps.add(
                    SolveStep(
                        title = "Simplify Root",
                        formula = "x = \\frac{-($b)}{${2 * a}} = $r1Formatted",
                        explanation = "Solve for the single repeated root."
                    )
                )
                finalAnswer = "x = $r1Formatted"
            } else {
                steps.add(
                    SolveStep(
                        title = "Solve for Roots",
                        formula = "x = \\frac{-($b) \\pm $sqrtDisc}{${2 * a}}",
                        explanation = "Find both solutions by calculating for + and - cases."
                    )
                )
                steps.add(
                    SolveStep(
                        title = "Root 1 (+ Case)",
                        formula = "x_1 = \\frac{-($b) + $sqrtDisc}{${2 * a}} = $r1Formatted",
                        explanation = "Calculate the first root by adding the square root."
                    )
                )
                steps.add(
                    SolveStep(
                        title = "Root 2 (- Case)",
                        formula = "x_2 = \\frac{-($b) - $sqrtDisc}{${2 * a}} = $r2Formatted",
                        explanation = "Calculate the second root by subtracting the square root."
                    )
                )
                finalAnswer = "x_1 = $r1Formatted, \\; x_2 = $r2Formatted"
            }
        } else {
            // Complex roots
            val realPart = -b / (2 * a)
            val imagPart = sqrt(-disc) / (2 * a)
            val realFormatted = roundToDisplay(realPart)
            val imagFormatted = roundToDisplay(abs(imagPart))

            steps.add(
                SolveStep(
                    title = "Solve for Complex Roots",
                    formula = "x = $realFormatted \\pm ${imagFormatted}i",
                    explanation = "Since the discriminant is negative, we express the roots in terms of the imaginary unit i."
                )
            )
            finalAnswer = "x = $realFormatted \\pm ${imagFormatted}i"
        }

        return SolveResult(
            originalEquation = input,
            finalAnswer = finalAnswer,
            type = "Quadratic Equation",
            steps = steps,
            supportsGraph = true,
            graphParams = mapOf("type" to 2.0, "a" to a, "b" to b, "c" to c)
        )
    }

    private fun trySolveSystem(eq1: String, eq2: String): SolveResult? {
        // Solves a system of equations: ax + by = c and dx + ey = f
        val coeffs1 = parseSystemExpression(eq1) ?: return null
        val coeffs2 = parseSystemExpression(eq2) ?: return null

        val a = coeffs1.a
        val b = coeffs1.b
        val c = coeffs1.c

        val d = coeffs2.a
        val e = coeffs2.b
        val f = coeffs2.c

        // Determinant for Cramer's rule
        val det = a * e - b * d

        val steps = mutableListOf<SolveStep>()
        steps.add(
            SolveStep(
                title = "Original System",
                formula = "1) \\; ${formatTerm(a, "x")} ${formatSignedTerm(b, "y")} = $c \\newline 2) \\; ${formatTerm(d, "x")} ${formatSignedTerm(e, "y")} = $f",
                explanation = "We have a system of two linear equations with variables x and y."
            )
        )

        if (det == 0.0) {
            // Check if parallel or coincident
            val isCoincident = (a != 0.0 && d != 0.0 && c / a == f / d) || (b != 0.0 && e != 0.0 && c / b == f / e)
            return if (isCoincident) {
                steps.add(
                    SolveStep(
                        title = "Final Solution",
                        formula = "Infinite Solutions",
                        explanation = "The equations represent the exact same line, meaning there are infinite solutions."
                    )
                )
                SolveResult("$eq1, $eq2", "Infinite Solutions", "System of Equations", steps)
            } else {
                steps.add(
                    SolveStep(
                        title = "Final Solution",
                        formula = "No Solution",
                        explanation = "The lines are parallel and never intersect, so there is no solution."
                    )
                )
                SolveResult("$eq1, $eq2", "No Solution", "System of Equations", steps)
            }
        }

        // Use Cramer's rule or substitution steps for explanation
        steps.add(
            SolveStep(
                title = "Express y in terms of x",
                formula = "y = \\frac{$c - ${formatTerm(a, "x")}}{$b}",
                explanation = "We isolate y in equation 1) to prepare for substitution."
            )
        )

        // Substitute into 2
        // d*x + e * ((c - a*x)/b) = f
        // d*b*x + e*c - e*a*x = f*b
        // (d*b - e*a)*x = f*b - e*c
        val xVal = (c * e - b * f) / det
        val yVal = (a * f - c * d) / det

        val xFormatted = roundToDisplay(xVal)
        val yFormatted = roundToDisplay(yVal)

        steps.add(
            SolveStep(
                title = "Substitute y into Equation 2",
                formula = "${formatTerm(d, "x")} + $e \\cdot \\left(\\frac{$c - ${formatTerm(a, "x")}}{$b}\\right) = $f",
                explanation = "Substitute the expression for y from equation 1) into equation 2)."
            )
        )

        steps.add(
            SolveStep(
                title = "Solve for x",
                formula = "x = $xFormatted",
                explanation = "Solve the single-variable equation for x."
            )
        )

        steps.add(
            SolveStep(
                title = "Substitute x back to find y",
                formula = "y = \\frac{$c - $a \\cdot ($xFormatted)}{$b}",
                explanation = "Substitute the value of x back into the expression we found for y."
            )
        )

        steps.add(
            SolveStep(
                title = "Final Coordinates",
                formula = "x = $xFormatted, \\; y = $yFormatted",
                explanation = "The point of intersection of the two lines is ($xFormatted, $yFormatted)."
            )
        )

        return SolveResult(
            originalEquation = "$eq1, \\; $eq2",
            finalAnswer = "x = $xFormatted, \\; y = $yFormatted",
            type = "System of Equations",
            steps = steps,
            supportsGraph = true,
            graphParams = mapOf(
                "type" to 3.0,
                "a1" to a, "b1" to b, "c1" to c,
                "a2" to d, "b2" to e, "c2" to f,
                "x_intersect" to xVal, "y_intersect" to yVal
            )
        )
    }

    private fun trySolveCalculus(input: String): SolveResult? {
        // Parses d/dx(...)
        // Format: d/dx(3x^2 + 5x)
        val regex = "d/dx\\((.*)\\)".toRegex()
        val matchResult = regex.find(input) ?: return null
        val innerExpr = matchResult.groupValues[1]

        val terms = parsePolynomialTerms(innerExpr)
        if (terms.isEmpty()) return null

        val steps = mutableListOf<SolveStep>()
        steps.add(
            SolveStep(
                title = "Original Derivative",
                formula = "\\frac{d}{dx}\\left($innerExpr\\right)",
                explanation = "We need to find the derivative of the expression with respect to x."
            )
        )

        steps.add(
            SolveStep(
                title = "Apply Sum & Constant Rules",
                formula = terms.joinToString(" + ") { "\\frac{d}{dx}\\left(${formatTerm(it.coeff, "x^" + it.pow.toInt())}\\right)" },
                explanation = "The derivative of a sum is the sum of the derivatives. We take the derivative of each term individually."
            )
        )

        val solvedTerms = mutableListOf<String>()
        val resultTerms = mutableListOf<PolynomialTerm>()

        for (term in terms) {
            val power = term.pow
            val coeff = term.coeff

            if (power == 0.0) {
                solvedTerms.add("\\frac{d}{dx}($coeff) = 0")
            } else {
                val newCoeff = coeff * power
                val newPower = power - 1
                val termFormula = "\\frac{d}{dx}(${formatTerm(coeff, "x^" + power.toInt())}) = $coeff \\cdot $power x^{${power.toInt() - 1}} = ${formatTerm(newCoeff, if (newPower == 0.0) "" else "x^" + newPower.toInt())}"
                solvedTerms.add(termFormula)
                resultTerms.add(PolynomialTerm(newCoeff, newPower))
            }
        }

        steps.add(
            SolveStep(
                title = "Differentiate Each Term",
                formula = solvedTerms.joinToString(" \\newline "),
                explanation = "Apply the power rule: d/dx(x^n) = n*x^(n-1) to each term. The derivative of any constant is 0."
            )
        )

        // Combine result terms
        val finalAnswer = combinePolynomialTerms(resultTerms)
        steps.add(
            SolveStep(
                title = "Combine Terms",
                formula = "\\frac{d}{dx}\\left($innerExpr\\right) = $finalAnswer",
                explanation = "Combine the derived terms to get the final simplified derivative."
            )
        )

        return SolveResult(
            originalEquation = input,
            finalAnswer = finalAnswer,
            type = "Calculus",
            steps = steps,
            supportsGraph = true,
            graphParams = mapOf(
                "type" to 4.0,
                // store original terms coefficients for drawing
                "original_power" to (terms.maxOfOrNull { it.pow } ?: 0.0)
            )
        )
    }

    private fun trySolveFractions(input: String): SolveResult? {
        // Parses simple fraction addition/subtraction, e.g. 1/2 + 3/4
        val regex = "(-?\\d+)/(\\d+)\\s*([+-])\\s*(-?\\d+)/(\\d+)".toRegex()
        val matchResult = regex.find(input) ?: return null

        val n1 = matchResult.groupValues[1].toInt()
        val d1 = matchResult.groupValues[2].toInt()
        val op = matchResult.groupValues[3]
        val n2 = matchResult.groupValues[4].toInt()
        val d2 = matchResult.groupValues[5].toInt()

        if (d1 == 0 || d2 == 0) return null

        val steps = mutableListOf<SolveStep>()
        steps.add(
            SolveStep(
                title = "Write Expression",
                formula = "\\frac{$n1}{$d1} $op \\frac{$n2}{$d2}",
                explanation = "We perform addition or subtraction on two fractions."
            )
        )

        // Find common denominator (LCM)
        val commonDenom = lcm(d1, d2)
        steps.add(
            SolveStep(
                title = "Find Common Denominator",
                formula = "\\text{LCM}($d1, $d2) = $commonDenom",
                explanation = "To add or subtract fractions, they must share a common denominator. The Least Common Multiple of $d1 and $d2 is $commonDenom."
            )
        )

        val factor1 = commonDenom / d1
        val factor2 = commonDenom / d2

        val newN1 = n1 * factor1
        val newN2 = n2 * factor2

        steps.add(
            SolveStep(
                title = "Adjust Numerators",
                formula = "\\frac{$n1 \\cdot $factor1}{$commonDenom} $op \\frac{$n2 \\cdot $factor2}{$commonDenom} = \\frac{$newN1}{$commonDenom} $op \\frac{$newN2}{$commonDenom}",
                explanation = "Multiply both the numerator and denominator of each fraction so that its denominator is $commonDenom."
            )
        )

        val resultN = if (op == "+") newN1 + newN2 else newN1 - newN2
        steps.add(
            SolveStep(
                title = "Combine Numerators",
                formula = "\\frac{$newN1 $op $newN2}{$commonDenom} = \\frac{$resultN}{$commonDenom}",
                explanation = "Now, add or subtract the numerators while keeping the common denominator."
            )
        )

        // Simplify
        val g = gcd(abs(resultN), commonDenom)
        val finalN = resultN / g
        val finalD = commonDenom / g

        val simplifiedFormula = if (g > 1) {
            "\\frac{$resultN \\div $g}{$commonDenom \\div $g} = \\frac{$finalN}{$finalD}"
        } else {
            "\\frac{$finalN}{$finalD}"
        }

        val finalAnsStr = if (finalD == 1) "$finalN" else "$finalN/$finalD"

        steps.add(
            SolveStep(
                title = "Simplify Fraction",
                formula = simplifiedFormula,
                explanation = "Reduce the fraction to its lowest terms by dividing the numerator and denominator by their Greatest Common Divisor ($g)."
            )
        )

        return SolveResult(
            originalEquation = input,
            finalAnswer = finalAnsStr,
            type = "Fractions",
            steps = steps
        )
    }

    private fun evaluateArithmetic(input: String): SolveResult {
        // Evaluate simple arithmetic equations using basic tokenizing
        val expr = input.replace("×", "*").replace("÷", "/").replace("·", "*")
        val resultVal = evalExpression(expr)
        
        val steps = mutableListOf<SolveStep>()
        steps.add(
            SolveStep(
                title = "Calculate Expression",
                formula = expr,
                explanation = "We evaluate the arithmetic expression using standard order of operations (PEMDAS/BODMAS)."
            )
        )

        steps.add(
            SolveStep(
                title = "Final Value",
                formula = "= ${roundToDisplay(resultVal)}",
                explanation = "Perform calculations to find the final numerical answer."
            )
        )

        return SolveResult(
            originalEquation = input,
            finalAnswer = roundToDisplay(resultVal),
            type = "Arithmetic",
            steps = steps
        )
    }

    // Helper Math utilities
    private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
    private fun lcm(a: Int, b: Int): Int = (a * b) / gcd(a, b)

    private fun roundToDisplay(value: Double): String {
        if (value.isInfinite() || value.isNaN()) return value.toString()
        if (value == value.toInt().toDouble()) return value.toInt().toString()
        return String.format(Locale.US, "%.4f", value).trimEnd('0').trimEnd('.')
    }

    private fun formatTerm(coeff: Double, variable: String): String {
        if (coeff == 0.0) return ""
        if (coeff == 1.0) return variable
        if (coeff == -1.0) return "-$variable"
        val formattedCoeff = if (coeff == coeff.toInt().toDouble()) coeff.toInt().toString() else coeff.toString()
        return "$formattedCoeff$variable"
    }

    private fun formatSignedTerm(coeff: Double, variable: String): String {
        if (coeff == 0.0) return ""
        val sign = if (coeff > 0) "+ " else "- "
        val absCoeff = abs(coeff)
        val coeffStr = if (absCoeff == 1.0) "" else (if (absCoeff == absCoeff.toInt().toDouble()) absCoeff.toInt().toString() else absCoeff.toString())
        return "$sign$coeffStr$variable"
    }

    private fun formatConstant(value: Double): String {
        return if (value == value.toInt().toDouble()) value.toInt().toString() else value.toString()
    }

    private fun formatSignedConstant(value: Double): String {
        if (value == 0.0) return ""
        val sign = if (value > 0) "+ " else "- "
        val absVal = abs(value)
        val valStr = if (absVal == absVal.toInt().toDouble()) absVal.toInt().toString() else absVal.toString()
        return "$sign$valStr"
    }

    // A simple arithmetic expression evaluator
    private fun evalExpression(str: String): Double {
        return object : Any() {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < str.length) str[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < str.length) throw RuntimeException("Unexpected: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm() // addition
                    else if (eat('-'.code)) x -= parseTerm() // subtraction
                    else return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor() // multiplication
                    else if (eat('/'.code)) x /= parseFactor() // division
                    else return x
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor() // unary plus
                if (eat('-'.code)) return -parseFactor() // unary minus

                var x: Double
                val startPos = this.pos
                if (eat('('.code)) { // parentheses
                    x = parseExpression()
                    eat(')'.code)
                } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) { // numbers
                    while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                    x = str.substring(startPos, this.pos).toDouble()
                } else {
                    throw RuntimeException("Unexpected: " + ch.toChar())
                }

                if (eat('^'.code)) x = x.pow(parseFactor()) // exponentiation

                return x
            }
        }.parse()
    }

    // Linear Expression Parser ax + b
    private fun parseLinearExpression(expr: String): Pair<Double, Double>? {
        val sanitized = expr.replace("\\s+".toRegex(), "").replace("-", "+-")
        val terms = sanitized.split("+")
        var coeffX = 0.0
        var constant = 0.0

        for (term in terms) {
            if (term.isEmpty()) continue
            if (term.contains("x")) {
                val coeffStr = term.replace("x", "")
                val coeff = when {
                    coeffStr.isEmpty() -> 1.0
                    coeffStr == "-" -> -1.0
                    coeffStr == "+" -> 1.0
                    else -> coeffStr.toDoubleOrNull() ?: return null
                }
                coeffX += coeff
            } else {
                val value = term.toDoubleOrNull() ?: return null
                constant += value
            }
        }
        return Pair(coeffX, constant)
    }

    // Quadratic Expression ax^2 + bx + c
    data class QuadCoeffs(val a: Double, val b: Double, val c: Double)

    private fun parseQuadraticExpression(expr: String): QuadCoeffs? {
        val sanitized = expr.replace("x²", "x^2").replace("-", "+-").replace("\\s+".toRegex(), "")
        val terms = sanitized.split("+")
        var a = 0.0
        var b = 0.0
        var c = 0.0

        for (term in terms) {
            if (term.isEmpty()) continue
            if (term.contains("x^2")) {
                val coeffStr = term.replace("x^2", "")
                val coeff = when {
                    coeffStr.isEmpty() -> 1.0
                    coeffStr == "-" -> -1.0
                    coeffStr == "+" -> 1.0
                    else -> coeffStr.toDoubleOrNull() ?: return null
                }
                a += coeff
            } else if (term.contains("x")) {
                val coeffStr = term.replace("x", "")
                val coeff = when {
                    coeffStr.isEmpty() -> 1.0
                    coeffStr == "-" -> -1.0
                    coeffStr == "+" -> 1.0
                    else -> coeffStr.toDoubleOrNull() ?: return null
                }
                b += coeff
            } else {
                val value = term.toDoubleOrNull() ?: return null
                c += value
            }
        }
        return QuadCoeffs(a, b, c)
    }

    // System of Equations Parser
    private fun parseSystemExpression(expr: String): QuadCoeffs? {
        // e.g. 2x + y = 5 -> translates to 2x + y - 5 = 0
        val parts = expr.split("=")
        if (parts.size != 2) return null

        val left = parts[0].replace("-", "+-")
        val rightVal = parts[1].toDoubleOrNull() ?: return null

        val leftTerms = left.split("+")
        var coeffX = 0.0
        var coeffY = 0.0
        var constant = 0.0

        for (term in leftTerms) {
            val t = term.trim()
            if (t.isEmpty()) continue
            if (t.contains("x")) {
                val coeffStr = t.replace("x", "")
                val coeff = when {
                    coeffStr.isEmpty() -> 1.0
                    coeffStr == "-" -> -1.0
                    coeffStr == "+" -> 1.0
                    else -> coeffStr.toDoubleOrNull() ?: return null
                }
                coeffX += coeff
            } else if (t.contains("y")) {
                val coeffStr = t.replace("y", "")
                val coeff = when {
                    coeffStr.isEmpty() -> 1.0
                    coeffStr == "-" -> -1.0
                    coeffStr == "+" -> 1.0
                    else -> coeffStr.toDoubleOrNull() ?: return null
                }
                coeffY += coeff
            } else {
                val value = t.toDoubleOrNull() ?: return null
                constant += value
            }
        }

        // ax + by + constant = rightVal -> ax + by = rightVal - constant
        return QuadCoeffs(coeffX, coeffY, rightVal - constant)
    }

    // Polynomial Term for Calculus d/dx(...)
    data class PolynomialTerm(val coeff: Double, val pow: Double)

    private fun parsePolynomialTerms(expr: String): List<PolynomialTerm> {
        val sanitized = expr.replace("-", "+-").replace("\\s+".toRegex(), "")
        val rawTerms = sanitized.split("+")
        val list = mutableListOf<PolynomialTerm>()

        for (raw in rawTerms) {
            val term = raw.trim()
            if (term.isEmpty()) continue

            if (term.contains("x^")) {
                val parts = term.split("x^")
                val coeffStr = parts[0]
                val coeff = when {
                    coeffStr.isEmpty() -> 1.0
                    coeffStr == "-" -> -1.0
                    coeffStr == "+" -> 1.0
                    else -> coeffStr.toDoubleOrNull() ?: continue
                }
                val power = parts[1].toDoubleOrNull() ?: continue
                list.add(PolynomialTerm(coeff, power))
            } else if (term.contains("x")) {
                val coeffStr = term.replace("x", "")
                val coeff = when {
                    coeffStr.isEmpty() -> 1.0
                    coeffStr == "-" -> -1.0
                    coeffStr == "+" -> 1.0
                    else -> coeffStr.toDoubleOrNull() ?: continue
                }
                list.add(PolynomialTerm(coeff, 1.0))
            } else {
                val coeff = term.toDoubleOrNull() ?: continue
                list.add(PolynomialTerm(coeff, 0.0))
            }
        }
        return list
    }

    private fun combinePolynomialTerms(terms: List<PolynomialTerm>): String {
        if (terms.isEmpty()) return "0"
        val sorted = terms.filter { it.coeff != 0.0 }.sortedByDescending { it.pow }
        if (sorted.isEmpty()) return "0"

        val sb = StringBuilder()
        for (i in sorted.indices) {
            val t = sorted[i]
            val termStr = formatTerm(t.coeff, if (t.pow == 0.0) "" else (if (t.pow == 1.0) "x" else "x^" + t.pow.toInt()))

            if (i == 0) {
                sb.append(termStr)
            } else {
                if (t.coeff > 0) {
                    sb.append(" + ").append(termStr)
                } else {
                    sb.append(" - ").append(formatTerm(abs(t.coeff), if (t.pow == 0.0) "" else (if (t.pow == 1.0) "x" else "x^" + t.pow.toInt())))
                }
            }
        }
        return sb.toString()
    }

    private fun fallbackResult(eq: String, msg: String): SolveResult {
        return SolveResult(
            originalEquation = eq,
            finalAnswer = "Tap edit to modify equation",
            type = "Arithmetic",
            steps = listOf(
                SolveStep("Parsing Error", eq, msg)
            )
        )
    }
}

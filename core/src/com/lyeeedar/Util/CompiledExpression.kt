package com.lyeeedar.Util

import com.badlogic.gdx.utils.ObjectFloatMap
import com.badlogic.gdx.utils.ObjectSet
import squidpony.squidmath.LightRNG
import java.lang.Exception
import java.lang.RuntimeException
import java.lang.StringBuilder

class CompiledExpression(val expression: String)
{
	val variableNames = ObjectSet<String>()
	val functionNames = ObjectSet<String>()

	private val root: AbstractExpressionPart

	private val cachedValue: Float?
	private val sharedData = ExpressionData.get()

	init
	{
		try
		{
			root = parseExpressionPart(expression, this)
		}
		catch (ex: Exception)
		{
			throw RuntimeException("Failed to parse '$expression' due to ${ex.message}", ex)
		}

		if (variableNames.size == 0 && functionNames.size == 0)
		{
			cachedValue = root.evaluate(sharedData)
		}
		else
		{
			cachedValue = null
		}
	}

	fun evaluate(data: ExpressionData): Float
	{
		if (cachedValue != null) return cachedValue
		return root.evaluate(data)
	}

	fun evaluate(variables: ObjectFloatMap<String>, rng: LightRNG) = evaluate(sharedData.set(variables, rng))
	fun evaluate(variables: ObjectFloatMap<String>, seed: Long) = evaluate(variables, LightRNG(seed))
}

fun String.evaluate(variables: ObjectFloatMap<String> = ObjectFloatMap(), rng: LightRNG = LightRNG()): Float
{
	val parsed = CompiledExpression(this)
	val data = ExpressionData.get(variables, rng)

	return parsed.evaluate(data)
}

class ExpressionData private constructor()
{
	lateinit var rng: LightRNG
	lateinit var variables: ObjectFloatMap<String>

	fun set(variables: ObjectFloatMap<String>, rng: LightRNG): ExpressionData
	{
		this.rng = rng
		this.variables = variables

		return this
	}

	companion object
	{
		fun get(variables: ObjectFloatMap<String>, rng: LightRNG): ExpressionData = ExpressionData().set(variables, rng)
		fun get() = get(ObjectFloatMap(), LightRNG())
	}
}

abstract class AbstractExpressionPart(val rawPart: String)
{
	abstract fun evaluate(data: ExpressionData): Float
}

class NumberExpressionPart(val value: Float = 0f, rawPart: String) : AbstractExpressionPart(rawPart)
{
	override fun evaluate(data: ExpressionData): Float
	{
		return value
	}

	companion object
	{
		fun tryParse(rawPart: String): NumberExpressionPart?
		{
			val num = rawPart.toFloatOrNull()
			if (num == null) return null

			return NumberExpressionPart(num, rawPart)
		}
	}
}

class VariableExpressionPart(val variable: String, rawPart: String) : AbstractExpressionPart(rawPart)
{
	override fun evaluate(data: ExpressionData): Float
	{
		if (variable == "null") return 0f
		else if (variable == "else") return 1f

		return data.variables[variable, 0f]
	}

	companion object
	{
		fun tryParse(rawPart: String, compiledExpression: CompiledExpression): VariableExpressionPart?
		{
			if (rawPart.any { !it.isLetterOrDigit() && it != '.' })
			{
				return null
			}

			compiledExpression.variableNames.add(rawPart)
			return VariableExpressionPart(rawPart, rawPart)
		}
	}
}

class ExpressionExpressionPart(val leftSide: AbstractExpressionPart, val rightSide: AbstractExpressionPart, val operator: Operator, rawPart: String) : AbstractExpressionPart(rawPart)
{
	// in precedence order
	enum class Operator private constructor(val chars: String)
	{
		EQUALS("=="),
		NOT_EQUALS("!="),
		AND("&&"),
		OR("||"),

		LESS_THAN_OR_EQUAL("<="),
		GREATER_THAN_OR_EQUAL(">="),
		LESS_THAN("<"),
		GREATER_THAN(">"),

		ADD("+"),
		SUBTRACT("-"),

		MULTIPLY("*"),
		DIVIDE("/"),

		PERCENTAGE("%")
	}

	override fun evaluate(data: ExpressionData): Float
	{
		val lhs = leftSide.evaluate(data)
		val rhs = rightSide.evaluate(data)

		return when (operator)
		{
			Operator.MULTIPLY -> lhs * rhs
			Operator.DIVIDE -> lhs / rhs
			Operator.ADD -> lhs + rhs
			Operator.SUBTRACT -> lhs - rhs

			Operator.EQUALS -> if (lhs == rhs) 1f else 0f
			Operator.NOT_EQUALS -> if (lhs != rhs) 1f else 0f
			Operator.AND -> if (lhs != 0f && rhs != 0f) 1f else 0f
			Operator.OR -> if (lhs != 0f || rhs != 0f) 1f else 0f

			Operator.LESS_THAN -> if (lhs < rhs) 1f else 0f
			Operator.GREATER_THAN -> if (lhs > rhs) 1f else 0f
			Operator.LESS_THAN_OR_EQUAL -> if (lhs <= rhs) 1f else 0f
			Operator.GREATER_THAN_OR_EQUAL -> if (lhs >= rhs) 1f else 0f

			Operator.PERCENTAGE -> (lhs / 100f) * rhs
		}
	}

	companion object
	{
		fun tryParse(rawPart: String, compiledExpression: CompiledExpression): ExpressionExpressionPart?
		{
			for (op in Operator.values())
			{
				val exp = tryParse(rawPart, compiledExpression, op)
				if (exp != null) return exp
			}

			return null
		}

		fun tryParse(rawPart: String, compiledExpression: CompiledExpression, operator: Operator): ExpressionExpressionPart?
		{
			if (!rawPart.contains(operator.chars)) return null

			val lhs = StringBuilder()
			val rhs = StringBuilder()

			var current = lhs

			var parensDepth = 0
			for (i in 0 until rawPart.length)
			{
				val c = rawPart[i]

				if (c == '(')
				{
					parensDepth++
				}
				else if (c == ')')
				{
					parensDepth--
				}

				current.append(c)

				if (parensDepth == 0 && current == lhs)
				{
					if (current.endsWith(operator.chars))
					{
						current.delete(current.length - operator.chars.length, current.length)
						current = rhs
					}
				}
			}

			if (current != rhs || lhs.isEmpty() || rhs.isEmpty()) return null

			val leftSide = parseExpressionPart(lhs.toString(), compiledExpression)
			val rightSide = parseExpressionPart(rhs.toString(), compiledExpression)

			return ExpressionExpressionPart(leftSide, rightSide, operator, rawPart)
		}
	}
}

class FunctionExpressionPart(val function: String, val args: Array<AbstractExpressionPart>, rawPart: String) : AbstractExpressionPart(rawPart)
{
	override fun evaluate(data: ExpressionData): Float
	{
		return when(function)
		{
			"min" -> min(args[0].evaluate(data), args[1].evaluate(data))
			"max" -> max(args[0].evaluate(data), args[1].evaluate(data))

			"round" -> args[0].evaluate(data).round().toFloat()
			"floor" -> args[0].evaluate(data).floor().toFloat()
			"ciel" -> args[0].evaluate(data).ciel().toFloat()
			"clamp" -> args[0].evaluate(data).clamp(args[1].evaluate(data), args[2].evaluate(data))

			"chance" -> if (data.rng.nextFloat() * args[1].evaluate(data) <= args[0].evaluate(data)) 1f else 0f
			"rnd" -> data.rng.nextFloat() * args[1].evaluate(data)
			"rndSign" -> if (data.rng.nextBoolean()) 1f else -1f

			else -> throw RuntimeException("Unknown expression function '$function'")
		}
	}

	companion object
	{
		fun tryParse(rawPart: String, compiledExpression: CompiledExpression): FunctionExpressionPart?
		{
			if (!rawPart.endsWith(')')) return null
			if (!rawPart.contains('(')) return null

			val funcName = rawPart.split('(')[0]
			if (funcName.isBlank() || funcName.any { !it.isLetterOrDigit() }) return null

			val funcArgs = rawPart.substring(funcName.length+1)

			val args = com.badlogic.gdx.utils.Array<StringBuilder>()
			var current = StringBuilder()
			args.add(current)

			var parensDepth = 0
			for (i in 0 until funcArgs.length-1)
			{
				val c = funcArgs[i]

				if (c == '(')
				{
					parensDepth++
				}
				else if (c == ')')
				{
					parensDepth--
				}

				if (parensDepth == 0 && c == ',')
				{
					current = StringBuilder()
					args.add(current)
				}
				else
				{
					current.append(c)
				}
			}

			compiledExpression.functionNames.add(funcName)
			val parsedArgs = args.map { parseExpressionPart(it.toString(), compiledExpression) }.toTypedArray()
			return FunctionExpressionPart(funcName, parsedArgs, rawPart)
		}
	}
}

fun parseExpressionPart(part: String, compiledExpression: CompiledExpression): AbstractExpressionPart
{
	var part = part.trim()
	if (part.startsWith('(') && part.endsWith(')')) part = part.substring(1, part.length-1)

	val asNumber = NumberExpressionPart.tryParse(part)
	if (asNumber != null) return asNumber

	val asVariable = VariableExpressionPart.tryParse(part, compiledExpression)
	if (asVariable != null) return asVariable

	val asExpression = ExpressionExpressionPart.tryParse(part, compiledExpression)
	if (asExpression != null) return asExpression

	val asFunction = FunctionExpressionPart.tryParse(part, compiledExpression)
	if (asFunction != null) return asFunction

	throw RuntimeException("Unable to parse '$part'")
}
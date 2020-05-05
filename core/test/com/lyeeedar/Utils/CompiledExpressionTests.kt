package com.lyeeedar.Utils

import com.badlogic.gdx.utils.ObjectFloatMap
import com.lyeeedar.Util.CompiledExpression
import com.lyeeedar.Util.ExpressionData
import com.lyeeedar.Util.set
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CompiledExpressionTests
{
	@Test
	fun basicNumber()
	{
		val expression = CompiledExpression("4")
		assertEquals(4f, expression.evaluate(ExpressionData.get()))
	}

	@Test
	fun basicAddition()
	{
		val expression = CompiledExpression("4+4")
		assertEquals(8f, expression.evaluate(ExpressionData.get()))
	}

	@Test
	fun basicMultiplication()
	{
		val expression = CompiledExpression("4 *4")
		assertEquals(16f, expression.evaluate(ExpressionData.get()))
	}

	@Test
	fun operatorPrecedence()
	{
		val expression = CompiledExpression("4*4+4")
		assertEquals(20f, expression.evaluate(ExpressionData.get()))
	}

	@Test
	fun operatorPrecedenceParens()
	{
		val expression = CompiledExpression("4*(4+4)")
		assertEquals(32f, expression.evaluate(ExpressionData.get()))
	}

	@Test
	fun variable()
	{
		val data = ExpressionData.get()
		data.variables["pie"] = 6f

		val expression = CompiledExpression("pie")
		assertEquals(6f, expression.evaluate(data))
		assertEquals(1, expression.variableNames.size)
		assertEquals(0, expression.functionNames.size)
		assertEquals("pie", expression.variableNames.first())
	}

	@Test
	fun variableAndNumber()
	{
		val data = ExpressionData.get()
		data.variables["pie"] = 6f

		val expression = CompiledExpression("pie+5")
		assertEquals(11f, expression.evaluate(data))
		assertEquals(1, expression.variableNames.size)
		assertEquals(0, expression.functionNames.size)
		assertEquals("pie", expression.variableNames.first())
	}

	@Test
	fun variableAndNumberAndParens()
	{
		val data = ExpressionData.get()
		data.variables["pie"] = 1f

		val expression = CompiledExpression("pie+((5+2)+((pie)+pie*0))")
		assertEquals(9f, expression.evaluate(data))
		assertEquals(1, expression.variableNames.size)
		assertEquals(0, expression.functionNames.size)
		assertEquals("pie", expression.variableNames.first())
	}

	@Test
	fun unknownVariable()
	{
		val data = ExpressionData.get()
		data.variables["apple"] = 6f

		val expression = CompiledExpression("pie")
		assertEquals(0f, expression.evaluate(data))
		assertEquals(1, expression.variableNames.size)
		assertEquals(0, expression.functionNames.size)
		assertEquals("pie", expression.variableNames.first())
	}

	@Test
	fun min()
	{
		val data = ExpressionData.get()
		data.variables["pie"] = 6f

		val expression = CompiledExpression("min(pie, 3)")
		assertEquals(3f, expression.evaluate(data))
		assertEquals(1, expression.variableNames.size)
		assertEquals(1, expression.functionNames.size)
		assertEquals("pie", expression.variableNames.first())
		assertEquals("min", expression.functionNames.first())
	}

	@Test
	fun operatorEquals()
	{
		val expression = CompiledExpression("4==4")
		assertEquals(1f, expression.evaluate(ExpressionData.get()))
	}

	@Test
	fun operatorGTE()
	{
		val data = ExpressionData.get()
		data.variables["attackCount"] = 4f
		val expression = CompiledExpression("attackCount>=2")
		assertEquals(1f, expression.evaluate(data))
	}

	@Test
	fun dottedVariable()
	{
		val data = ExpressionData.get()
		data.variables["source.damage"] = 4f
		val expression = CompiledExpression("source.damage * 2")
		assertEquals(8f, expression.evaluate(data))
	}

	@Test
	fun booleanEquals()
	{
		val expression = CompiledExpression("cheese == null")
		assertEquals(1f, expression.evaluate(ExpressionData.get()))
	}

	@Test
	fun booleanNotEquals()
	{
		val expression = CompiledExpression("cheese != null")
		assertEquals(0f, expression.evaluate(ExpressionData.get()))
	}

	@Test
	fun elseVariable()
	{
		val expression = CompiledExpression("else")
		assertEquals(1f, expression.evaluate(ExpressionData.get()))
	}

	@Test
	fun randomVariable()
	{
		val expression = CompiledExpression("random")
		assertNotEquals(0f, expression.evaluate(ObjectFloatMap(), seed = 0))
	}

	@Test
	fun booleanOr()
	{
		val data = ExpressionData.get()
		assertEquals(0f, CompiledExpression("cheese || pie").evaluate(data))
		data.variables["cheese"] = 1f
		assertEquals(1f, CompiledExpression("cheese || pie").evaluate(data))
	}

	@Test
	fun booleanAnd()
	{
		val data = ExpressionData.get()
		data.variables["pie"] = 1f
		assertEquals(0f, CompiledExpression("cheese && pie").evaluate(data))
		data.variables["cheese"] = 1f
		assertEquals(1f, CompiledExpression("cheese && (pie)").evaluate(data))
	}
}
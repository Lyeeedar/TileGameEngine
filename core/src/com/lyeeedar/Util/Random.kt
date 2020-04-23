package com.lyeeedar.Util

import com.badlogic.gdx.utils.Pool
import squidpony.squidmath.LightRNG

class Random
{
	companion object
	{
		val sharedRandom = LightRNG()

		private val pool = object : Pool<LightRNG>() {
			override fun newObject(): LightRNG
			{
				return LightRNG()
			}

		}

		fun obtainTS(seed: Long): LightRNG
		{
			synchronized(pool)
			{
				val item = pool.obtain()
				item.setSeed(seed)
				return item
			}
		}

		fun freeTS(ran: LightRNG)
		{
			synchronized(pool)
			{
				pool.free(ran)
			}
		}

		fun sign(rng: LightRNG): Float
		{
			return if (rng.nextBoolean()) 1.0f else -1.0f
		}

		fun randomWeighted(rng: LightRNG): Float
		{
			val ran = random(rng)
			return ran * ran
		}

		fun random(rng: LightRNG): Float
		{
			return rng.nextFloat()
		}

		fun random(rng: LightRNG, max: Float): Float
		{
			return rng.nextFloat() * max
		}

		fun random(rng: LightRNG, min: Float, max: Float): Float
		{
			return rng.nextFloat() * (max - min) + min
		}

		fun random(rng: LightRNG, min: Int, max: Int): Int
		{
			return rng.nextInt(min, max)
		}

		fun random(rng: LightRNG, max: Int): Int
		{
			val ranVal = rng.nextInt(max)
			if (ranVal > max) throw Exception("Random broke!")
			return ranVal
		}
	}
}

fun LightRNG.freeTS()
{
	Random.freeTS(this)
}

fun LightRNG.nextFloat(value: Float) = this.nextFloat() * value
fun LightRNG.sign() = Random.sign(this)
fun LightRNG.randomWeighted() = Random.randomWeighted(this)
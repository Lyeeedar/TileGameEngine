package com.lyeeedar.Util

class FastEnumMap<T : Enum<T>, V> : Iterable<V>, Map<T, V>
{
	override var size = 0
	lateinit var keyType: Class<T>
	private lateinit var items: Array<V?>

	override val entries: Set<Map.Entry<T, V>> = keyType.enumConstants.filter { items[it.ordinal] != null }.map { FastEnumMapEntry<T, V>(it, items[it.ordinal]!!) }.toSet()
	override val keys: Set<T> = keyType.enumConstants.filter { items[it.ordinal] != null }.toSet()
	override val values: Collection<V> = items.mapNotNull { it }.toList()

	constructor(keyType: Class<T>)
	{
		this.keyType = keyType
		items = arrayOfNulls<Any>(keyType.enumConstants.size) as Array<V?>
	}

	constructor(other: FastEnumMap<T, V>)
	{
		keyType = other.keyType
		items = arrayOfNulls<Any>(keyType.enumConstants.size) as Array<V?>

		addAll(other)
	}

	fun numItems(): Int
	{
		return items.size
	}

	fun calculateSize()
	{
		var count = 0
		for (i in items.indices)
		{
			if (items[i] != null)
			{
				count++
			}
		}
		size = count
	}

	override fun isEmpty(): Boolean = size == 0

	fun remove(key: T)
	{
		items[key.ordinal] = null
		calculateSize()
	}

	fun clear()
	{
		for (i in items.indices) items[i] = null
		calculateSize()
	}

	operator fun set(key: T, value: V)
	{
		items[key.ordinal] = value
		calculateSize()
	}

	override operator fun get(key: T): V?
	{
		return items[key.ordinal]
	}

	operator fun get(key: T, defaultValue: V): V
	{
		return items[key.ordinal] ?: return defaultValue
	}

	operator fun get(index: Int): V?
	{
		return items[index]
	}

	override fun containsKey(key: T): Boolean
	{
		return items[key.ordinal] != null
	}

	fun remove(index: Int)
	{
		items[index] = null
		calculateSize()
	}

	fun put(key: T, value: V?)
	{
		items[key.ordinal] = value
		calculateSize()
	}

	fun put(index: Int, value: V?)
	{
		items[index] = value
		calculateSize()
	}

	fun addAll(other: FastEnumMap<T, V>)
	{
		for (key in keyType.enumConstants)
		{
			put(key, other[key])
		}
	}

	fun containsKey(index: Int): Boolean
	{
		return items[index] != null
	}

	override fun containsValue(value: V): Boolean = values.contains(value)

	fun copy(): FastEnumMap<T, V>
	{
		return FastEnumMap(this)
	}

	override fun iterator(): MutableIterator<V>
	{
		return FastEnumMapIterator(this)
	}

	private inner class FastEnumMapIterator(obj: FastEnumMap<T, V>?) : MutableIterator<V>
	{
		var i = 0
		var prev = 0
		var obj: FastEnumMap<T, V>? = null
		fun reset(obj: FastEnumMap<T, V>?): FastEnumMapIterator
		{
			i = 0
			prev = 0
			this.obj = obj
			if (size != 0)
			{
				while (i < items.size)
				{
					if (items[i] != null)
					{
						break
					}
					i++
				}
				prev = i
			}
			return this
		}

		override fun hasNext(): Boolean
		{
			return if (size == 0 || i >= items.size) false else items[i] != null
		}

		override fun next(): V
		{
			prev = i++
			while (i < items.size)
			{
				if (items[i] != null)
				{
					break
				}
				i++
			}
			return items[prev]!!
		}

		override fun remove()
		{
			obj!!.remove(prev)
		}

		init
		{
			reset(obj)
		}
	}

	class FastEnumMapEntry<T, V>(override val key: T, override val value: V): Map.Entry<T, V>
}
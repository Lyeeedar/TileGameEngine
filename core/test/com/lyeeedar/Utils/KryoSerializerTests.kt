package com.lyeeedar.Utils

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Logger
import com.badlogic.gdx.utils.ObjectMap
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Registration
import com.esotericsoftware.kryo.util.DefaultClassResolver
import com.esotericsoftware.kryo.util.IntMap
import com.esotericsoftware.minlog.Log
import com.kryo.deserialize
import com.kryo.initKryo
import com.kryo.serialize
import com.kryo.sharedKryo
import com.lyeeedar.Components.ComponentType
import com.lyeeedar.Components.Entity
import com.lyeeedar.Components.EntityPool
import com.lyeeedar.Direction
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.*
import ktx.collections.set
import org.junit.Assert.*
import org.junit.Test
import java.lang.RuntimeException

class KryoSerializerTests
{
	@Test
	fun initialisingKryo()
	{
		val kryo = initKryo()
		assertEquals(true, kryo.isRegistrationRequired)
		assertEquals(true, kryo.references)
	}

	@Test
	fun gdxArray()
	{
		val kryo = sharedKryo

		val array = Array<String>()
		array.add("this")
		array.add("is")
		array.add("working")

		val serialized = serialize(array, kryo)
		val deserialized = deserialize(serialized, Array::class.java, kryo) as Array<String>

		assertEquals(array.size, deserialized.size)
		assertEquals(array[0], deserialized[0])
		assertEquals(array[1], deserialized[1])
		assertEquals(array[2], deserialized[2])
	}

	@Test
	fun gdxObjectMap()
	{
		val kryo = sharedKryo

		val map = ObjectMap<String, Array<String>>()
		map["pie"] = Array()

		val serialized = serialize(map, kryo)
		val deserialized = deserialize(serialized, ObjectMap::class.java, kryo) as ObjectMap<String, Array<String>>

		assertEquals(map.size, deserialized.size)
		assertEquals(map["pie"].size, deserialized["pie"].size)
	}

	@Test
	fun fastEnumMap()
	{
		val kryo = sharedKryo

		val map = FastEnumMap<Direction, Float>(Direction::class.java)
		map[Direction.CENTER] = 1f
		map[Direction.NORTH] = 2f

		val serialized = serialize(map, kryo)
		val deserialized = deserialize(serialized, FastEnumMap::class.java, kryo) as FastEnumMap<Direction, Float>

		assertEquals(2, deserialized.size)
		assertEquals(1f, deserialized[Direction.CENTER])
		assertEquals(2f, deserialized[Direction.NORTH])
		assertEquals(null, deserialized[Direction.SOUTH])
	}

	@Test
	fun references()
	{
		val kryo = sharedKryo

		val entity = EntityPool.obtain()
		entity.addComponent(ComponentType.Position)
		entity.addComponent(ComponentType.Renderable)
		entity.addComponent(ComponentType.Name)

		val entity2 = EntityPool.obtain()
		entity2.addComponent(ComponentType.Position)
		entity2.addComponent(ComponentType.Renderable)

		val map = FastEnumMap<SpaceSlot, Entity>(SpaceSlot::class.java)
		map[SpaceSlot.ENTITY] = entity
		map[SpaceSlot.ABOVEENTITY] = entity
		map[SpaceSlot.FLOOR] = entity2

		val serialized = serialize(map, kryo)
		val deserialized = deserialize(serialized, FastEnumMap::class.java, kryo) as FastEnumMap<SpaceSlot, Entity>

		assertEquals(3, deserialized.size)
		assertNull(deserialized[SpaceSlot.WALL])
		assertNotNull(deserialized[SpaceSlot.ENTITY])

		assertEquals(deserialized[SpaceSlot.ENTITY], deserialized[SpaceSlot.ABOVEENTITY])
		assertNotEquals(deserialized[SpaceSlot.ENTITY], deserialized[SpaceSlot.FLOOR])
	}

	@Test
	fun initIds()
	{
		val resolver1 = TestClassResolver()
		val kryo1 = Kryo(resolver1, null)
		kryo1.registerGdxSerialisers()
		kryo1.registerLyeeedarSerialisers()
		kryo1.registerGameSerializers()

		val resolver2 = TestClassResolver()
		val kryo2 = Kryo(resolver2, null)
		kryo2.registerGameSerializers()
		kryo2.registerLyeeedarSerialisers()
		kryo2.registerGdxSerialisers()

		assertEquals(resolver1.accessIdToRegistration().size, resolver2.accessIdToRegistration().size)

		val itr = resolver1.accessIdToRegistration().keys()
		while (itr.hasNext)
		{
			val id = itr.next()
			assertEquals(resolver1.accessIdToRegistration()[id].type, resolver2.accessIdToRegistration()[id].type)
		}

		val point = Point(4, 20)
		val array = Array<Point>()
		array.add(point)

		val serialized = serialize(array, kryo1)
		val deserialized = deserialize(serialized, Array::class.java, kryo2) as Array<Point>

		assertEquals(1, deserialized.size)
		assertEquals(4, deserialized[0].x)
		assertEquals(20, deserialized[0].y)
	}

	class TestClassResolver : DefaultClassResolver()
	{
		fun accessIdToRegistration(): IntMap<Registration> = idToRegistration

		override fun unregister(classID: Int): Registration?
		{
			val existing = super.unregister(classID)
			if (existing != null) throw RuntimeException("Id $classID already registered as ${existing.type}!")
			return existing
		}
	}
}
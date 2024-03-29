package com.lyeeedar.Util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.XmlReader
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import ktx.collections.set
import java.util.*

class XmlData
{
	lateinit var name: String
	var nameId: Int = -1

	var children: Array<XmlData> = Array(0){_ -> XmlData()}
	val childMap: IntMap<XmlData> = IntMap()
	var lastIndex = 0

	val childCount: Int
		get() = children.size

	var attributeMap: IntMap<XmlAttributeData> = IntMap()

	var value: Any? = null

	constructor()
	{
	}

	constructor(handle: FileHandle)
	{
		load(handle)
	}

	constructor(name: String, data: String)
	{
		this.name = name
		this.nameId = name.uppercase(Locale.ENGLISH).hashCode()
		this.value = data
	}

	fun getChild(index: Int) = children[index]

	fun children(): Array<XmlData> = children

	fun getChildByName(name: String): XmlData?
	{
		return getChildById(name.uppercase(Locale.ENGLISH).hashCode())
	}

	fun getChildrenByName(name: String): Sequence<XmlData>
	{
		val id = name.uppercase(Locale.ENGLISH).hashCode()
		return sequence {
			for (child in children)
			{
				if (child.nameId == id)
				{
					yield(child)
				}
			}
		}
	}

	fun getChildById(id: Int): XmlData?
	{
		if (children.isEmpty()) return null

		lastIndex++
		if (lastIndex == children.size) lastIndex = 0

		if (children[lastIndex].nameId == id)
		{
			return children[lastIndex]
		}

		return childMap[id]
	}

	fun descendants(): Sequence<XmlData>
	{
		val thisEl = this
		return sequence {
			yield(thisEl)

			for (child in children)
			{
				for (desc in child.descendants())
				{
					yield(desc)
				}
			}
		}
	}

	fun get(name: String): String
	{
		return getChildByName(name)?.text ?: throw GdxRuntimeException("Element ${this.name} has no child called $name!")
	}

	fun get(name: String, fallback: String?): String?
	{
		return getChildByName(name)?.text ?: fallback
	}

	fun getInt(name: String): Int
	{
		return getChildByName(name)?.int() ?: throw GdxRuntimeException("Element ${this.name} has no child called $name!")
	}

	fun getInt(name: String, fallback: Int): Int
	{
		return getChildByName(name)?.int() ?: fallback
	}

	fun getFloat(name: String): Float
	{
		return getChildByName(name)?.float() ?: throw GdxRuntimeException("Element ${this.name} has no child called $name!")
	}

	fun getFloat(name: String, fallback: Float): Float
	{
		return getChildByName(name)?.float() ?: fallback
	}

	fun getBoolean(name: String): Boolean
	{
		return getChildByName(name)?.boolean() ?: throw GdxRuntimeException("Element ${this.name} has no child called $name!")
	}

	fun getBoolean(name: String, fallback: Boolean): Boolean
	{
		return getChildByName(name)?.boolean() ?: fallback
	}

	fun getLong(name: String): Long
	{
		return getChildByName(name)?.long() ?: throw GdxRuntimeException("Element ${this.name} has no child called $name!")
	}

	fun getLong(name: String, fallback: Long): Long
	{
		return getChildByName(name)?.long() ?: fallback
	}

	fun getPoint(name: String): Point
	{
		val str = get(name)
		val split = str.split(",")
		val x = split[0].toInt()
		val y = split[1].toInt()

		return Point(x, y)
	}

	fun getPoint(name: String, fallback: Point): Point
	{
		val str = get(name, null) ?: return fallback
		val split = str.split(",")
		val x = split[0].toInt()
		val y = split[1].toInt()

		return Point(x, y)
	}

	fun getAttribute(name: String): String
	{
		return attributeMap[name.uppercase(Locale.ENGLISH)
			.hashCode()]?.text() ?: throw GdxRuntimeException("Element ${this.name} has no attribute called $name!")
	}

	fun getAttribute(name: String, fallback: String?): String?
	{
		return attributeMap[name.uppercase(Locale.ENGLISH).hashCode()]?.text() ?: fallback
	}

	fun getAttributeInt(name: String): Int
	{
		return attributeMap[name.uppercase(Locale.ENGLISH)
			.hashCode()]?.int() ?: throw GdxRuntimeException("Element ${this.name} has no attribute called $name!")
	}

	fun getAttributeInt(name: String, fallback: Int): Int
	{
		return attributeMap[name.uppercase(Locale.ENGLISH).hashCode()]?.int() ?: fallback
	}

	fun getAttributeFloat(name: String): Float
	{
		return attributeMap[name.uppercase(Locale.ENGLISH)
			.hashCode()]?.float() ?: throw GdxRuntimeException("Element ${this.name} has no attribute called $name!")
	}

	fun getAttributeFloat(name: String, fallback: Float): Float
	{
		return attributeMap[name.uppercase(Locale.ENGLISH).hashCode()]?.float() ?: fallback
	}

	fun getAttributeBoolean(name: String): Boolean
	{
		return attributeMap[name.uppercase(Locale.ENGLISH)
			.hashCode()]?.boolean() ?: throw GdxRuntimeException("Element ${this.name} has no attribute called $name!")
	}

	fun getAttributeBoolean(name: String, fallback: Boolean): Boolean
	{
		return attributeMap[name.uppercase(Locale.ENGLISH).hashCode()]?.boolean() ?: fallback
	}

	fun set(name: String, value: Any)
	{
		val holder = XmlData()
		holder.name = name
		holder.nameId = name.hashCode()
		holder.value = value

		addChild(holder)
	}

	fun addChild(xmlData: XmlData)
	{
		children = Array(childCount+1) { i -> if (i < childCount) children[i] else xmlData }
	}

	fun addChild(name: String): XmlData
	{
		val holder = XmlData()
		holder.name = name
		holder.nameId = name.hashCode()

		addChild(holder)

		return holder
	}

	val text: String
			get() = value?.toString() ?: ""
	fun int(): Int = value as? Int ?: value.toString().toIntOrNull() ?: throw TypeCastException("Cannot cast $value to an Int!")
	fun float(): Float = value as? Float ?: value.toString().toFloatOrNull() ?: throw TypeCastException("Cannot cast $value to a Float!")
	fun boolean(): Boolean = value as? Boolean ?: value.toString().toBoolean()
	fun long(): Long = value as? Long ?: value.toString().toLong()

	fun save(path: String)
	{
		Gdx.files.local(path).write(false).use {
			Output(it).use {
				save(it)
			}
		}
	}

	fun save(output: Output)
	{
		output.writeString(name)
		output.writeInt(children.size, true)

		if (children.isNotEmpty())
		{
			for (child in children)
			{
				child.save(output)
			}
		}
		else
		{
			when (value)
			{
				is Int ->
				{
					output.writeShort(0)
					output.writeInt(value as Int)
				}
				is Float ->
				{
					output.writeShort(1)
					output.writeFloat(value as Float)
				}
				is Boolean ->
				{
					output.writeShort(2)
					output.writeBoolean(value as Boolean)
				}
				is Long ->
				{
					output.writeShort(4)
					output.writeLong(value as Long)
				}
				else ->
				{
					if (value == null)
					{
						value = ""
					}

					output.writeShort(3)
					output.writeString(value as String)
				}
			}
		}

		output.writeInt(attributeMap.size, true)
		for (att in attributeMap)
		{
			att.value.save(output)
		}
	}

	fun load(path: String)
	{
		load(Gdx.files.internal(path))
	}

	fun load(handle: FileHandle)
	{
		handle.read().use {
			Input(it).use {
				load(it)
			}
		}
	}

	fun load(input: Input)
	{
		name = input.readString()
		nameId = name.uppercase(Locale.ENGLISH).hashCode()

		val childCount = input.readInt(true)
		children = Array(childCount) { e -> XmlData() }

		if (childCount > 0)
		{
			for (i in 0 until childCount)
			{
				val child = XmlData()
				child.load(input)
				children[i] = child
				childMap[child.nameId] = child
			}
		} else
		{
			val valueType = input.readShort().toInt()
			value = when (valueType)
			{
				0 -> input.readInt()
				1 -> input.readFloat()
				2 -> input.readBoolean()
				3 -> input.readString()
				4 -> input.readLong()
				else -> throw RuntimeException("Unknown xml data type '$valueType'!")
			}
		}

		val attCount = input.readInt(true)
		attributeMap = IntMap()
		for (i in 0 until attCount)
		{
			val att = XmlAttributeData()
			att.load(input)
			attributeMap[att.nameId] = att
		}
	}

	companion object
	{
		val cachedXml = ObjectMap<String, XmlData>()
		var existingPaths: com.badlogic.gdx.utils.Array<String>? = null

		fun getXml(path: String): XmlData
		{
			val existing = cachedXml.get(path, null)
			if (existing != null) { return existing }

			var filepath = path

			filepath = filepath.replace("\\", "/")

			filepath = "CompressedData/" + filepath.hashCode() + ".xmldata"

			try
			{
				var handle = Gdx.files.internal(filepath)
				if (!handle.exists()) handle = Gdx.files.absolute(filepath)

				val loaded = XmlData(handle)

				cachedXml[path] = loaded

				return loaded
			}
			catch (ex: Exception)
			{
				throw Exception("Failed to load file: '$path'. Reason: ${ex.message}")
			}
		}

		fun loadFromElement(el: XmlReader.Element): XmlData
		{
			val data = XmlData()
			data.name = el.name
			data.nameId = data.name.uppercase(Locale.ENGLISH).hashCode()

			if (el.childCount == 0 && el.text == null)
			{
				data.value = ""
			} else if (el.childCount == 0)
			{
				val intVal = el.text.toIntOrNull()
				if (intVal != null)
				{
					data.value = intVal
				} else
				{
					val floatVal = el.text.toFloatOrNull()
					if (floatVal != null)
					{
						data.value = floatVal
					} else
					{
						val boolVal = when (el.text.lowercase(Locale.ENGLISH))
						{
							"true" -> true
							"false" -> false
							else -> null
						}
						if (boolVal != null)
						{
							data.value = boolVal
						} else
						{
							data.value = el.text
						}
					}
				}
			} else
			{
				data.children = Array(el.childCount) { e -> loadFromElement(el.getChild(e)) }
				for (child in data.children)
				{
					data.childMap[child.nameId] = child
				}
			}

			if ((el.attributes?.size ?: 0) > 0)
			{
				data.attributeMap = IntMap()
				for (att in el.attributes)
				{
					val attdata = XmlAttributeData.load(att.key, att.value)
					data.attributeMap[attdata.nameId] = attdata
				}
			} else
			{
				data.attributeMap = IntMap()
			}

			return data
		}

		fun getExistingPaths(): Sequence<String>
		{
			if (existingPaths == null)
			{
				existingPaths = com.badlogic.gdx.utils.Array()

				val xml = getXml("ProcessedPaths.xml")
				for (el in xml.children)
				{
					existingPaths?.add(el.text)
				}
			}

			return sequence {
				for (i in 0 until existingPaths!!.size)
				{
					yield(existingPaths!![i])
				}
			}
		}

		fun enumeratePaths(base: String, type: String): Sequence<String>
		{
			if (existingPaths == null)
			{
				existingPaths = com.badlogic.gdx.utils.Array()

				val xml = getXml("ProcessedPaths.xml")
				for (el in xml.children)
				{
					existingPaths?.add(el.text)
				}
			}

			return sequence {
				for (i in 0 until existingPaths!!.size)
				{
					val path = existingPaths!![i]

					if (path.startsWith(base, true))
					{
						val xml = getXml(path)
						if (xml.name == type)
						{
							yield(path)
						}
					}
				}
			}
		}

		fun performanceTest()
		{
			val runs = 10000

			val rawXml = getRawXml("UnlockTrees/Fire.xml")
			val dataXml = loadFromElement(rawXml)

			dataXml.save("test.xmldata")

			fun profile(lambda: () -> Unit, message: String)
			{
				val start = System.currentTimeMillis()
				for (i in 0..runs)
				{
					lambda.invoke()
				}
				val end = System.currentTimeMillis()
				val diff = end - start

				System.out.println("$message completed in {$diff}ms")
			}

			// get child by name
			profile({rawXml.getChildByName("Abilities")}, "Element.getChildByName")
			profile({dataXml.getChildByName("Abilities")}, "XmlData.getChildByName")

			// get string child
			val elAb = rawXml.getChildByName("Abilities").getChild(0).getChildByName("AbilityData")
			val daAb = dataXml.getChildByName("Abilities")!!.children[0].getChildByName("AbilityData")!!
			profile({elAb.get("UnboughtDescription")}, "Element.get")
			profile({daAb.get("UnboughtDescription")}, "XmlData.get")

			// get int child
			val elEd = elAb.getChildByName("EffectData")
			val daEd = daAb.getChildByName("EffectData")!!
			profile({elEd.getInt("Cost")}, "Element.getInt")
			profile({daEd.getInt("Cost")}, "XmlData.getInt")

			// textload
			profile({getXml("UnlockTrees/Fire.xml")}, "Element.load")
			profile({XmlData().load("test.xmldata")}, "XmlData.load")

			// get string attribute

			// gete int attribute
		}
	}
}

class XmlAttributeData
{
	lateinit var name: String
	var nameId: Int = -1

	var value: Any? = null

	fun text(): String = value.toString()
	fun int(): Int = value as? Int ?: value.toString().toIntOrNull() ?: throw TypeCastException("Cannot cast $value to an Int!")
	fun float(): Float = value as? Float ?: value.toString().toFloatOrNull() ?: throw TypeCastException("Cannot cast $value to a Float!")
	fun boolean(): Boolean = value as? Boolean ?: value.toString().toBoolean()

	fun save(output: Output)
	{
		output.writeString(name)
		when (value)
		{
			is Int ->
			{
				output.writeShort(0)
				output.writeInt(value as Int)
			}
			is Float ->
			{
				output.writeShort(1)
				output.writeFloat(value as Float)
			}
			is Boolean ->
			{
				output.writeShort(2)
				output.writeBoolean(value as Boolean)
			}
			else ->
			{
				output.writeShort(3)
				output.writeString(value as String)
			}
		}
	}

	fun load(input: Input)
	{
		name = input.readString()
		nameId = name.uppercase(Locale.ENGLISH).hashCode()

		val valueType = input.readShort().toInt()
		value = when (valueType)
		{
			0 -> input.readInt()
			1 -> input.readFloat()
			2 -> input.readBoolean()
			3 -> input.readString()
			else -> throw RuntimeException("Unknown xml data type '$valueType'!")
		}
	}

	companion object
	{
		fun load(name: String, rawvalue: String): XmlAttributeData
		{
			val data = XmlAttributeData()
			data.name = name
			data.nameId = name.uppercase(Locale.ENGLISH).hashCode()

			val floatVal = rawvalue.toFloatOrNull()
			if (floatVal != null)
			{
				data.value = floatVal
			} else
			{
				val intVal = rawvalue.toIntOrNull()
				if (intVal != null)
				{
					data.value = intVal
				} else
				{
					val boolVal = when (rawvalue.lowercase(Locale.ENGLISH))
					{
						"true" -> true
						"false" -> false
						else -> null
					}
					if (boolVal != null)
					{
						data.value = boolVal
					} else
					{
						data.value = rawvalue
					}
				}
			}

			return data
		}
	}
}

abstract class XmlDataClass
{
	open fun load(xmlData: XmlData) {}
	open fun afterLoad() { }
}
abstract class GraphXmlDataClass<T> : XmlDataClass()
{
	open fun resolve(nodes: ObjectMap<String, T>) {}
}

annotation class DataFile(val colour: String = "", val icon: String = "")
annotation class DataClass(val name: String = "", val category: String = "", val colour: String = "", val global: Boolean = false, val implementsStaticLoad: Boolean = false)
annotation class DataNumericRange(val min: Float = -9999999f, val max: Float = 9999999f)
annotation class DataValue(val dataName: String = "", val visibleIf: String = "")
annotation class DataVector(val name1: String = "", val name2: String = "", val name3: String = "", val name4: String = "")
annotation class DataNeedsLocalisation(val file: String = "")
annotation class DataArray(val minCount: Int = 0, val maxCount: Int = 9999999, val childrenAreUnique: Boolean = false)
annotation class DataFileReference(val basePath: String = "", val stripExtension: Boolean = true, val resourceType: String = "", val allowedFileTypes: String = "")
annotation class DataCompiledExpression(val createExpressionMethod: String = "", val knownVariables: String = "", val default: String = "")
annotation class DataGraphNodes()
annotation class DataGraphNode()
annotation class DataClassCollection()
annotation class DataGraphReference(val useParentDescription: Boolean = false, val elementIsChild: Boolean = false)
annotation class DataXml(val actualClass: String = "")
annotation class DataAsciiGrid
annotation class DataTimeline(val timelineGroup: Boolean = false)
annotation class DataLayeredSprite()

expect class XmlDataClassLoader
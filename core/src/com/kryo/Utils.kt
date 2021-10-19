package com.kryo

import com.badlogic.gdx.Gdx
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.lyeeedar.Util.registerGameSerializers
import com.lyeeedar.Util.registerGdxSerialisers
import com.lyeeedar.Util.registerLyeeedarSerialisers

val sharedKryo: Kryo by lazy { initKryo() }
fun initKryo(): Kryo
{
	val kryo = Kryo()
	kryo.references = true
	kryo.registerGdxSerialisers()
	kryo.registerLyeeedarSerialisers()
	kryo.registerGameSerializers()

	return kryo
}

fun serialize(obj: Any, path: String, kryo: Kryo? = null)
{
	val kryo = kryo ?: sharedKryo

	try
	{
		Gdx.files.local(path).write(false).use {
			Output(it).use {
				kryo.writeObject(it, obj)
			}
		}
	}
	catch (e: Exception)
	{
		e.printStackTrace()
		return
	}
}

fun serialize(obj: Any, kryo: Kryo? = null): ByteArray
{
	val kryo = kryo ?: sharedKryo

	try
	{
		Output(128, -1).use {
			kryo.writeObject(it, obj)

			return it.buffer
		}
	}
	catch (e: Exception)
	{
		e.printStackTrace()
		throw e
	}
}

fun <T> deserialize(byteArray: ByteArray, clazz: Class<T>, kryo: Kryo? = null): T
{
	val kryo = kryo ?: sharedKryo

	val data: T
	try
	{
		Input(byteArray).use {
			data = kryo.readObject(it, clazz)
		}
	}
	catch (e: Exception)
	{
		e.printStackTrace()
		throw e
	}

	return data
}

fun <T> deserialize(path: String, clazz: Class<T>, kryo: Kryo? = null): T?
{
	val kryo = kryo ?: sharedKryo

	var data: T?
	try
	{
		Gdx.files.local(path).read().use {
			Input(it).use {
				data = kryo.readObject(it, clazz)
			}
		}
	}
	catch (e: Exception)
	{
		e.printStackTrace()
		data = null
	}

	return data
}
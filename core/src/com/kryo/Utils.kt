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

	val outputFile = Gdx.files.local(path)

	var output: Output? = null
	try
	{
		output = Output(outputFile.write(false))
	}
	catch (e: Exception)
	{
		e.printStackTrace()
		return
	}

	kryo.writeObject(output, obj)

	output.close()
}

fun serialize(obj: Any, kryo: Kryo? = null): ByteArray
{
	val kryo = kryo ?: sharedKryo

	val output: Output
	try
	{
		output = Output(128, -1)
	}
	catch (e: Exception)
	{
		e.printStackTrace()
		throw e
	}

	kryo.writeObject(output, obj)

	output.close()

	return output.buffer
}

fun <T> deserialize(byteArray: ByteArray, clazz: Class<T>, kryo: Kryo? = null): T
{
	val kryo = kryo ?: sharedKryo

	var input: Input? = null

	val data: T
	try
	{
		input = Input(byteArray)
		data = kryo.readObject(input, clazz)
	}
	catch (e: Exception)
	{
		e.printStackTrace()
		throw e
	}
	finally
	{
		input?.close()
	}

	return data
}

fun <T> deserialize(path: String, clazz: Class<T>, kryo: Kryo? = null): T?
{
	val kryo = kryo ?: sharedKryo

	var input: Input? = null

	var data: T?
	try
	{
		input = Input(Gdx.files.local(path).read())
		data = kryo.readObject(input, clazz)
	}
	catch (e: Exception)
	{
		e.printStackTrace()
		data = null
	}
	finally
	{
		input?.close()
	}

	return data
}
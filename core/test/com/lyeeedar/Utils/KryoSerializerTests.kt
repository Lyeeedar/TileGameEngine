package com.lyeeedar.Utils

import com.esotericsoftware.kryo.Kryo
import com.lyeeedar.Util.registerGameSerializers
import com.lyeeedar.Util.registerGdxSerialisers
import com.lyeeedar.Util.registerLyeeedarSerialisers
import org.junit.Test

class KryoSerializerTests
{
	@Test
	fun initialisingKryo()
	{
		val kryo = Kryo()
		kryo.isRegistrationRequired = true

		kryo.registerGdxSerialisers()
		kryo.registerLyeeedarSerialisers()
		kryo.registerGameSerializers()
	}
}
package com.lyeeedar.Renderables

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClass

class Shadow : XmlDataClass()
{
	lateinit var texture: TextureRegion
	var scale: Float = 1f
	var offset: Vector2 = Vector2()

	//region non-data
	var positions = Array<Vector2>()
	var queuedPositions = 0
	var queuedBatchID = 0
	//endregion

	fun copy(): Shadow
	{
		val out = Shadow()
		out.texture = texture
		out.scale = scale

		return out
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		texture = AssetManager.loadTextureRegion(xmlData.getChildByName("Texture")!!)
		scale = xmlData.getFloat("Scale", 1f)
		val offsetRaw = xmlData.get("Offset", "0,0")!!.split(',')
		offset = Vector2(offsetRaw[0].trim().toFloat(), offsetRaw[1].trim().toFloat())
	}
	//endregion
}
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
	lateinit var colour: Colour
	var scale: Float = 1f

	//region non-data
	var positions = Array<Vector2>()
	var queuedPositions = 0
	var queuedBatchID = 0
	//endregion

	fun copy(): Shadow
	{
		val out = Shadow()
		out.texture = texture
		out.colour = colour.copy()
		out.scale = scale

		return out
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		texture = AssetManager.loadTextureRegion(xmlData.getChildByName("Texture")!!)
		colour = AssetManager.loadColour(xmlData.getChildByName("Colour")!!)
		scale = xmlData.getFloat("Scale", 1f)
	}
	//endregion
}
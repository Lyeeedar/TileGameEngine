package com.lyeeedar.Renderables

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClass

class Shadow : XmlDataClass()
{
	lateinit var texture: TextureRegion
	lateinit var colour: Colour

	//region generated
	override fun load(xmlData: XmlData)
	{
		texture = AssetManager.loadTextureRegion(xmlData.getChildByName("Texture")!!)
		colour = AssetManager.loadColour(xmlData.getChildByName("Colour")!!)
	}
	//endregion
}
package com.lyeeedar.Renderables.Sprite

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.XmlData

class MaskedTextureData()
{
	constructor(xmlData: XmlData) : this()
	{
		parse(xmlData)
	}

	lateinit var base: TextureRegion
	lateinit var glow: TextureRegion
	lateinit var mask: TextureRegion
	lateinit var layer1: TextureRegion
	lateinit var layer2: TextureRegion
	lateinit var layer3: TextureRegion

	fun parse(xmlData: XmlData)
	{
		base = AssetManager.tryLoadTextureRegion(xmlData.get("Base"))!!
		glow = AssetManager.tryLoadTextureRegion(xmlData.get("Glow"))!!
		mask = AssetManager.tryLoadTextureRegion(xmlData.get("Mask"))!!
		layer1 = AssetManager.tryLoadTextureRegion(xmlData.get("Layer1"))!!
		layer2 = AssetManager.tryLoadTextureRegion(xmlData.get("Layer2"))!!
		layer3 = AssetManager.tryLoadTextureRegion(xmlData.get("Layer3"))!!
	}
}
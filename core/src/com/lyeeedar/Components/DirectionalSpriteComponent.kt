package com.lyeeedar.Components

import com.lyeeedar.Renderables.Sprite.DirectionalSprite
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.XmlData

class DirectionalSpriteComponent : DataComponent()
{
	override val type: ComponentType = ComponentType.DirectionalSprite

	lateinit var directionalSprite: DirectionalSprite

	var currentAnim: String = "idle"
	var lastV: DirectionalSprite.VDir = DirectionalSprite.VDir.DOWN
	var lastH: DirectionalSprite.HDir = DirectionalSprite.HDir.RIGHT

	override fun reset()
	{
		currentAnim = "idle"
		lastV = DirectionalSprite.VDir.DOWN
		lastH = DirectionalSprite.HDir.RIGHT
	}

	override fun initialiseFrom(data: AbstractComponentData)
	{
		val data = data as DirectionalSpriteComponentData
		directionalSprite = data.directionalSprite
	}
}

@DataClass(name = "DirectionalSpriteComponent")
class DirectionalSpriteComponentData : AbstractComponentData()
{
	lateinit var directionalSprite: DirectionalSprite

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		directionalSprite = AssetManager.loadDirectionalSprite(xmlData.getChildByName("DirectionalSprite")!!)
	}
	override val classID: String = "DirectionalSprite"
	//endregion
}
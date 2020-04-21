package com.lyeeedar.Components

import com.lyeeedar.Renderables.Sprite.DirectionalSprite
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.XmlData

inline fun Entity.directionalSprite(): DirectionalSpriteComponent? = this.components[ComponentType.DirectionalSprite] as DirectionalSpriteComponent?
class DirectionalSpriteComponent(data: DirectionalSpriteComponentData) : AbstractComponent<DirectionalSpriteComponentData>(data)
{
	override val type: ComponentType = ComponentType.DirectionalSprite

	var currentAnim: String = "idle"
	var lastV: DirectionalSprite.VDir = DirectionalSprite.VDir.DOWN
	var lastH: DirectionalSprite.HDir = DirectionalSprite.HDir.RIGHT

	override fun reset()
	{
		currentAnim = "idle"
		lastV = DirectionalSprite.VDir.DOWN
		lastH = DirectionalSprite.HDir.RIGHT
	}
}

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
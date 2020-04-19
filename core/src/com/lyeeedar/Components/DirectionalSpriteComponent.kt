package com.lyeeedar.Components

import com.lyeeedar.Renderables.Sprite.DirectionalSprite
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.XmlData

inline fun Entity.directionalSprite(): DirectionalSpriteComponent? = this.components[ComponentType.DirectionalSprite] as DirectionalSpriteComponent?
class DirectionalSpriteComponent() : AbstractComponent()
{
	override val type: ComponentType = ComponentType.DirectionalSprite

	lateinit var directionalSprite: DirectionalSprite

	var currentAnim: String = "idle"
	var lastV: DirectionalSprite.VDir = DirectionalSprite.VDir.DOWN
	var lastH: DirectionalSprite.HDir = DirectionalSprite.HDir.RIGHT

	override fun parse(xml: XmlData, entity: Entity, parentPath: String)
	{
		directionalSprite = AssetManager.loadDirectionalSprite(xml, entity.pos()?.size ?: 1)
	}

	override fun reset()
	{
		lastV = DirectionalSprite.VDir.DOWN
		lastH = DirectionalSprite.HDir.RIGHT
		currentAnim = "idle"
	}
}

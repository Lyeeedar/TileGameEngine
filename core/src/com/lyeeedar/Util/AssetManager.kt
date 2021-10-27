package com.lyeeedar.Util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.Texture.TextureFilter
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.esotericsoftware.spine.*
import com.esotericsoftware.spine.SkeletonData
import com.lyeeedar.BlendMode
import com.lyeeedar.Renderables.*
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.Particle.ParticleEffectDescription
import com.lyeeedar.Renderables.Particle.TextureOverride
import com.lyeeedar.Renderables.Sprite.*
import ktx.collections.gdxArrayOf
import ktx.collections.set
import java.util.*

class AssetManager
{
	companion object
	{
		fun invalidate()
		{
			loadedFonts.clear()
			loadedSounds.clear()
			loadedTextureRegions.clear()
			loadedTextures.clear()
			loadedSkeletons.clear()
			loadedAnimGraphs.clear()

			SpriteWrapper.loaded.clear()

			XmlData.cachedXml.clear()
			XmlData.existingPaths = null

			ParticleEffect.storedMap.clear()

			prepackedAtlas = TextureAtlas(Gdx.files.internal("CompressedData/SpriteAtlas.atlas")!!)

			Localisation.invalidate()
		}

		private val loadedFonts = HashMap<String, BitmapFont>()

		@JvmOverloads fun loadFont(name: String, size: Int, colour: Color = Color.WHITE, borderWidth: Int = 1, borderColour: Color = Color.BLACK, shadow: Boolean = false): BitmapFont?
		{
			val key = name + size + colour.toString() + borderWidth + borderColour.toString()

			if (loadedFonts.containsKey(key))
			{
				return loadedFonts[key]
			}

			val fgenerator = FreeTypeFontGenerator(Gdx.files.internal(name))
			val parameter = FreeTypeFontParameter()
			parameter.size = size
			parameter.borderWidth = borderWidth.toFloat()
			parameter.kerning = true
			parameter.borderColor = borderColour
			parameter.borderStraight = true
			parameter.color = colour

			if (shadow)
			{
				parameter.shadowOffsetX = -1
				parameter.shadowOffsetY = 1
			}

			val font = fgenerator.generateFont(parameter)
			font.data.markupEnabled = true
			fgenerator.dispose() // don't forget to dispose to avoid memory leaks!

			loadedFonts.put(key, font)

			return font
		}

		private val loadedSounds = HashMap<String, Sound?>()

		fun loadSound(path: String): Sound?
		{
			if (loadedSounds.containsKey(path))
			{
				return loadedSounds[path]
			}

			var file = Gdx.files.internal("Sounds/$path.mp3")
			if (!file.exists())
			{
				file = Gdx.files.internal("Sounds/$path.ogg")

				if (!file.exists())
				{
					loadedSounds.put(path, null)
					return null
				}
			}

			val sound = Gdx.audio.newSound(file)

			loadedSounds.put(path, sound)

			return sound
		}

		private val loadedSkeletons = ObjectMap<String, SkeletonData>()
		private val loadedAnimGraphs = ObjectMap<String, AnimationGraph>()

		private var prepackedAtlas = TextureAtlas(Gdx.files.internal("CompressedData/SpriteAtlas.atlas")!!)

		private val loadedTextureRegions = ObjectMap<String, TextureRegion?>()

		fun loadTextureRegion(xml: XmlData): TextureRegion
		{
			return loadTextureRegion(xml.get("File"))
		}

		fun loadTextureRegion(path: String): TextureRegion
		{
			return tryLoadTextureRegion(path) ?: throw RuntimeException("Texture region '${path}' not found")
		}

		@JvmStatic fun tryLoadTextureRegion(path: String): TextureRegion?
		{
			if (loadedTextureRegions.containsKey(path))
			{
				return loadedTextureRegions[path]
			}

			var atlasName = path
			if (atlasName.startsWith("Sprites/")) atlasName = atlasName.replaceFirst("Sprites/".toRegex(), "")
			atlasName = atlasName.replace(".png", "")

			val region = prepackedAtlas.findRegion(atlasName)
			if (region != null)
			{
				val textureRegion = TextureRegion(region)
				loadedTextureRegions.put(path, textureRegion)
				return textureRegion
			}
			else
			{
				loadedTextureRegions.put(path, null)
				return null
			}
		}

		private val loadedTextures = HashMap<String, Texture?>()

		fun loadTexture(path: String, filter: TextureFilter = TextureFilter.Linear, wrapping: Texture.TextureWrap = Texture.TextureWrap.ClampToEdge): Texture?
		{
			var rawpath = path
			if (!rawpath.endsWith(".png")) rawpath += ".png"

			val path = "CompressedData/" + rawpath.hashCode() + ".png"

			if (loadedTextures.containsKey(path))
			{
				return loadedTextures[path]
			}

			val file = Gdx.files.internal(path)
			if (!file.exists())
			{
				loadedTextures.put(path, null)
				return null
			}

			val region = Texture(path)
			region.setFilter(filter, filter)
			region.setWrap(wrapping, wrapping)
			loadedTextures.put(path, region)

			return region
		}

		fun loadParticleEffect(name: String, colour: Colour = Colour.WHITE, flipX: Boolean = false, flipY: Boolean = false, scale: Float = 1f, useFacing: Boolean = true, timeMultiplier: Float = 1f, killOnAnimComplete: Boolean = false): ParticleEffectDescription
		{
			val effect = ParticleEffectDescription(name)
			effect.colour.set(colour)
			effect.flipX = flipX
			effect.flipY = flipY
			effect.scale = scale
			effect.useFacing = useFacing
			effect.timeMultiplier = timeMultiplier
			effect.killOnAnimComplete = killOnAnimComplete
			return effect
		}

		fun loadParticleEffect(xml:XmlData): ParticleEffectDescription
		{
			val effectXml: XmlData
			val overridesEl: XmlData?
			if (xml.getChildByName("Name") == null)
			{
				// its a template
				effectXml = xml.getChildByName("Base")!!
				overridesEl = xml.getChildByName("Overrides")
			}
			else
			{
				effectXml = xml
				overridesEl = null
			}

			val effect = ParticleEffectDescription(effectXml.get("Name"))

			val colourElement = effectXml.getChildByName("Colour")
			var colour = Colour(1f, 1f, 1f, 1f)
			if (colourElement != null)
			{
				colour = loadColour(colourElement)
			}

			effect.colour.set(colour)

			effect.flipX = effectXml.getBoolean("FlipX", false)
			effect.flipY = effectXml.getBoolean("FlipY", false)
			effect.scale = effectXml.getFloat("Scale", 1f)
			effect.useFacing = effectXml.getBoolean("UseFacing", true)
			effect.timeMultiplier = effectXml.getFloat("TimeMultiplier", 1f)
			effect.killOnAnimComplete = effectXml.getBoolean("KillOnAnimComplete", false)

			if (overridesEl != null)
			{
				for (overrideEl in overridesEl.children)
				{
					val texName = overrideEl.get("Name")
					val overrideName = overrideEl.getChildByName("Texture")!!.get("File")
					val blendModeStr = overrideEl.get("BlendMode", "Current")!!
					val blendMode = if (blendModeStr != "Current") BlendMode.valueOf(blendModeStr.toUpperCase(Locale.ENGLISH)) else null

					effect.textureOverrides.add(TextureOverride(texName, overrideName, blendMode))
				}
			}

			return effect
		}

		fun tryLoadParticleEffect(xml: XmlData?): ParticleEffectDescription?
		{
			if (xml == null) return null
			return loadParticleEffect(xml)
		}

		fun tryLoadSpriteWrapper(path: String?): SpriteWrapper?
		{
			if (path == null) return null
			val xml = getXml(path)
			return SpriteWrapper.load(xml)
		}

		fun tryLoadSpriteWrapper(xml: XmlData?): SpriteWrapper?
		{
			if (xml == null) return null
			return SpriteWrapper.load(xml)
		}

		fun loadSpriteWrapper(xml: XmlData): SpriteWrapper
		{
			return SpriteWrapper.load(xml)
		}

		fun loadGrayscaleSprite(name: String, colour: Colour = Colour(1f, 1f, 1f, 1f), drawActualSize: Boolean = false): Sprite
		{
			return loadSprite(name+"_grayscale", 0.5f, colour, drawActualSize)
		}

		fun loadSprite(name: String, drawActualSize: Boolean): Sprite
		{
			return loadSprite(name, 0.5f, Colour(1f, 1f, 1f, 1f), drawActualSize)
		}

		fun loadSprite(name: String, updateTime: Float, reverse: Boolean): Sprite
		{
			return loadSprite(name, updateTime, Colour(1f, 1f, 1f, 1f), false, reverse)
		}

		@JvmOverloads fun loadSprite(name: String, updateTime: Float = 0.5f, colour: Colour = Colour(1f, 1f, 1f, 1f), drawActualSize: Boolean = false, reverse: Boolean = false, light: Light? = null): Sprite
		{
			var updateTime = updateTime
			val textures = Array<TextureRegion>(false, 1, TextureRegion::class.java)

			// Try sprite without indexes
			val tex = tryLoadTextureRegion("Sprites/$name.png")
			if (tex != null)
			{
				textures.add(tex)
			}

			// Try 0 indexed sprite
			if (textures.size == 0)
			{
				var i = 0
				while (true)
				{
					val tex = tryLoadTextureRegion("Sprites/" + name + "_" + i + ".png")

					if (tex == null)
					{
						break
					}
					else
					{
						textures.add(tex)
					}

					i++
				}
			}

			// Try 1 indexed sprite
			if (textures.size == 0)
			{
				var i = 1
				while (true)
				{
					val tex = tryLoadTextureRegion("Sprites/" + name + "_" + i + ".png")

					if (tex == null)
					{
						break
					} else
					{
						textures.add(tex)
					}

					i++
				}
			}

			if (textures.size == 0)
			{
				throw RuntimeException("Cant find any textures for $name!")
			}

			if (reverse)
			{
				textures.reverse()
			}

			if (updateTime <= 0)
			{
				updateTime = 0.5f
			}

			val sprite = Sprite(name, updateTime, textures, colour, drawActualSize)
			sprite.light = light

			return sprite
		}

		fun tryLoadSpriteWithResources(xml:XmlData, resources: ObjectMap<String,XmlData>): Sprite
		{
			if (xml.childCount == 0) return loadSprite(resources[xml.text])
			else return loadSprite(xml)
		}

		fun tryLoadSprite(xml:XmlData?): Sprite?
		{
			if (xml == null) return null
			else if (xml.childCount == 0) return null
			else return loadSprite(xml)
		}

		fun loadSprite(xml:XmlData, texture: TextureRegion? = null): Sprite
		{
			val data = SpriteData()
			data.load(xml)

			val sprite = if (texture != null)
				Sprite(
					data.name,
					data.updateRate,
					gdxArrayOf(texture),
					data.colour ?: Colour.WHITE.copy(),
					data.drawActualSize)
				else
				loadSprite(
					data.name,
					data.updateRate,
					data.colour ?: Colour.WHITE.copy(),
					data.drawActualSize)

			sprite.repeatDelay = data.repeatDelay
			sprite.frameBlend = data.blend

			sprite.disableHDR = data.disableHDR

			if (data.randomStart)
			{
				sprite.texIndex = Random.random(Random.sharedRandom, sprite.textures.size)
				sprite.animationAccumulator = Random.random(Random.sharedRandom, sprite.animationDelay)
			}

			sprite.light = data.light
			sprite.shadow = data.shadow

			return sprite
		}

		fun loadColour(stringCol: String, colour: Colour = Colour()): Colour
		{
			val cols = stringCol.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
			colour.r = java.lang.Float.parseFloat(cols[0]) / 255.0f
			colour.g = java.lang.Float.parseFloat(cols[1]) / 255.0f
			colour.b = java.lang.Float.parseFloat(cols[2]) / 255.0f
			colour.a = if (cols.size > 3) cols[3].toFloat() / 255.0f else 1f

			return colour
		}

		fun loadColour(xml:XmlData): Colour
		{
			return loadColour(xml.text)
		}

		fun tryLoadColour(xml: XmlData?): Colour?
		{
			if (xml == null) return null
			return loadColour(xml)
		}

		fun loadTilingSprite(xml:XmlData): TilingSprite
		{
			return TilingSprite.load(xml)
		}

		fun tryLoadTilingSprite(xml: XmlData?): TilingSprite?
		{
			if (xml == null) return null
			return loadTilingSprite(xml)
		}

		fun loadLayeredSprite(xml: XmlData): Sprite
		{
			val renderedLayeredSprite = RenderedLayeredSprite()
			renderedLayeredSprite.load(xml)

			val mergedName = renderedLayeredSprite.toString()
			val tex = tryLoadTextureRegion("Sprites/$mergedName.png")
					  ?: throw RuntimeException("Cant find any textures for layered sprite $mergedName!")

			val sprite = Sprite(tex)
			sprite.drawActualSize = renderedLayeredSprite.layers.any { it.drawActualSize }

			return sprite
		}

		fun tryLoadLayeredSprite(xml: XmlData?): Sprite?
		{
			if (xml == null) return null
			return loadLayeredSprite(xml)
		}

		fun loadDirectionalSprite(xml:XmlData, size: Int = 1): DirectionalSprite
		{
			val directionalSprite = DirectionalSprite()

			val anims = xml.getChildByName("Animations")!!
			for (i in 0 until anims.childCount)
			{
				val el = anims.getChild(i)
				val name = el.get("Name").lowercase(Locale.ENGLISH)
				val up = AssetManager.loadSprite(el.getChildByName("Up")!!)
				val down = AssetManager.loadSprite(el.getChildByName("Down")!!)

				up.size[0] = size
				up.size[1] = size

				down.size[0] = size
				down.size[1] = size

				directionalSprite.addAnim(name, up, down)
			}

			return directionalSprite
		}

		fun tryLoadDirectionalSprite(xml: XmlData?): DirectionalSprite?
		{
			if (xml == null) return null
			return loadDirectionalSprite(xml)
		}

		fun loadRenderable(xml:XmlData): Renderable
		{
			val type = xml.getAttribute("meta:RefKey", null)?.toUpperCase(Locale.ENGLISH) ?: xml.name.toUpperCase(Locale.ENGLISH)

			return when(type)
			{
				"SPRITE" -> AssetManager.loadSprite(xml)
				"PARTICLEEFFECT", "PARTICLE", "PARTICLEEFFECTTEMPLATE" -> AssetManager.loadParticleEffect(xml).getParticleEffect()
				"TILINGSPRITE" -> AssetManager.loadTilingSprite(xml)
				"SKELETON" -> AssetManager.loadSkeleton(xml)
				else -> throw Exception("Unknown renderable type '$type'!")
			};
		}

		fun tryLoadRenderable(xml: XmlData?): Renderable?
		{
			if (xml == null) return null
			return loadRenderable(xml)
		}

		fun loadSkeleton(xml: XmlData): SkeletonRenderable
		{
			val data = com.lyeeedar.Renderables.SkeletonData()
			data.load(xml)

			var animationGraph = loadedAnimGraphs.get(data.animGraph)
			if (animationGraph == null)
			{
				animationGraph = AnimationGraph()
				animationGraph.load(getXml(data.animGraph))
				loadedAnimGraphs[data.animGraph] = animationGraph
			}

			val key = data.path + data.scale
			var skeletonData = loadedSkeletons.get(key)
			if (skeletonData == null)
			{
				var filepath = data.path.replace("\\", "/")
				filepath = "CompressedData/" + filepath.hashCode()

				val atlas = TextureAtlas(Gdx.files.internal("${filepath}.atlas"))
				val json = SkeletonJson(atlas)
				json.scale = data.scale * (48f / 256f)
				skeletonData = json.readSkeletonData(Gdx.files.internal("${filepath}.json"))

				loadedSkeletons[key] = skeletonData
			}

			val skeleton = Skeleton(skeletonData)
			val stateData = AnimationStateData(skeletonData)
			stateData.defaultMix = 0.1f
			val state = AnimationState(stateData)
			skeleton.setSkin(data.skin)

			val renderable = SkeletonRenderable(skeleton, state, animationGraph)
			renderable.colour = data.colour ?: Colour.WHITE
			return renderable
		}

		fun tryLoadSkeleton(xml: XmlData?): SkeletonRenderable?
		{
			if (xml == null) return null
			return loadSkeleton(xml)
		}
	}
}
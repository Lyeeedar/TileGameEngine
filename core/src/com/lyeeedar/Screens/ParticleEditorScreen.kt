package com.lyeeedar.Screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Renderables.Animation.MoveAnimation
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.Particle.ParticleEffectDescription
import com.lyeeedar.Renderables.Renderable
import com.lyeeedar.Renderables.Renderer.SortedRenderer
import com.lyeeedar.SpaceSlot
import com.lyeeedar.UI.addClickListener
import com.lyeeedar.Util.*
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.toCharGrid
import java.io.File
import javax.swing.JColorChooser
import ktx.collections.set
import ktx.collections.toGdxArray

/**
 * Created by Philip on 14-Aug-16.
 */

class ParticleEditorScreen : AbstractScreen()
{
	enum class BackgroundType
	{
		MAP,
		WHITE,
		BLACK,
		GRASS,
		DIRT,
		WOOD,
		WATER
	}

	val white = AssetManager.loadSprite("white")
	val grass = AssetManager.loadSprite("Oryx/uf_split/uf_terrain/ground_grass_1")
	val dirt = AssetManager.loadSprite("Oryx/uf_split/uf_terrain/ground_dirt_brown_1")
	val wood = AssetManager.loadSprite("Oryx/uf_split/uf_terrain/wall_stone_10")
	val water = AssetManager.loadSprite("Oryx/uf_split/uf_terrain/water_blue_1")

	val checkerCol = Colour(0f, 0f, 0f, 0.2f).lockColour()

	var backgroundType = BackgroundType.MAP
	lateinit var particle: ParticleEffect
	val batch = SpriteBatch()
	lateinit var background: Array2D<ParticleEditorSymbol>
	lateinit var collision: Array2D<Boolean>
	var tileSize = 32f
	lateinit var spriteRender: SortedRenderer
	val shape = ShapeRenderer()
	var colour: java.awt.Color = java.awt.Color.WHITE
	val crossedTiles = ObjectSet<Point>()
	val particlePos = Point()
	lateinit var debugButton: CheckBox
	lateinit var alignUpButton: CheckBox
	lateinit var flyRandomlyButton: CheckBox
	lateinit var lockToTileButton: CheckBox
	var deltaMultiplier = 1f
	var size = 1

	override fun show()
	{
		if ( !created )
		{
			baseCreate()
			created = true
		}

		Gdx.input.inputProcessor = inputMultiplexer
	}

	override fun create()
	{
		drawFPS = false

		val backgroundTypeBox = SelectBox<BackgroundType>(Statics.skin)
		backgroundTypeBox.setItems(BackgroundType.values().toGdxArray())
		backgroundTypeBox.selected = backgroundType
		backgroundTypeBox.addListener(object : ChangeListener()
		                              {
			                              override fun changed(event: ChangeEvent?, actor: Actor?)
			                              {
				                              backgroundType = backgroundTypeBox.selected
			                              }
		                              })

		val playbackSpeedBox = SelectBox<Float>(Statics.skin)
		playbackSpeedBox.setItems(0.01f, 0.05f, 0.1f, 0.25f, 0.5f, 0.75f, 1f, 1.5f, 2f, 3f, 4f, 5f)
		playbackSpeedBox.selected = 1f

		playbackSpeedBox.addListener(object : ChangeListener()
		{
			override fun changed(event: ChangeEvent?, actor: Actor?)
			{
				deltaMultiplier = playbackSpeedBox.selected
			}

		})

		val colourButton = TextButton("Colour", Statics.skin)
		colourButton.addClickListener {
			colour = JColorChooser.showDialog(null, "Particle Colour", colour) ?: java.awt.Color.WHITE
			particle.colour.set(colour.red / 255f, colour.green / 255f, colour.blue / 255f, colour.alpha / 255f)
			colourButton.color = particle.colour.color()
		}

		debugButton = CheckBox("", Statics.skin)
		alignUpButton = CheckBox("", Statics.skin)
		flyRandomlyButton = CheckBox("", Statics.skin)
		lockToTileButton = CheckBox("", Statics.skin)

		val sizeBox = SelectBox<Int>(Statics.skin)
		sizeBox.setItems(1, 2, 3, 4, 5)
		sizeBox.selected = 1

		sizeBox.addListener(object : ChangeListener()
									 {
										 override fun changed(event: ChangeEvent?, actor: Actor?)
										 {
											 size = sizeBox.selected
										 }

									 })

		val options = Table()
		options.background = TextureRegionDrawable(AssetManager.tryLoadTextureRegion("white")).tint(Color(0.4f, 0.4f, 0.4f, 0.4f))
		options.defaults().pad(5f).growX()

		options.add(Label("Playback Speed", Statics.skin))
		options.add(playbackSpeedBox)
		options.row()

		options.add(Label("Colour", Statics.skin))
		options.add(colourButton)
		options.row()

		options.add(Label("Debug", Statics.skin))
		options.add(debugButton)
		options.row()

		options.add(Label("Align Up", Statics.skin))
		options.add(alignUpButton)
		options.row()

		options.add(Label("Fly Randomly", Statics.skin))
		options.add(flyRandomlyButton)
		options.row()

		options.add(Label("Lock To Tile", Statics.skin))
		options.add(lockToTileButton)
		options.row()

		options.add(Label("Size", Statics.skin))
		options.add(sizeBox)
		options.row()

		options.add(Label("Background", Statics.skin))
		options.add(backgroundTypeBox)
		options.row()

		val optionsToggle = TextButton("Options", Statics.skin)
		optionsToggle.addClickListener {
			options.isVisible = !options.isVisible
		}

		options.isVisible = false

		mainTable.add(optionsToggle).expandX().right().pad(5f)
		mainTable.row()
		mainTable.add(options).expandX().right().pad(5f)
		mainTable.row()

		particle = ParticleEffect(ParticleEffectDescription(""))

		loadLevel()

		particlePos.set(background.width / 2, background.height / 2)

		val clickTable = Table()
		clickTable.touchable = Touchable.enabled

		clickTable.addListener(object : InputListener()
							   {
								   override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean
								   {
									   val xp = x + ((spriteRender.width * tileSize) / 2f) - (clickTable.width / 2f)

									   val sx = (xp / tileSize).toInt()
									   val sy = spriteRender.height.toInt() - ((spriteRender.height.toInt()-1) - (y / tileSize).toInt()) - 1

									   val p1 = Vector2(particle.position)
									   val p2 = Vector2(sx.toFloat(), sy.toFloat())

									   particlePos.set(sx, sy)

									   particle.animation = null
									   if (lockToTileButton.isChecked)
									   {
										   particle.position.set(p2)
										   particle.rotation = 0f
									   }
									   else
									   {
										   val dist = p1.dst(p2)

										   particle.animation = MoveAnimation.obtain().set(dist, arrayOf(p1, p2), Interpolation.linear)
										   particle.rotation = getRotation(p1, p2)

										   Point.freeAll(crossedTiles)
										   crossedTiles.clear()
										   particle.collisionFun = fun(x: Int, y: Int)
										   {
											   crossedTiles.add(Point.obtain().set(x, y))
										   }
									   }

									   particle.start()

									   return true
								   }
							   })

		mainTable.add(clickTable).grow()
	}

	fun loadLevel()
	{
		val particleTestLevel = ParticleTestLevel()
		particleTestLevel.load(getXml("Particles/ParticleTestLevel"))

		val symbolMap = ObjectMap<Char, ParticleEditorSymbol>()

		for (symbol in particleTestLevel.symbols)
		{
			symbolMap[symbol.char] = symbol
		}

		val width = particleTestLevel.grid.width
		val height = particleTestLevel.grid.height
		background = Array2D(width, height) { x, y -> symbolMap.get(particleTestLevel.grid[x, y]).copy() }
		collision = Array2D(width, height) { x, y -> background[x, y].isWall }

		val tilex = Statics.resolution.x.toFloat() / width.toFloat()
		tileSize = tilex

		spriteRender = SortedRenderer(tileSize, width.toFloat(), height.toFloat(), 2, true)
	}

	fun flyRandomly()
	{
		if (particle.animation == null && flyRandomlyButton.isChecked)
		{
			val randomPos = Point(Random.random(Random.sharedRandom, background.width), Random.random(Random.sharedRandom, background.height))

			val p1 = Vector2(particle.position)
			val p2 = randomPos.toVec()

			particlePos.set(randomPos)

			val dist = p1.dst(p2) * 0.6f

			particle.animation = null
			particle.animation = MoveAnimation.obtain().set(dist, arrayOf(p1, p2), Interpolation.linear)
			particle.rotation = getRotation(p1, p2)

			if (debugButton.isChecked)
			{
				Point.freeAll(crossedTiles)
				crossedTiles.clear()
				particle.collisionFun = fun(x: Int, y: Int)
				{
					crossedTiles.add(Point.obtain().set(x, y))
				}
			}

			particle.start()
		}
	}

	var lastDataChange = 0L
	var lastModified = 0L
	fun tryLoadParticle()
	{
		try
		{
			val modified = getLastModified(File("."))
			if (modified != lastDataChange)
			{
				lastDataChange = modified

				AssetManager.invalidate()

				lastModified = 0L
			}
		} catch (ex: Exception) {}

		try
		{
			val tempParticleFile = File("../caches/editor/particle.xml")
			if (tempParticleFile.exists())
			{
				val modified = tempParticleFile.lastModified()

				if (lastModified != modified)
				{
					lastModified = modified

					val rawxml = getRawXml(tempParticleFile.absolutePath)
					val xmlData = XmlData.loadFromElement(rawxml)

					val nparticle = ParticleEffect.load(xmlData, ParticleEffectDescription(tempParticleFile.absolutePath))
					nparticle.killOnAnimComplete = false
					nparticle.setPosition(particle.position.x, particle.position.y)
					nparticle.rotation = particle.rotation
					nparticle.colour.set(colour.red / 255f, colour.green / 255f, colour.blue / 255f, colour.alpha / 255f)
					particle = nparticle
					particle.start()
				}
			}
		} catch (ex: Exception) {}
	}

	fun getLastModified(directory: File): Long
	{
		val files = directory.listFiles()!!
		if (files.isEmpty()) return directory.lastModified()
		return files.maxOf { it.lastModified() }
	}

	var stoppedTimer = 0f
	val tempPoint = Point()
	override fun doRender(delta: Float)
	{
		tryLoadParticle()
		flyRandomly()

		if (particle.completed && particle.complete())
		{
			stoppedTimer += delta

			if (stoppedTimer > 1f)
			{
				particle.start()
				stoppedTimer = 0f
			}
		}

		particle.size[0] = size
		particle.size[1] = size

		if (alignUpButton.isChecked)
		{
			particle.rotation = 0f
		}

		batch.projectionMatrix = stage.camera.combined

		Statics.lightCollisionGrid = collision

		spriteRender.begin(delta * deltaMultiplier, 0f, 0f, Colour.WHITE)

		for (x in 0..background.xSize-1)
		{
			for (y in 0..background.ySize-1)
			{
				tempPoint.set(x, y)
				val col = if (crossedTiles.contains(tempPoint)) Colour.GOLD else Colour.WHITE

				when (backgroundType)
				{
					BackgroundType.MAP -> {
						val symbol = background[x, y]
						var i = 0
						for (renderable in symbol.sprites)
						{
							spriteRender.queue(renderable, x.toFloat(), y.toFloat(), 0, i++, col)
						}
					}
					BackgroundType.WHITE -> {
						spriteRender.queueSprite(white, x.toFloat(), y.toFloat(), 0, 0, col)
					}
					BackgroundType.BLACK -> {
						val col = if (crossedTiles.contains(tempPoint)) Colour.GOLD else Colour.BLACK
						spriteRender.queueSprite(white, x.toFloat(), y.toFloat(), 0, 0, col)
					}
					BackgroundType.GRASS -> {
						spriteRender.queueSprite(grass, x.toFloat(), y.toFloat(), 0, 0, col)
					}
					BackgroundType.DIRT -> {
						spriteRender.queueSprite(dirt, x.toFloat(), y.toFloat(), 0, 0, col)
					}
					BackgroundType.WATER -> {
						spriteRender.queueSprite(water, x.toFloat(), y.toFloat(), 0, 0, col)
					}
					BackgroundType.WOOD -> {
						spriteRender.queueSprite(wood, x.toFloat(), y.toFloat(), 0, 0, col)
					}
				}

				if ((x + y).rem(2) == 0)
				{
					spriteRender.queueSprite(white, x.toFloat(), y.toFloat(), 0, 1, checkerCol)
				}
			}
		}
		spriteRender.queueParticle(particle, particlePos.x.toFloat(), particlePos.y.toFloat(), 1, 0)

		batch.color = Color.WHITE
		spriteRender.end(batch)

		if (debugButton.isChecked)
		{
			shape.projectionMatrix = stage.camera.combined
			shape.setAutoShapeType(true)
			shape.begin()

			particle.debug(shape, 0f, 0f, tileSize, true, true, true)

			shape.end()
		}
	}
}

class ParticleEditorSymbol : XmlDataClass()
{
	var char: Char = ' '
	val sprites: Array<Renderable> = Array()
	var isWall: Boolean = false

	fun copy(): ParticleEditorSymbol
	{
		val symbol = ParticleEditorSymbol()
		symbol.char = char
		for (sprite in sprites)
		{
			symbol.sprites.add(sprite.copy())
		}
		symbol.isWall = isWall

		return symbol
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		char = xmlData.get("Char", " ")!![0]
		val spritesEl = xmlData.getChildByName("Sprites")
		if (spritesEl != null)
		{
			for (el in spritesEl.children)
			{
				val objsprites: Renderable
				objsprites = AssetManager.tryLoadRenderable(el)!!
				sprites.add(objsprites)
			}
		}
		isWall = xmlData.getBoolean("IsWall", false)
	}
	//endregion
}

@DataFile
class ParticleTestLevel : XmlDataClass()
{
	val symbols: Array<ParticleEditorSymbol> = Array()

	@DataAsciiGrid
	lateinit var grid: Array2D<Char>

	//region generated
	override fun load(xmlData: XmlData)
	{
		val symbolsEl = xmlData.getChildByName("Symbols")
		if (symbolsEl != null)
		{
			for (el in symbolsEl.children)
			{
				val objsymbols: ParticleEditorSymbol
				val objsymbolsEl = el
				objsymbols = ParticleEditorSymbol()
				objsymbols.load(objsymbolsEl)
				symbols.add(objsymbols)
			}
		}
		val gridEl = xmlData.getChildByName("Grid")
		if (gridEl != null) grid = gridEl.toCharGrid()
		else grid = Array2D<Char>(0,0){_,_->' '}
	}
	//endregion
}
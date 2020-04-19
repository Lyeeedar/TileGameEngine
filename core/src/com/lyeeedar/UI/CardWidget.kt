package com.lyeeedar.UI

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Direction
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.*
import ktx.actors.alpha
import ktx.actors.then
import ktx.collections.gdxArrayOf
import ktx.collections.toGdxArray
import kotlin.math.max

data class Pick(val string: String, var pickFun: (card: CardWidget) -> Unit)

class CardWidget(val frontTable: Table, val frontDetailTable: Table, val backImage: TextureRegion, var data: Any? = null, val colour: Colour = Colour.WHITE, val border: Colour = Colour.TRANSPARENT) : WidgetGroup()
{
	val referenceWidth = Statics.resolution.x - 100f
	val referenceHeight = Statics.resolution.y - 200f

	val contentTable = Table()
	private val backTable: Table

	var canZoom = true
	var canPickFaceDown = false

	val pickFuns = Array<Pick>()
	var collapseFun: (() -> Unit)? = null
	var flipFun: (() -> Unit)? = null

	var flipEffect: ParticleEffectActor? = null
	var flipDelay = 0f

	var faceup = false
	private var flipping = false
	private var fullscreen = false
	var isPicked = false

	var clickable = true

	private val frontBackground = AssetManager.loadTextureRegion("GUI/CardBase")
	private val frontBackgroundLarge = AssetManager.loadTextureRegion("GUI/CardBaseLarge")
	private val cardBack = AssetManager.loadTextureRegion("GUI/CardBack")

	init
	{
		if (frontTable == frontDetailTable)
		{
			throw RuntimeException("Front table cannot equal front detail table!")
		}

		addActor(contentTable)
		contentTable.background = TextureRegionDrawable(cardBack).tint(colour.color())

		backTable = Table()
		backTable.add(SpriteWidget(Sprite(backImage), 160f, 160f)).expand().center()

		contentTable.add(backTable).pad(10f).grow()

		contentTable.isTransform = true
		contentTable.originX = referenceWidth / 2
		contentTable.originY = referenceHeight / 2

		contentTable.setSize(referenceWidth, referenceHeight)

		addClickListener {
			if (clickable)
			{
				if (canZoom)
				{
					if (!fullscreen && faceup)
					{
						focus()
					}
					else if (!faceup)
					{
						flip(true)
					}
				}
				else
				{
					if (canPickFaceDown || faceup)
					{
						if (pickFuns.size > 0)
						{
							pickFuns[0].pickFun(this)
						}
					}
					else
					{
						flip(true)
					}
				}
			}
		}

		//debug()
	}

	fun addPick(string: String, pickFun: (card: CardWidget) -> Unit): CardWidget
	{
		pickFuns.add(Pick(string, pickFun))
		return this
	}

	private data class Bounds(val x: Float, val y: Float, val width: Float, val height: Float)
	private fun getTableBounds(): Bounds
	{
		val actualMidX = contentTable.x + contentTable.width / 2f
		val drawX = actualMidX - (contentTable.width * contentTable.scaleX) / 2f

		val actualMidY = contentTable.y + contentTable.height / 2f
		val drawY = actualMidY - (contentTable.height * contentTable.scaleY) / 2f

		return Bounds(x + drawX, y + drawY, contentTable.width * contentTable.scaleX, contentTable.height * contentTable.scaleY)
	}

	override fun hit(x: Float, y: Float, touchable: Boolean): Actor?
	{
		val (tablex, tabley, width, height) = getTableBounds()
		val minx = tablex - this.x
		val miny = tabley - this.y

		if (touchable && this.touchable != Touchable.enabled) return null
		return if (x >= minx && x < minx+width && y >= miny && y < miny+height) this else null
	}

	override fun act(delta: Float)
	{
		super.act(delta)

		if (!fullscreen)
		{
			contentTable.act(delta)
		}
	}

	override fun setBounds(x: Float, y: Float, width: Float, height: Float)
	{
		super.setBounds(x, y, width, height)

		val scaleX = width / referenceWidth
		val scaleY = height / referenceHeight
		val min = min(scaleX, scaleY)

		contentTable.setScale(min)

		val middleX = width / 2f
		val middleY = height / 2f

		contentTable.setPosition(middleX - contentTable.width / 2f, middleY - contentTable.height / 2f)
	}

	fun getWidthFromHeight(height: Float): Float
	{
		val scaleY = height / referenceHeight
		return referenceWidth * scaleY
	}

	override fun positionChanged()
	{
		super.positionChanged()
		setBounds(x, y, width, height)
	}

	override fun rotationChanged()
	{
		super.rotationChanged()
		contentTable.rotation = rotation
	}

	override fun sizeChanged()
	{
		super.sizeChanged()
		setBounds(x, y, width, height)
	}

	override fun setPosition(x: Float, y: Float)
	{
		super.setPosition(x, y)

		setBounds(x, y, width, height)
	}

	override fun drawDebug(shapes: ShapeRenderer)
	{
		super.drawDebug(shapes)

		val bounds = getTableBounds()

		shapes.color = Color.OLIVE
		shapes.set(ShapeRenderer.ShapeType.Line)
		shapes.rect(x, y, width, height)

		shapes.set(ShapeRenderer.ShapeType.Line)
		shapes.color = Color.MAGENTA
		shapes.rect(bounds.x, bounds.y, bounds.width, bounds.height)
	}

	override fun draw(batch: Batch?, parentAlpha: Float)
	{
		super.draw(batch, parentAlpha)

		if (!fullscreen)
		{
			if (!animating) setBounds(x, y, width, height)
			contentTable.draw(batch, parentAlpha * alpha)
		}
	}

	override fun drawChildren(batch: Batch?, parentAlpha: Float)
	{

	}

	fun setFacing(faceup: Boolean, animate: Boolean = true)
	{
		if (faceup != this.faceup)
		{
			flip(animate)
		}
	}

	private var animating = false
	fun flip(animate: Boolean = true)
	{
		if (flipping) return
		flipping = true

		if (animating) return

		val nextTable = if (faceup) backTable else frontTable
		val flipFun = fun () {
			this.flipFun?.invoke()

			contentTable.clearChildren()
			contentTable.add(nextTable).pad(10f).grow()
			flipping = false

			if (faceup)
			{
				contentTable.background = TextureRegionDrawable(frontBackgroundLarge).tint(colour.color())
			}
			else
			{
				contentTable.background = TextureRegionDrawable(cardBack).tint(colour.color())
			}
		}

		faceup = !faceup
		if (animate)
		{
			if (flipEffect != null)
			{
				flipEffect!!.setSize(width, height)
				flipEffect!!.setPosition(x, y)
				(stage ?: Statics.stage).addActor(flipEffect)
				toFront()
			}

			val scale = contentTable.scaleX
			val duration = 0.2f
			animating = true
			val sequence = delay(flipDelay) then scaleTo(0f, scale, duration) then lambda { flipFun() } then scaleTo(scale, scale, duration) then lambda{ animating = false }
			contentTable.addAction(sequence)
		}
		else
		{
			flipFun()
		}
	}

	fun focus()
	{
		if (fullscreen) { return }
		fullscreen = true

		val stage = stage ?: Statics.stage

		val pickButtonTable = Table()
		val closeButton = Button(Statics.skin, "closecard")
		closeButton.name = "Close"
		closeButton.setSize(24f, 24f)

		val table = RemoveAwareTable { pickButtonTable.remove(); closeButton.remove() }
		val background = Table()

		val currentScale = contentTable.scaleX
		val point = contentTable.localToStageCoordinates(Vector2(0f, 0f))

		val zoomTable = Table()
		zoomTable.isTransform = true
		zoomTable.originX = referenceWidth / 2
		zoomTable.originY = referenceHeight / 2
		zoomTable.background = TextureRegionDrawable(frontBackgroundLarge).tint(colour.color())
		zoomTable.setPosition(contentTable.x, contentTable.y)
		zoomTable.setSize(referenceWidth, referenceHeight)
		zoomTable.setScale(contentTable.scaleX, contentTable.scaleY)

		// Setup anim
		val speed = 0.1f
		val destX = (stage.width - referenceWidth) / 2f
		val destY = (stage.height - referenceHeight) / 2f

		val collapseSequence = lambda {
			closeButton.remove()
			pickButtonTable.remove()

			zoomTable.clear()

			val detailTable = Table()
			detailTable.add(frontDetailTable).grow()

			val stack = Stack()
			stack.add(detailTable)
			stack.add(frontTable)
			zoomTable.add(stack).pad(10f).grow()

			val hideSequence = alpha(1f) then fadeOut(speed)
			val showSequence = alpha(0f) then fadeIn(speed)

			detailTable.addAction(hideSequence)
			frontTable.addAction(showSequence)
		} then parallel(scaleTo(currentScale, currentScale, speed), moveTo(point.x, point.y, speed)) then lambda {
			fullscreen = false

			background.remove()
			table.remove()
			table.clear()

			zoomTable.clear()
			contentTable.clear()
			contentTable.add(frontTable).pad(10f).grow()

			collapseFun?.invoke()
		}

		val expandSequence = lambda {
			contentTable.clear()

			frontDetailTable.alpha = 1f
			val detailTable = Table()
			detailTable.add(frontDetailTable).grow()

			val stack = Stack()
			stack.add(detailTable)
			stack.add(frontTable)
			zoomTable.add(stack).pad(10f).grow()

			val hideSequence = alpha(1f) then fadeOut(speed)
			val showSequence = alpha(0f) then fadeIn(speed)

			frontTable.addAction(hideSequence)
			detailTable.addAction(showSequence)
		} then parallel(scaleTo(1f, 1f, speed), moveTo(destX, destY, speed)) then lambda {
			zoomTable.clear()
			zoomTable.add(frontDetailTable).pad(10f).grow()

			stage.addActor(closeButton)
			closeButton.setPosition(destX + zoomTable.width + 5f, destY + zoomTable.height + 5f)

			stage.addActor(pickButtonTable)
			pickButtonTable.setPosition(Statics.screenSize.x * 0.5f - pickButtonTable.width * 0.5f, destY - 40f)
		}

		closeButton.addClickListener {
			table.addAction(collapseSequence)
			background.addAction(fadeOut(speed))
		}

		var pickButtonWidth = 0f
		for (pick in pickFuns)
		{
			val pickButton = TextButton(pick.string, Statics.skin, "defaultcard")
			pickButtonWidth = max(pickButtonWidth, pickButton.prefWidth)
		}

		for (pick in pickFuns)
		{
			val pickButton = TextButton(pick.string, Statics.skin, "defaultcard")
			pickButton.name = pick.string
			pickButton.addClickListener {
				pick.pickFun(this)
				table.addAction(collapseSequence)
				background.addAction(fadeOut(speed))
			}

			pickButtonTable.add(pickButton).width(pickButtonWidth).pad(10f)
		}

		// Create holder
		table.touchable = Touchable.enabled
		table.isTransform = true

		table.add(zoomTable).grow()
		zoomTable.setScale(1f)

		table.setSize(referenceWidth, referenceHeight)
		table.setPosition(point.x, point.y)
		table.setScale(currentScale)
		table.addAction(expandSequence)
		table.addClickListener {

		}

		// Background
		background.touchable = Touchable.enabled
		background.setFillParent(true)
		background.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(0f, 0f, 0f, 0.8f))
		background.addClickListener {

			if (fullscreen)
			{
				table.addAction(collapseSequence)
				background.addAction(fadeOut(speed))
			}
		}
		background.alpha = 0f
		background.addAction(fadeIn(speed))

		stage.addActor(background)
		stage.addActor(table)
	}

	enum class DissolveType
	{
		BURN,
		HOLY,
		ACID
	}
	fun dissolve(type: DissolveType, duration: Float, smoothness: Float)
	{
		val stage = stage ?: Statics.stage

		val table = Table()
		table.isTransform = true
		table.originX = referenceWidth / 2
		table.originY = referenceHeight / 2
		table.background = contentTable.background
		table.setSize(referenceWidth, referenceHeight)
		table.setScale(contentTable.scaleX)
		table.setPosition(x + contentTable.x, y + contentTable.y)

		val gradient = when(type)
		{
			DissolveType.BURN -> AssetManager.loadTextureRegion("GUI/burngradient")!!
			DissolveType.HOLY -> AssetManager.loadTextureRegion("GUI/holygradient")!!
			DissolveType.ACID -> AssetManager.loadTextureRegion("GUI/acidgradient")!!
		}

		val cardDissolve = DissolveEffect(table, duration, gradient, smoothness)
		cardDissolve.setPosition(x, y)
		cardDissolve.setSize(width, height)

		stage.addActor(cardDissolve)
		remove()
	}

	companion object
	{
		private val cardCircle = AssetManager.loadTextureRegion("GUI/CardCircle")!!

		fun createFrontTable(desc: FrontTableSimple): Table
		{
			val contentTable = Table()

			if (desc.topText != null)
			{
				val bottom = Label(desc.topText, Statics.skin, "cardrewardtitle")
				bottom.setWrap(true)
				bottom.setAlignment(Align.center)
				contentTable.add(bottom).growX().pad(10f).center()
				contentTable.row()
			}

			contentTable.add(SpriteWidget(desc.image, 64f, 64f, align = Align.bottom)).pad(10f).grow()
			contentTable.row()

			return createCardTable(desc.title, desc.type, desc.titleIcon, contentTable, null)
		}

		fun createFrontTable(desc: FrontTableComplex): Table
		{
			return createCardTable(desc.title, desc.type, desc.titleIcon, desc.content, desc.descriptors, desc.colour)
		}

		fun createCardTable(title: String, type: String, typeIcon: Sprite?, content: Table, descriptors: Array<Pair<Sprite, String?>>? = null, colour: Colour = Colour.WHITE): Table
		{
			val table = Table()

			val typeTable = Table()
			typeTable.alpha = 0.4f
			val typeLabel = AutoScalingLabel(type, 25f, Statics.skin, "card")

			typeTable.add(typeLabel).growX().center()
			table.add(typeTable).expandX().center().width(80f).height(25f)
			table.row()

			table.add(content).pad(10f).grow()
			table.row()

			val circlesTableElevation = if (descriptors != null && descriptors.size > 0) 30f else 0f
			val circlesTables = Table()
			if (descriptors != null)
			{
				circlesTables.add(Table()).growX()
				for (circleDesc in descriptors)
				{
					val circle = SpriteWidget(cardCircle)
					circle.color = colour.color()
					val icon = SpriteWidget(circleDesc.first.copy())
					icon.drawable.drawActualSize = false

					val stack = Stack()
					stack.add(circle)
					stack.add(icon)

					if (circleDesc.second != null)
					{
						stack.addTapToolTip(circleDesc.second!!)
					}

					circlesTables.add(stack).pad(0f, 5f, 0f, 5f).center().height(40f).padBottom(circlesTableElevation)
				}
				circlesTables.add(Table()).growX()
			}

			val bottomStack = Stack()
			val titleIconsTable = Table()
			val bottomTable = Table()

			bottomStack.add(titleIconsTable)
			bottomStack.add(bottomTable)
			bottomStack.add(circlesTables)

			fun addTitleIcons(left: Boolean) {
				if (typeIcon != null) {
					val circle = SpriteWidget(cardCircle).tint(Color(1f, 1f, 1f, 0.2f))
					val icon = SpriteWidget(typeIcon.copy()).tint(Color(1f, 1f, 1f, 0.35f))

					val stack = Stack()
					stack.add(circle)
					stack.add(icon)

					if (left) {
						titleIconsTable.add(stack).size(15f).expandX().pad(3f).left().padTop(circlesTableElevation + 3f)
					} else {
						titleIconsTable.add(stack).size(15f).expandX().pad(3f).right().padTop(circlesTableElevation + 3f)
					}
				}
			}

			addTitleIcons(true)
			addTitleIcons(false)

			val top = AutoScalingLabel(title, 30f, Statics.skin, "card")
			bottomTable.add(top).growX().center().pad(3f).padTop(circlesTableElevation + 3f)

			table.add(bottomStack).height(40f + circlesTableElevation).growX()
			table.row()

			return table
		}

		fun createCard(title: String, type: String, typeIcon: Sprite, smallContent: Table, detailContent: Table, data: Any? = null, colour: Colour = Colour.WHITE, descriptors: Array<Pair<Sprite, String?>>? = null): CardWidget
		{
			return CardWidget(
					createCardTable(title, type, typeIcon, smallContent, descriptors, colour),
					createCardTable(title, type, typeIcon, detailContent, descriptors, colour),
					typeIcon.textures[0],
					data,
					colour)
		}

		fun createCard(title: String, type: String, icon: Sprite, detailContent: Table, typeIcon: TextureRegion, data: Any? = null, colour: Colour = Colour.WHITE, topText: String? = null, descriptors: Array<Pair<Sprite, String?>>? = null): CardWidget
		{
			var descriptors = descriptors
			if (descriptors == null)
			{
				descriptors = gdxArrayOf( Pair(icon.copy(), null))
			}

			return CardWidget(
					CardWidget.createFrontTable(
							FrontTableSimple(title, type, icon.copy(), Sprite(typeIcon), topText)),
					CardWidget.createFrontTable(
							FrontTableComplex(title,
									type,
									detailContent,
									Sprite(typeIcon),
									descriptors, colour)),
					typeIcon,
					data,
					colour)
		}

		fun createCard(desc: FrontTableComplex, data: Any? = null, colour: Colour = Colour.WHITE): CardWidget
		{
			desc.colour = colour
			return CardWidget(createFrontTable(desc), createFrontTable(desc), desc.titleIcon!!.textures[0], data, colour)
		}

		fun layoutCard(card: CardWidget, enterFrom: Direction, dstWidget: Table? = null, animate: Boolean = true)
		{
			layoutCards(gdxArrayOf(card), enterFrom, dstWidget, animate, flip = true)
		}

		fun layoutCards(cardWidgets: Array<CardWidget>, enterFrom: Direction, dstWidget: Table? = null, animate: Boolean = true, flip: Boolean = false, startScale: Float = 1f)
		{
			val areapos = dstWidget?.localToStageCoordinates(Vector2()) ?: Vector2()
			val areaWidth = dstWidget?.width ?: Statics.resolution.x.toFloat()
			val areaHeight = dstWidget?.height ?: Statics.resolution.y.toFloat()

			val cardWidth: Float
			val cardHeight: Float
			if (cardWidgets.size == 1)
			{
				val padding = 20f
				cardWidth = (areaWidth - 3f * padding) / 2f
				cardHeight = (areaHeight - 3f * padding) / 2f

				// center
				cardWidgets[0].setPosition(areaWidth / 2f - cardWidth / 2f + areapos.x, areaHeight / 2f - cardHeight / 2f + areapos.y)
			}
			else if (cardWidgets.size == 2)
			{
				val padding = 20f
				cardWidth = (areaWidth - 3f * padding) / 2f
				cardHeight = (areaHeight - 3f * padding) / 2f

				// vertical alignment
				cardWidgets[0].setPosition(areaWidth / 2f - cardWidth / 2f + areapos.x, padding * 2f + cardHeight + areapos.y)
				cardWidgets[1].setPosition(areaWidth / 2f - cardWidth / 2f + areapos.x, padding + areapos.y)
			}
			else if (cardWidgets.size == 3)
			{
				val padding = 20f
				cardWidth = (areaWidth - 3f * padding) / 2f
				cardHeight = (areaHeight - 3f * padding) / 2f

				// triangle, single card at top, 2 below
				cardWidgets[0].setPosition(areaWidth / 2f - cardWidth / 2f + areapos.x, padding * 2f + cardHeight + areapos.y)
				cardWidgets[1].setPosition(padding + areapos.x, padding + areapos.y)
				cardWidgets[2].setPosition(padding * 2f + cardWidth + areapos.x, padding + areapos.y)
			}
			else
			{

				var cardXCount = 1
				var cardYCount = (cardWidgets.size.toFloat() / cardXCount).ciel()

				val padding = 10f
				while (true)
				{
					val xSize = (areaWidth - (cardXCount + 2) * padding) / cardXCount
					val ySize = (areaHeight - (cardYCount + 2) * padding) / cardYCount

					val scaleX = xSize / cardWidgets[0].referenceWidth
					val scaleY = ySize / cardWidgets[0].referenceHeight
					val min = min(scaleX, scaleY)

					val cardRefWidth = cardWidgets[0].referenceWidth * min
					val cardRefHeight = cardWidgets[0].referenceHeight * min

					val testXCount = cardXCount + 1
					val textYCount = (cardWidgets.size.toFloat() / testXCount).ciel()
					val testXSize = (areaWidth - (testXCount + 2) * padding) / testXCount
					val testYSize = (areaHeight - (textYCount + 2) * padding) / textYCount
					val testScaleX = testXSize / cardWidgets[0].referenceWidth
					val testScaleY = testYSize / cardWidgets[0].referenceHeight
					val testMin = min(testScaleX, testScaleY)

					val testCardWidth = cardWidgets[0].referenceWidth * testMin
					val testCardHeight = cardWidgets[0].referenceHeight * testMin

					if (testCardWidth > cardRefWidth || testCardHeight > cardRefHeight)
					{
						cardXCount = testXCount
						cardYCount = textYCount
					}
					else
					{
						break
					}
				}

				// Calculate card sizes
				cardWidth = min((areaWidth - padding * 2) / 2f, (areaWidth - (cardXCount + 2) * padding) / cardXCount)
				cardHeight = min((areaHeight - padding * 2) / 2f, (areaHeight - (cardYCount + 2) * padding) / cardYCount)

				val normalLayout = (cardWidgets.size.toFloat() / cardXCount).toInt() * cardXCount
				val finalRowLen = cardWidgets.size - normalLayout
				val finalRowStep = (areaWidth - (finalRowLen + 2) * padding) / finalRowLen

				var x = 0
				var y = 0
				for (i in 0 until cardWidgets.size)
				{
					val widget = cardWidgets[i]

					if (i >= normalLayout)
					{
						widget.setPosition(areapos.x + padding + finalRowStep * 0.5f + x * finalRowStep + x * padding - cardWidth * 0.5f, areapos.y + padding + y * cardHeight + y * padding)
					}
					else
					{
						widget.setPosition(areapos.x + padding + x * cardWidth + x * padding, areapos.y + padding + y * cardHeight + y * padding)
					}

					x++
					if (x == cardXCount)
					{
						x = 0
						y++
					}
				}
			}

			for (widget in cardWidgets)
			{
				widget.setSize(cardWidth, cardHeight)
			}

			// do animation
			if (animate)
			{
				val startXSize = cardWidth * startScale
				val startYSize = cardHeight * startScale

				// calculate start position
				val startX: Float = when (enterFrom)
				{
					Direction.CENTER -> areaWidth / 2f - startXSize / 2f
					Direction.NORTH -> areaWidth / 2f - startXSize / 2f
					Direction.SOUTH -> areaWidth / 2f - startXSize / 2f

					Direction.EAST -> areaWidth
					Direction.NORTHEAST -> areaWidth
					Direction.SOUTHEAST -> areaWidth

					Direction.WEST -> -startXSize
					Direction.NORTHWEST -> -startXSize
					Direction.SOUTHWEST -> -startXSize
				}

				val startY: Float = when (enterFrom)
				{
					Direction.CENTER -> areaHeight / 2f - startYSize / 2f
					Direction.EAST -> areaHeight / 2f - startYSize / 2f
					Direction.WEST -> areaHeight / 2f - startYSize / 2f

					Direction.NORTH -> areaHeight
					Direction.NORTHWEST -> areaHeight
					Direction.NORTHEAST -> areaHeight

					Direction.SOUTH -> -startYSize
					Direction.SOUTHWEST -> -startYSize
					Direction.SOUTHEAST -> -startYSize
				}

				for (widget in cardWidgets)
				{
					widget.setSize(startXSize, startYSize)
				}

				var delay = 0.2f
				for (widget in cardWidgets)
				{
					val x = widget.x
					val y = widget.y

					widget.setPosition(startX + areapos.x, startY + areapos.y)

					widget.clickable = false

					val delayVal = delay
					delay += 0.04f
					val sequence = delay(delayVal) then parallel(moveTo(x, y, 0.2f), sizeTo(cardWidth, cardHeight, 0.2f)) then delay(0.1f) then lambda {

						if (flip) widget.setFacing(true, true)
						widget.clickable = true
					}

					widget.addAction(sequence)
				}
			}
		}

		enum class LootAnimation
		{
			CHEST,
			COIN_GOLD,
			COIN_SILVER,
			COIN_BRONZE
		}
		fun displayLoot(cards: Array<CardWidget>, animation: LootAnimation, onCompleteAction: ()->Unit)
		{
			if (cards.size == 0)
			{
				throw RuntimeException("Cannot show loot with 0 cards!")
			}

			val greyoutTable = createGreyoutTable(Statics.stage)

			val cardsTable = Table()
			greyoutTable.add(cardsTable).grow()
			greyoutTable.row()

			val buttonTable = Table()
			greyoutTable.add(buttonTable).growX()

			val dissolveDur = 1f

			var gatheredLoot = 0
			var i = 0
			for (card in cards)
			{
				card.name = "Reward$i"
				i++

				val oldPickFuns = card.pickFuns.toGdxArray()
				card.pickFuns.clear()

				for (oldPick in oldPickFuns)
				{
					card.addPick(oldPick.string) {
						card.dissolve(DissolveType.HOLY, dissolveDur, 6f)

						card.isPicked = true
						oldPick.pickFun(card)

						gatheredLoot++
						if (gatheredLoot == cards.size)
						{
							Future.call(
								{
									greyoutTable.fadeOutAndRemove(0.2f)
									Future.call({onCompleteAction()}, 0.2f)
								}, dissolveDur)

						}
					}
				}
			}

			when (animation)
			{
				LootAnimation.CHEST ->
				{
					val chestClosed = SpriteWidget(AssetManager.loadSprite("Oryx/uf_split/uf_items/chest_gold"), 128f, 128f)
					val chestOpen = SpriteWidget(AssetManager.loadSprite("Oryx/uf_split/uf_items/chest_gold_open"), 128f, 128f)

					val wobbleDuration = 1f
					chestClosed.setPosition(Statics.stage.width / 2f - 64f, Statics.stage.height / 2f - 64f)
					chestClosed.setSize(128f, 128f)
					chestClosed.addAction(WobbleAction(0f, 35f, 0.1f, wobbleDuration))
					Statics.stage.addActor(chestClosed)

					Future.call(
						{
							chestClosed.remove()

							chestOpen.setPosition(Statics.stage.width / 2f - 64f, Statics.stage.height / 2f - 64f)
							chestOpen.setSize(128f, 128f)
							Statics.stage.addActor(chestOpen)

							val effect = AssetManager.loadParticleEffect("ChestOpen").getParticleEffect()
							val particleActor = ParticleEffectActor(effect, true)
							particleActor.setSize(128f, 128f)
							particleActor.setPosition(Statics.stage.width / 2f - 64f, Statics.stage.height / 2f - 64f)
							Statics.stage.addActor(particleActor)

							Future.call(
								{
									val sequence = Actions.delay(0.2f) then Actions.fadeOut(0.3f) then Actions.removeActor()
									chestOpen.addAction(sequence)

									for (card in cards)
									{
										Statics.stage.addActor(card)
									}

									layoutCards(cards, Direction.CENTER, cardsTable, startScale = 0.3f, flip = true)
								}, 0.4f)
						}, wobbleDuration)
				}

				LootAnimation.COIN_GOLD, LootAnimation.COIN_BRONZE, LootAnimation.COIN_SILVER ->
				{
					val coin = SpriteWidget(AssetManager.loadSprite("Oryx/Custom/items/coin_grey"), 128f, 128f)
					coin.color = when (animation) {
						LootAnimation.COIN_GOLD -> Color.GOLD
						LootAnimation.COIN_SILVER -> Color.WHITE
						LootAnimation.COIN_BRONZE -> Color.ORANGE
						else -> throw RuntimeException("Eh?")
					}
					val crack1 = SpriteWidget(AssetManager.loadSprite("Oryx/Custom/items/coin_break_1"), 128f, 128f)
					val crack2 = SpriteWidget(AssetManager.loadSprite("Oryx/Custom/items/coin_break_2"), 128f, 128f)
					val crack3 = SpriteWidget(AssetManager.loadSprite("Oryx/Custom/items/coin_break_3"), 128f, 128f)

					val stack = Stack()
					stack.add(coin)

					stack.setPosition(Statics.stage.width / 2f - 64f, Statics.stage.height / 2f - 64f)
					stack.setSize(128f, 128f)

					val bumpDur = 0.1f
					val bumpDist = 16f
					val delay = 0.6f

					val sequence =
						// crack 1
						delay(delay) then
							parallel(
								bump(Vector2(Random.random(-1f, 1f), Random.random(-1f, 1f)).nor(), bumpDist, bumpDur),
								lambda {
									crack1.alpha = 0f
									crack1.addAction(fadeIn(bumpDur * 0.5f))
									stack.add(crack1)
								}) then

							// crack 2
							delay(delay) then
							parallel(
								bump(Vector2(Random.random(-1f, 1f), Random.random(-1f, 1f)).nor(), bumpDist, bumpDur),
								lambda {
									crack2.alpha = 0f
									crack2.addAction(fadeIn(bumpDur * 0.5f))
									stack.add(crack2)
								}) then

							// crack 3
							delay(delay) then
							parallel(
								bump(Vector2(Random.random(-1f, 1f), Random.random(-1f, 1f)).nor(), bumpDist, bumpDur),
								lambda {
									crack3.alpha = 0f
									crack3.addAction(fadeIn(bumpDur * 0.5f))
									stack.add(crack3)
								})

					stack.addAction(sequence)

					Statics.stage.addActor(stack)

					Future.call(
						{
							stack.remove()

							val effect = AssetManager.loadParticleEffect("ChestOpen").getParticleEffect()
							val particleActor = ParticleEffectActor(effect, true)
							particleActor.setSize(128f, 128f)
							particleActor.setPosition(Statics.stage.width / 2f - 64f, Statics.stage.height / 2f - 64f)
							Statics.stage.addActor(particleActor)

							Future.call(
								{
									for (card in cards)
									{
										Statics.stage.addActor(card)
									}

									layoutCards(cards, Direction.CENTER, cardsTable, startScale = 0.3f, flip = true)
								}, 0.4f)
						}, delay * 4 + bumpDur * 3)
				}
			}
		}
	}
}

class FrontTableSimple(val title: String, val type: String, val image: Sprite, var titleIcon: Sprite? = null, var topText: String? = null)

class FrontTableComplex(val title: String, val type: String, val content: Table, var titleIcon: Sprite? = null, val descriptors: Array<Pair<Sprite, String?>> = Array(), var colour: Colour = Colour.WHITE)

class RemoveAwareTable(val removeFun: ()->Unit) : Table()
{
	override fun childrenChanged()
	{
		super.childrenChanged()

		if (stage == null)
		{
			removeFun.invoke()
		}
	}
}
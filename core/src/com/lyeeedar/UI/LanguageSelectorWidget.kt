package com.lyeeedar.UI

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.*

class Language(val code: String, val name: String, val icon: Sprite, val isAutoTranslated: Boolean)

class LanguageSelectorWidget(skin: Skin) : Table(skin)
{
	private val tick = AssetManager.loadSprite("Oryx/uf_split/uf_interface/uf_interface_680", colour = Colour(Color.FOREST))

	val languages = Array<Language>()
	private val warningLabel = Label("", skin)
	var selectedLanguage: Language

	init
	{
		warningLabel.setWrap(true)
		warningLabel.name = "languageWarning"

		val languagesXml = getXml("Localisation/Languages.xml")
		for (el in languagesXml.children)
		{
			val code = el.get("Code")
			val name = el.get("Name")
			val icon = AssetManager.loadSprite(el.getChildByName("Icon")!!)
			val isAutoTranslated = el.getBoolean("IsAutoTranslated", false)

			languages.add(Language(code, name, icon, isAutoTranslated))
		}

		selectedLanguage = languages.first { it.code == Statics.language }

		rebuild()
	}

	fun rebuild()
	{
		clear()

		if (selectedLanguage.isAutoTranslated)
		{
			add(warningLabel).growX().height(30f)
			warningLabel.setText("* " + Localisation.getText("languageselector.isautotranslated", "UI", selectedLanguage.code))
			row()
		}
		else
		{
			add(Table()).height(30f)
			row()
		}

		val languagesTable = Table()

		for (language in languages)
		{
			val languageTable = Table()
			languageTable.name = "Language_${language.code}"
			languageTable.background = NinePatchDrawable(NinePatch(AssetManager.loadTextureRegion("Sprites/GUI/Button.png"), 6, 6, 6, 6))

			if (language == selectedLanguage)
			{
				languageTable.add(SpriteWidget(tick)).growY().width(Value.percentWidth(0.2f, languageTable)).pad(3f)
			}
			else
			{
				languageTable.add(Table()).growY().width(Value.percentWidth(0.2f, languageTable)).pad(3f)
			}

			var name = language.name
			if (language.isAutoTranslated)
			{
				name += " *"
			}
			languageTable.add(Label(name, skin)).grow().pad(3f)
			languageTable.add(SpriteWidget(language.icon)).growY().width(Value.percentWidth(0.2f, languageTable)).pad(3f)

			languageTable.touchable = Touchable.enabled
			languageTable.addClickListener {
				selectedLanguage = language
				rebuild()
			}

			languagesTable.add(languageTable).growX().pad(3f)
			languagesTable.row()
		}

		val scroll = ScrollPane(languagesTable, skin)
		scroll.setScrollingDisabled(true, false)
		scroll.setForceScroll(false, true)
		scroll.fadeScrollBars = false
		add(scroll).grow()
	}
}
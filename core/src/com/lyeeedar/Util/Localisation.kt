package com.lyeeedar.Util

import com.badlogic.gdx.utils.IntMap
import ktx.collections.set

class Localisation
{
	companion object
	{
		var loaded = false
		val localisedIds: IntMap<IntMap<String>> = IntMap()

		fun loadLocalisation()
		{
			loaded = true

			val languagesXml = getXml("Localisation/Languages.xml")
			for (el in languagesXml.children)
			{
				val code = el.get("Code")
				val map = IntMap<String>()
				localisedIds[code.hashCode()] = map

				for (file in XmlData.enumeratePaths("Localisation/$code", "Localisation"))
				{
					val fileName = file.filename(false)
					val xml = getXml(file)

					for (el in xml.children)
					{
						val trueId = "$fileName/${el.getAttribute("ID")}"
						map[trueId.hashCode()] = el.value.toString()
					}
				}
			}
		}

		fun invalidate()
		{
			localisedIds.clear()
			loadLocalisation()
		}

		fun getText(id: String, file: String, language: String? = null): String
		{
			if (!loaded)
			{
				loadLocalisation()
			}

			val languageCode = language ?: Statics.language
			val languageIds = localisedIds[languageCode.hashCode()] ?: throw RuntimeException("Language $languageCode does not exist!")

			val trueId = "$file/$id"
			val hash = trueId.hashCode()

			return languageIds[hash] ?: throw RuntimeException("Id $trueId does not exist in $languageCode!")
		}
	}
}
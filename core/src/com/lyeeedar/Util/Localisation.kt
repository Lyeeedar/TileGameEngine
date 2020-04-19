package com.lyeeedar.Util

import com.badlogic.gdx.utils.IntMap
import ktx.collections.set

class Localisation
{
	companion object
	{
		val localisedIds: IntMap<String> by lazy {
			val map = IntMap<String>()

			val languagesXml = getXml("Localisation/Languages.xml")
			for (el in languagesXml.children)
			{
				val code = el.get("Code")

				for (file in XmlData.enumeratePaths("Localisation/$code", "Localisation"))
				{
					val fileName = file.filename(false)
					val xml = getXml(file)

					for (el in xml.children)
					{
						val trueId = "$code@$fileName/${el.getAttribute("ID")}"
						map[trueId.hashCode()] = el.value.toString()
					}
				}
			}

			map
		}

		fun getText(id: String, file: String, language: String? = null): String
		{
			val languageCode = language ?: Statics.language
			val trueId = "$languageCode@$file/$id"
			val hash = trueId.hashCode()

			return localisedIds[hash] ?: throw RuntimeException("Id $trueId does not exist!")
		}
	}
}
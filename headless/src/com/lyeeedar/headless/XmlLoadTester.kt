package com.lyeeedar.headless

import com.lyeeedar.AI.BehaviourTree.BehaviourTree
import com.lyeeedar.ActionSequence.ActionSequence
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.Particle.ParticleEffectDescription
import com.lyeeedar.Util.XmlData
import java.io.File
import java.util.*

class XmlLoadTester
{
	companion object
	{
		fun test()
		{
			println("")
			println("")
			println("-------------------------------------------------------------------------")
			println("")
			println("#####      Xml Load Tester      #######")
			println("")
			println("-------------------------------------------------------------------------")
			println("Running in directory: " + File("").absolutePath)
			println("")
			println("")

			for (path in XmlData.getExistingPaths().toList())
			{
				try
				{
					System.out.println("Begin test load '$path'")

					val xml = XmlData.getXml(path)
					when (xml.name.toUpperCase(Locale.ENGLISH))
					{
						"EFFECT" -> ParticleEffect.load(path.split("Particles/")[1], ParticleEffectDescription(""))
						"BEHAVIOURTREE" -> BehaviourTree.load(path)
						"ACTIONSEQUENCE" -> ActionSequence.load(path)
						"MAPGENERATOR" -> MapGenerator.load(path)
						else -> GameXmlLoadTester.testLoad(xml, path)
					}

					System.out.println("Success")
				}
				catch (ex: Exception)
				{
					System.err.println("Failed to load '$path'")
					throw ex
				}
			}
		}
	}
}
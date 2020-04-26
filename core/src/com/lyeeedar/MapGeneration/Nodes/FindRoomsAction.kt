package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.XmlData
import ktx.collections.set
import squidpony.squidgrid.mapping.RoomFinder

@DataClass(category = "Rooms", colour = "240,207,1")
class FindRoomsAction : AbstractMapGenerationAction()
{
	lateinit var roomName: String
	lateinit var corridorName: String

	override fun execute(generator: MapGenerator, args: NodeArguments)
	{
		val asChars = Array<CharArray>(args.area.width) { x -> CharArray(args.area.height) { y -> args.area[x, y]!!.char } }
		val roomFinder = RoomFinder(asChars)

		if (roomName.isNotBlank())
		{
			val rooms = roomFinder.findRooms()
			var roomsStore = generator.namedAreas[roomName]
			if (roomsStore == null)
			{
				roomsStore = com.badlogic.gdx.utils.Array()
				generator.namedAreas[roomName] = roomsStore
			}
			for (room in rooms)
			{
				val newArea = args.area.newAreaFromCharGrid(room)
				roomsStore.add(newArea)
			}
		}

		if (corridorName.isNotBlank())
		{
			val corridors = roomFinder.findCorridors()
			var corridorsStore = generator.namedAreas[corridorName]
			if (corridorsStore == null)
			{
				corridorsStore = com.badlogic.gdx.utils.Array()
				generator.namedAreas[corridorName] = corridorsStore
			}
			for (corridor in corridors)
			{
				val newArea = args.area.newAreaFromCharGrid(corridor)
				corridorsStore.add(newArea)
			}
		}
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		roomName = xmlData.get("RoomName")
		corridorName = xmlData.get("CorridorName")
		afterLoad()
	}
	override val classID: String = "FindRooms"
	override fun resolve(nodes: ObjectMap<String, MapGeneratorNode>)
	{
		super.resolve(nodes)
	}
	//endregion
}
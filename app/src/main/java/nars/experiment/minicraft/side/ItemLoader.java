/*
 * Copyright 2012 Jonathan Leahey
 * 
 * This file is part of Minicraft
 * 
 * Minicraft is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * Minicraft is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Minicraft. If not, see http://www.gnu.org/licenses/.
 */

package nars.experiment.minicraft.side;

import nars.experiment.minicraft.side.items.Items;
import nars.experiment.minicraft.side.items.Tools;

import java.util.HashMap;


public class ItemLoader {
	//private static final FSTGson gson = new Gson();
	
	public static HashMap<Character, Item> loadItems(int size) {
		ItemDefinition[] items = Items.items;
		ToolDefinition[] tools = Tools.tools;

//		// TODO: use the streaming API: https://sites.google.com/site/gson/streaming
//		try {
//			tools = new JsonFactory().createParser("").read
//                    //gson
//					//.fromJson(StockMethods.readFile("items/tools.json"), ToolDefinition[].class);
//			items = gson
//					.fromJson(StockMethods.readFile("items/items.json"), ItemDefinition[].class);
//		} catch (IOException e) {
//		}
//		if (tools == null || items == null) {
//			System.err.println("Failed to load items from json.");
//			System.exit(5);
//		}
		
		HashMap<Character, Item> itemTypes = new HashMap<Character, Item>();
		for (ToolDefinition td : tools) {
			itemTypes.put((char) td.item_id, td.makeTool(size));
		}
		for (ItemDefinition id : items) {
			itemTypes.put((char) id.item_id, id.makeItem(size));
		}
		return itemTypes;
	}
}


package nars.experiment.rogue.map;

import java.util.List;

public class LinearGlobalMap
{
	private List<MapDescriptor> myLevels;

	public Map getMapByNumber(int number)
	{
		if (myLevels==null)
		{
			throw new IllegalStateException("Levels list is null");
		}
		return myLevels.get(number).getMap();
	}

	
}


package nars.experiment.nario2.levels;


import nars.experiment.nario2.Level;

public class Level1 extends Level
{
    public Level1(String[] newMap)
    {
        String[] charMap = 
        {
            "c:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::c",            
            "c:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::c",
            "c:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::c",
            "c:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::c",
            "c:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::c",
            "c::::::::::::::::::::::::::::::::::::::::::::::ggggg::::::::::::::c",
            "c:::gggggg:::gggggg:::::::::::::::::::::::::::::::::::::::::::::::c",
            "c:::::::::::::::::::::::::::gggggg:::::gggggg:::::::::::::::::::::c",            
            "cgg:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::c",
            "cggg:::::::::::::::::::ggQgg:::::::::::::::::::ggggggggggggggg::::c",
            "cgggg::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::gc",
            "cggggg:::::::::::gggggggggggggg:::::::::::::::::::::::::::::::::ggc",            
            "cggggggggggg::::::::::::::::::::::gggggggggggggg:::::::::::::::gggc",
            "c::::::::::gggg:::::::::::::::::ggg:::::::::::::::::::::::::ggggggc",
            "c::::::::::::::gQ:::::::::::Qggg::::::::::::::::::gggggggggggggg::c",
            "c:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::c",            
            "c:::::::::::ggggggggggggggggggggggg::::::ggggggg::::::::::::::::::c",
            "c::::::::::gg::::::::::::::::::::::::::::::::::gg::::::ggggggg:::gc",
            "c::::::::gg:::::::::::::::::::::::::ggg:::::::::gg:::::::::::::::gc",
            "c::::gggg::::::::::gggg::::::::::::::::::::::::::ggg::::::::::::ggc",            
            "cg:::::::::::::::::g:::gggggggggggggggggggggg::::::::::::::::::gggc",
            "cgg:::::::::::::::g::::::::::::::::::::::::::::::::::::::::::gggggc",
            "cggggg:::::::::::::::::::::::::::::::::::::::::::::::::::::gggggggc",
            "cgggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggc",
            
        };

    
        /*
        for(int i=0; i<charMap.length; i++)
        {
            for(int j=0; j<charMap[i].length(); j++)
            {
                if (j == 0 || j == charMap[i].length()-1)
                {
                    char[] chars = charMap[i].toCharArray();
                    chars[j] = 'g';
                    charMap[i] = new String(chars);
                }
            }
        }
        * 
        */
        if (newMap == null)
        {
            super.initMap(charMap);
        }
        else
        {
            super.initMap(newMap);
        }
    }
}

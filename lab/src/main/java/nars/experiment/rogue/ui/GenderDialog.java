package nars.experiment.rogue.ui;

import nars.experiment.rogue.combat.PtrlConstants;
import nars.experiment.rogue.creatures.Player;

import java.awt.event.KeyEvent;

public class GenderDialog implements IGameScreen
{

	public GenderDialog(Player p)
	{
		pc=p;
		cur=0;
		options = new String[] {"Male", "Female"}; 
	}
	
	@Override
    public void paint(Console c)
	{
		c.clear();
		int sh=c.getSymHeight();
		int sw=c.getSymWidth();
		String head = "Select your sex:";
		int w=head.length();
		int h=4;
		int y = c.getSymHeight() /2 - h /2;
		int x = c.getSymWidth() /2 - w /2;
		c.printString(head, x, y, PtrlConstants.LCYAN);
		short col;
		for (int i=0;i<options.length;i++)
		{
			if (i==cur) col=PtrlConstants.WHITE;
			else col=PtrlConstants.LGRAY;
		
			c.printString(options[i], x, y+i+2, col); 
		}
	}
	@Override
    public boolean getKeyEvent(KeyEvent ke)
	{
		char ch=ke.getKeyChar();
		if ((ch=='8'||ke.getKeyCode()==KeyEvent.VK_UP)&&cur>0) cur--;
		else if ((ch=='2'||ke.getKeyCode()==KeyEvent.VK_DOWN)&&cur<options.length-1) cur++;
		else if (ke.getKeyCode()==KeyEvent.VK_ENTER)
		{
			pc.setSex(cur);
			return true;
		}

		return false;	
	}
	
	private final Player pc;
	private int cur;
	private final String[] options;
}

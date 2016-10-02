package nars.experiment.rogue.ui;

import nars.experiment.rogue.combat.PtrlConstants;
import nars.experiment.rogue.creatures.Player;

import java.awt.event.KeyEvent;

public class NameDialog implements IGameScreen
{

	public NameDialog(Player p)
	{
		pc=p;
		name="";
	}
	
	@Override
    public void paint(Console c)
	{
		c.clear();
		int sh=c.getSymHeight();
		int sw=c.getSymWidth();
		String question = "Enter your name:";
		int y = sh /2 - 1;
		int x = sw /2 - question.length()/2;
		c.printString(question, x, y, PtrlConstants.LCYAN);
		c.printString(name, x, y+2, PtrlConstants.WHITE);
	}

	@Override
    public boolean getKeyEvent(KeyEvent ke)
	{
		//TODO: improve symbol displayability check using regexps
		if (ke.getKeyChar()!= KeyEvent.CHAR_UNDEFINED &&ke.getKeyCode()!=KeyEvent.VK_ENTER&&ke.getKeyCode()!=KeyEvent.VK_BACK_SPACE) name+=ke.getKeyChar();
		else if (ke.getKeyCode()==KeyEvent.VK_BACK_SPACE&& !name.isEmpty()) name=name.substring(0, name.length()-1);
		else if (ke.getKeyCode()==KeyEvent.VK_ENTER&& !name.isEmpty())
		{
			pc.setName(name);
			return true;
		}
		return false;
	}
	
	private final Player pc;
	private String name;
}

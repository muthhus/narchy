package ideal.vacuum.view;


import ideal.vacuum.Environment;

import javax.swing.*;
import java.awt.*;
import java.awt.Dialog;

public class ConfigureRunDlg extends Dialog
{
	public static final long serialVersionUID = 1;
	
	 private final JTextField m_steps = new JTextField(50);
	 private final JTextField m_delay = new JTextField(50);

	 private final Environment m_env;
	
    public ConfigureRunDlg(JFrame parent, Environment env)
	{
        super(parent, "Configure Run");
		
		m_env = env;

        JPanel center = new JPanel(new GridLayout(2, 2));
        center.add(new Label("Time Steps: "));
        center.add(m_steps);
        center.add(new Label("Delay Between Steps (ms): "));
        center.add(m_delay);

        add(center, BorderLayout.CENTER);

		setSize(375, 130);
	}

	@Override
    public void setVisible(boolean b)
	{
		m_delay.setText(""+m_env.getDelay());
		super.setVisible(b);
	}

	/**
	 * Handles the modifications
	 * @autor mcohen
	 * @author ogeorgeon save the modifications as preferences
	 */
    protected boolean handleOk()
	{
		boolean bRet = true;
		try 
		{
			int iSteps = Integer.parseInt(m_steps.getText());
			int iDelay = Integer.parseInt(m_delay.getText());

			if (iSteps <=0)
			{
				JOptionPane.showMessageDialog(this, 
					"Please enter a positive integer for steps!",
					"Error!", 
					JOptionPane.ERROR_MESSAGE);
				bRet = false;
			}
			else if (iDelay <= 0)
			{
				JOptionPane.showMessageDialog(this, 
					"Delay must be a positive integer!",
					"Error!", 
					JOptionPane.ERROR_MESSAGE);
				bRet = false;
			}

			if (bRet)
			{
				m_env.setDelay(iDelay);
				m_env.putPreferences();
			}
		}
		catch (NumberFormatException e)
		{
			JOptionPane.showMessageDialog(this, 
				"Please enter a valid integer!",
				"Error!", 
				JOptionPane.ERROR_MESSAGE);
			bRet = false;
		}
		return bRet;
	}

	/**
	 * Handles default button
	 * @author ogeorgeon Reset the default values
	 */
    protected void handleDefault()
    {
    	m_steps.setText("" + Environment.INIT_STEPS);
    	m_delay.setText("" + Environment.INIT_DELAY);
    }

   
}

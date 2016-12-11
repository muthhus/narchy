package automenta.vivisect.swing;

import nars.Video;

/**
 * Button using FontAwesome icon as a label
 */
public class AwesomeButton extends JButton {

	public AwesomeButton(char faCode) {
		setFont(Video.FontAwesome);
		setText(String.valueOf(faCode));
	}
}

package cleargl.util.recorder;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;

public class FolderChooser extends JFileChooser
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private FolderChooser(String pChooserTitle, File pDefaultFolder)
	{
		super();
		setDialogTitle(pChooserTitle);
		setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		setAcceptAllFileFilterUsed(false);
		setFileFilter(new FileFilter()
		{

			@Override
			public boolean accept(File f)
			{
				return f.isDirectory();
			}

			@Override
			public String getDescription()
			{
				return "Directories only";
			}

		});
		setCurrentDirectory(pDefaultFolder.getParentFile());
		setSelectedFile(pDefaultFolder);
	}

	public static File openFolderChooser(	Component pParent,
																				String pChooserTitle,
																				File pDefaultFolder)
	{
		FolderChooser lFolderChooser = new FolderChooser(	pChooserTitle,
																											pDefaultFolder);

		if (lFolderChooser.showOpenDialog(pParent) == JFileChooser.APPROVE_OPTION)
		{
			return lFolderChooser.getSelectedFile();
		}
		else
		{
			return null;
		}
	}
}

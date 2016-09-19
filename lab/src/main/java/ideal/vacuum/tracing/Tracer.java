package ideal.vacuum.tracing;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


/**
 * This tracer logs the trace into a text file.
 * @author ogeorgeon 
 */
public class Tracer implements ITracer<Object>
{

	private  File logFile;

	/**
	 * Initialize the tracer.
	 * @param fileName The name of the file where to log the trace.
	 */
	public Tracer(String fileName)
	{ 	
		try 
		{
			logFile = new File(fileName);
			if (logFile.exists()) 
			{
				logFile.delete();
				logFile.createNewFile();
			}
		}
		catch (IOException e) {
			System.out.println("Error creating the file " + fileName);
			e.printStackTrace();
		}
	} 
	
	/**
	 * Prints a line to the log file. 
	 * @param line The line to print in the log file.
	 * @return True if success, false if failure.
	 */
	public boolean writeLine(String line) 
	{
		boolean r = false;
		if (logFile != null)
		{
			try 
			{
				FileWriter writer = new FileWriter(logFile, true);
				writer.write(line );
				writer.close();
				r = true;
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		return r;
	}

	@Override
    public boolean close()
	{
		return true;
	}

	@Override
    public void startNewEvent(int t)
	{
		addEventElement("cycle", " " + t); 
	}
	
	@Override
    public void finishEvent() {}

	@Override
    public void addEventElement(String name, String value)
	{
		addEventElementImpl(name, value);
	}
	
	@Override
    public Object addEventElement(String name)
	{
		return addEventElementImpl(name, "");
	}
	
	private Object addEventElementImpl(String name, String value) 
	{
		if (name.equals("cycle") || name.equals("enacted_act") || name.equals("interrupted")) 
		{
			try 
			{
				FileWriter writer = new FileWriter(logFile, true);
				writer.write(value );
				writer.close();
			} catch (IOException e)
			{
				System.out.println("Error logging the trace.");
				e.printStackTrace();
			}
		}	
		return null;
	}

	@Override
    public Object addSubelement(Object element, String name)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
    public void addSubelement(Object element, String name, String textContent)
	{
		// TODO Auto-generated method stub
	}

	@Override
    public Object newEvent(String source, String type, int t)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
    public Object addEventElement(String name, boolean display)
	{
		return addEventElementImpl(name, "");
	}
}

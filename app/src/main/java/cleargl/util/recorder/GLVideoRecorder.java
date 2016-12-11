package cleargl.util.recorder;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Math.abs;

/**
 * This class offers basic functionality to record the framebuffer of a
 * GLAutoDrawable. Videos are saved in the form of a folder containing PNG
 * files. It is up to the user to turn this into a single file video using their
 * favourite too. FiJi is an obvious choice.
 * 
 * Usage is simple:
 * 
 * <pre>
 * 
 * // Create the class an provide a default location to save the videos:
 * GLVideoRecorder lGLVideoRecorder = new GLVideoRecorder(new File(&quot;.&quot;));
 * 
 * // toggle the recording state:
 * lGLVideoRecorder.toggleActive();
 * 
 * // From the display method, aftr all drawing happens, call this:
 * lGLVideoRecorder.screenshot(lGLAutoDrawable);
 * 
 * </pre>
 * 
 * @author royer
 */
public class GLVideoRecorder
{

	private ExecutorService mExecutorService;

	private File mRootFolder = null;
	private volatile File mVideoFolder;
	private volatile long mVideoCounter = 0;
	private volatile long mImageCounter = 0;
	private volatile long mLastImageTimePoint = 0;
	private volatile boolean mFirstTime = true;

	private volatile boolean mActive = false;
	private volatile double mTargetFrameRate = 30;

	private final ReentrantLock mReadPixelsLock = new ReentrantLock();

	private final ConcurrentLinkedQueue<ByteBuffer> mPixelRGBBufferQueue = new ConcurrentLinkedQueue<ByteBuffer>();
	private final ThreadLocal<int[]> mPixelRGBIntsThreadLocal = new ThreadLocal<int[]>();
	private final ThreadLocal<BufferedImage> mPixelRGBBufferedImageThreadLocal = new ThreadLocal<BufferedImage>();

	/**
	 * Creates a GLVideoRecorder with a given root folder for saving the video
	 * files.
	 * 
	 * @param pRootFolder
	 *          folder in which video files will be saved.
	 */
	public GLVideoRecorder(File pRootFolder)
	{
		super();
		setRootFolder(pRootFolder);
	}

	/**
	 * If display requests need to be issued to force display update, a thread can
	 * be started here for that purpose.
	 * 
	 * @param pDisplayRequestRunnable
	 *          Runnable describing how to request a display update.
	 */
	public void startDisplayRequestDeamonThread(final Runnable pDisplayRequestRunnable)
	{
		if (pDisplayRequestRunnable != null)
		{
			final Runnable lDisplayRequestRunnable = new Runnable()
			{

				@Override
				public void run()
				{
					while (true)
					{
						if (mActive)
						{
							System.out.println("Recorder requests display now!");
							pDisplayRequestRunnable.run();
						}
						final int lTargetPeriodInMilliSeconds = (int) (1000 / getTargetFrameRate());
						try
						{
							Thread.sleep(lTargetPeriodInMilliSeconds);
						}
						catch (final InterruptedException e)
						{
						}
					}
				}
			};
			final Thread lDisplayRequestDeamonThread = new Thread(lDisplayRequestRunnable,
																														GLVideoRecorder.class.getSimpleName() + ".DisplayRequestThread");
			lDisplayRequestDeamonThread.setDaemon(true);
			lDisplayRequestDeamonThread.setPriority(Thread.MIN_PRIORITY);
			lDisplayRequestDeamonThread.start();
		}
	}

	/**
	 * Returns the target frame rate in FPS.
	 * 
	 * @return target frame rate in FPS.
	 */
	public double getTargetFrameRate()
	{
		return mTargetFrameRate;
	}

	/**
	 * Sets target frame rate in FPS.
	 * 
	 * @param pTargetFrameRate
	 *          target framerate in FPS.
	 */
	public void setTargetFrameRate(double pTargetFrameRate)
	{
		mTargetFrameRate = pTargetFrameRate;
	}

	/**
	 * Returns root folder.
	 * 
	 * @return root folder file.
	 */
	public File getRootFolder()
	{
		return mRootFolder;
	}

	/**
	 * Sets root folder.
	 * 
	 * @param pFolder
	 *          root folder file.
	 */
	public void setRootFolder(File pFolder)
	{
		mRootFolder = pFolder;
		mRootFolder.mkdirs();
	}

	/**
	 * Toggles the recorder between active (recording) and inactive
	 * (not-recording). By default the recorder starts in inactive mode. When
	 * toggling to inactive, the recorder will block until recording is finished.
	 */
	public void toggleActive()
	{
		if (!mActive)
		{
			if (mFirstTime)
			{
				mRootFolder = FolderChooser.openFolderChooser(null,
																											"Choose root folder to save videos",
																											mRootFolder);
				mFirstTime = false;
			}

			while (getNewVideoFolder())
				mVideoCounter++;
			mVideoFolder.mkdirs();

			mExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime()
																															.availableProcessors());

			mActive = true;
		}
		else
		{
			mActive = false;
			mExecutorService.shutdown();

			final JDialog lJDialog = new JDialog(	(JFrame) null,
																						"Saving video",
																						true);

			SwingUtilities.invokeLater(new Runnable()
			{

				@Override
				public void run()
				{
					final JProgressBar lJProgressBar = new JProgressBar(0, 500);
					lJProgressBar.setValue(499);
					lJDialog.add(BorderLayout.CENTER, lJProgressBar);
					lJDialog.add(	BorderLayout.NORTH,
												new JLabel("Saving images, please wait!"));
					lJDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
					lJDialog.setSize(300, 75);
					lJDialog.validate();
					lJDialog.setVisible(true);
				}
			});

			try
			{
				mExecutorService.awaitTermination(30, TimeUnit.SECONDS);
			}
			catch (final InterruptedException e)
			{
				e.printStackTrace();
			}
			mExecutorService.shutdownNow();
			mExecutorService = null;

			lJDialog.setVisible(false);
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{

					lJDialog.setModal(false);
					lJDialog.dispose();
				}
			});

		}
	}

	private boolean getNewVideoFolder()
	{
		final String lVideoFolderName = String.format("Video.%d",
																									mVideoCounter);
		mVideoFolder = new File(mRootFolder, lVideoFolderName);
		return mVideoFolder.exists();
	}

	/**
	 * This method nust be called from within a JOGL display method, after all
	 * rendering that needs to be recorded has been done.
	 * 
	 * @param pGLAutoDrawable
	 *          JOGL GLAutoDrawable to be used to get pixel data from.
	 * @param pAsynchronous
	 *          true if call should be asynchronous
	 */
	public void screenshot(	GLAutoDrawable pGLAutoDrawable,
													boolean pAsynchronous)
	{
		if (!mActive || tooSoon())
			return;

		final String lFileName = String.format(	"image%d.png",
																						mImageCounter);
		final File lNewFile = new File(mVideoFolder, lFileName);
		writeDrawableToFile(pGLAutoDrawable, lNewFile, pAsynchronous);

	}

	private boolean tooSoon()
	{
		final long lCurrentTimePoint = System.nanoTime();
		final double lElpasedTimeInSeconds = 0.001 * 0.001 * 0.001 * (abs(lCurrentTimePoint - mLastImageTimePoint));
		final double lTargetPeriodInSeconds = 1 / getTargetFrameRate();
		if (lElpasedTimeInSeconds < lTargetPeriodInSeconds)
		{
			// System.out.println("too soon!");
			return true;
		}
		// System.out.println("go!");
		return false;
	}

	/**
	 * Draws the contents of a GLAutoDrawable onto a file asynchronously or not.
	 * 
	 * Code adapted from: http://www.java-gaming.org/index.php/topic,5386.
	 * 
	 * @param pDrawable
	 *          JOGL drawable
	 * @param pOutputFile
	 *          output file
	 * @param pAsynchronous
	 */
	private void writeDrawableToFile(	GLAutoDrawable pDrawable,
																		final File pOutputFile,
																		final boolean pAsynchronous)
	{
		final int lTargetPeriodInMiliSeconds = (int) (1000 / getTargetFrameRate());
		try
		{
			final boolean lIsLocked = mReadPixelsLock.tryLock(lTargetPeriodInMiliSeconds / 2,
																												TimeUnit.MILLISECONDS);

			if (lIsLocked)
			{
				final int lWidth = pDrawable.getSurfaceWidth();
				final int lHeight = pDrawable.getSurfaceHeight();

				ByteBuffer lByteBuffer = mPixelRGBBufferQueue.poll();
				if (lByteBuffer == null || lByteBuffer.capacity() != lWidth * lHeight
																															* 3)
				{
					lByteBuffer = ByteBuffer.allocateDirect(lWidth * lHeight
																									* 3)
																	.order(ByteOrder.nativeOrder());
				}

				final GL lGL = pDrawable.getGL();

				lGL.glPixelStorei(GL.GL_PACK_ALIGNMENT, 1);
				lGL.glReadPixels(0, // GLint x
													0, // GLint y
													lWidth, // GLsizei width
													lHeight, // GLsizei height
													GL.GL_RGB, // GLenum format
													GL.GL_UNSIGNED_BYTE, // GLenum type
													lByteBuffer); // GLvoid *pixels
				mLastImageTimePoint = System.nanoTime();

				final ByteBuffer lFinalByteBuffer = lByteBuffer;
				if (pAsynchronous)
				{
					writeBufferToFile(pOutputFile,
														lWidth,
														lHeight,
														lFinalByteBuffer);
				}
				else if (mExecutorService != null)
				{
					mExecutorService.execute(new Runnable()
					{
						@Override
						public void run()
						{
							writeBufferToFile(pOutputFile,
																lWidth,
																lHeight,
																lFinalByteBuffer);

						}
					});
				}
			}

		}
		catch (final InterruptedException e)
		{
		}
		catch (final RejectedExecutionException e)
		{
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (mReadPixelsLock.isHeldByCurrentThread())
				mReadPixelsLock.unlock();
		}

	}

	/**
	 * Draws the contents of a ByteBuffer onto a PNG file.
	 * 
	 * Code adapted from: http://www.java-gaming.org/index.php/topic,5386.
	 * 
	 * @param pOutputFile
	 * @param pWidth
	 * @param pHeight
	 * @param pByteBuffer
	 */
	private void writeBufferToFile(	File pOutputFile,
																	int pWidth,
																	int pHeight,
																	ByteBuffer pByteBuffer)
	{
		try
		{
			int[] lPixelInts = mPixelRGBIntsThreadLocal.get();
			if (lPixelInts == null || lPixelInts.length != pWidth * pHeight)
			{
				lPixelInts = new int[pWidth * pHeight];
				mPixelRGBIntsThreadLocal.set(lPixelInts);
			}

			// Convert RGB bytes to ARGB ints with no transparency. Flip image
			// vertically by reading the
			// rows of pixels in the byte buffer in reverse - (0,0) is at bottom left
			// in
			// OpenGL.

			int p = pWidth * pHeight * 3; // Points to first byte (red) in each row.
			int q; // Index into ByteBuffer
			int i = 0; // Index into target int[]
			final int w3 = pWidth * 3; // Number of bytes in each row

			for (int row = 0; row < pHeight; row++)
			{
				p -= w3;
				q = p;
				for (int col = 0; col < pWidth; col++)
				{
					final int iR = pByteBuffer.get(q++);
					final int iG = pByteBuffer.get(q++);
					final int iB = pByteBuffer.get(q++);

					lPixelInts[i++] = 0xFF000000 | ((iR & 0x000000FF) << 16)
														| ((iG & 0x000000FF) << 8)
														| (iB & 0x000000FF);
				}

			}

			mPixelRGBBufferQueue.add(pByteBuffer);

			BufferedImage lBufferedImage = mPixelRGBBufferedImageThreadLocal.get();

			if (lBufferedImage == null || lBufferedImage.getWidth() != pWidth
					|| lBufferedImage.getHeight() != pHeight)
			{
				lBufferedImage = new BufferedImage(	pWidth,
																						pHeight,
																						BufferedImage.TYPE_INT_ARGB);
				mPixelRGBBufferedImageThreadLocal.set(lBufferedImage);
			}

			lBufferedImage.setRGB(0,
														0,
														pWidth,
														pHeight,
														lPixelInts,
														0,
														pWidth);

			ImageIO.write(lBufferedImage, "PNG", pOutputFile);
			mImageCounter++;
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
		}
	}

}

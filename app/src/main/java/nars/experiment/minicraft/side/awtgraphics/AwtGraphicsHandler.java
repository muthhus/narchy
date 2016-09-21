package nars.experiment.minicraft.side.awtgraphics;

import nars.experiment.minicraft.side.*;
import nars.experiment.minicraft.side.Color;
import nars.experiment.minicraft.side.SpriteStore.AwtSprite;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class AwtGraphicsHandler extends GraphicsHandler {
	public final Canvas canvas = new Canvas();
	//private BufferStrategy strategy;
	public JFrame container;
	private Cursor myCursor;
	public JPanel panel;
	public BufferedImage buffer;

	@Override
	public void init(final SideScrollMinicraft game) {
		//canvas = new Canvas();
		// create a frame to contain our game
		container = new JFrame("Minicraft");
		
		try {
			ImageIcon ii = new ImageIcon(new URL("file:sprites/other/mouse.png"));
			Image im = ii.getImage();
			Toolkit tk = canvas.getToolkit();
			myCursor = tk.createCustomCursor(im, new Point(0, 0), "MyCursor");
		} catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (Exception e) {
			System.out.println("myCursor creation failed " + e);
		}
		
		// get hold the content of the frame and set up the resolution of the game
		panel = (JPanel) container.getContentPane();
		panel.setPreferredSize(new Dimension(screenWidth, screenHeight));
		panel.setLayout(null);
		panel.setCursor(myCursor);
		panel.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				Dimension d = e.getComponent().getSize();
				canvas.setBounds(0, 0, d.width, d.height);
				screenWidth = d.width;
				screenHeight = d.height;
			}
		});
		
		// setup our canvas size and put it into the content of the frame
		canvas.setBounds(0, 0, screenWidth + 10, screenHeight + 10);
		panel.add(canvas);
		
		// Tell AWT not to bother repainting our canvas since we're
		// going to do that our self in accelerated mode
		canvas.setIgnoreRepaint(true);
		
		// finally make the window visible
		container.pack();
		container.setResizable(true);
		container.setVisible(true);
		
		// add a listener to respond to the user closing the window. If they
		// do we'd like to exit the game
		// TODO: add this back in
		container.addWindowListener(new MyWindowAdapter(game));

		
		// request the focus so key events come to us
		canvas.requestFocus();
		
		// create the buffering strategy which will allow AWT
		// to manage our accelerated graphics
		//canvas.createBufferStrategy(2);
		//strategy = canvas.getBufferStrategy();

		buffer = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_RGB);
		g = (Graphics2D) buffer.getGraphics();
	}
	
	Graphics2D g;
	
	@Override
	public void startDrawing() {
		// Get hold of a graphics context for the accelerated
		// surface and blank it out
		g = (Graphics2D) buffer.getGraphics();
		g.setColor(java.awt.Color.black);
		g.fillRect(0, 0, screenWidth, screenHeight);
		
	}
	
	@Override
	public void finishDrawing() {
		g.dispose();


		Graphics cg = canvas.getGraphics();
		cg.drawImage(buffer, 0, 0, null);
		cg.dispose();;

		//strategy.show();
	}
	
	@Override
	public void setColor(Color color) {
		// TODO: Profile, this might be quite slow "new" every color change
		g.setColor(new java.awt.Color(color.R, color.G, color.B, color.A));
	}
	
	@Override
	public void fillRect(int x, int y, int width, int height) {
		g.fillRect(x, y, width, height);
	}
	
	@Override
	public void drawString(String string, int x, int y) {
		g.drawString(string, x, y);
	}
	
	@Override
	public void fillOval(int x, int y, int width, int height) {
		g.fillOval(x, y, width, height);
	}
	
	@Override
	public void drawImage(Sprite sprite, int x, int y) {
		// TODO: This is inefficient, and serialization should be done more neatly
		AwtSprite awtSprite = (AwtSprite) sprite;
		if (awtSprite.image == null) {
			AwtSprite other = (AwtSprite) SpriteStore.get().loadSprite(awtSprite.ref);
			awtSprite.image = other.image;
		}
		g.drawImage(awtSprite.image, x, y, null);
	}
	
	@Override
	public void drawImage(Sprite sprite, int x, int y, Color tint) {
		int width = sprite.getWidth();
		int height = sprite.getHeight();
		drawImage(sprite, x, y, width, height, tint);
	}
	
	@Override
	public void drawImage(Sprite sprite, int x, int y, int width, int height) {
		AwtSprite awtSprite = (AwtSprite) sprite;
		if (awtSprite.image == null) {
			AwtSprite other = (AwtSprite) SpriteStore.get().loadSprite(awtSprite.ref);
			awtSprite.image = other.image;
		}
		g.drawImage(awtSprite.image, x, y, width, height, null);
	}
	
	@Override
	public void drawImage(Sprite sprite, int x, int y, int width, int height,
			Color tint) {
		drawImage(sprite, x, y, width, height);
		java.awt.Color old = g.getColor();
		this.setColor(tint);
		this.fillRect(x, y, width, height);
		g.setColor(old);
	}

	private static class MyWindowAdapter extends WindowAdapter {
		private final SideScrollMinicraft game;

		public MyWindowAdapter(SideScrollMinicraft game) {
			this.game = game;
		}

		@Override
        public void windowClosing(WindowEvent e) {
            game.goToMainMenu();  // this saves and cleans up appropriately
            SideScrollMinicraft.quit();
        }
	}
}

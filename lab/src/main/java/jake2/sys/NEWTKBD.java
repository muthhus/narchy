package jake2.sys;

import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseEvent;
import jake2.client.Key;

final public class NEWTKBD extends KBD
{
	public static final InputListener listener = new InputListener();
	// static Cursor emptyCursor = null;
	static Window c;
	
	static int win_w2;
	static int win_h2;
	
	@Override
    public void Init() {
	}

        // Used only for the applet case
        public static void Init(Window window) {
            c = window;
            handleCreateAndConfigureNotify(window);
        }

	@Override
    public void Update() {
		// get events
		HandleEvents();
	}

	@Override
    public void Close() {
	}
	
	private void HandleEvents() 
	{
		int key;

		Jake2InputEvent event;
		while ( (event=InputListener.nextEvent()) != null ) {
	                Window eventWin = null;
	                Object source = event.ev.getSource();
	                if(source instanceof Window) {
	                    eventWin = (Window)source;
	                }
			switch(event.type) {
				case Jake2InputEvent.KeyPress:
				case Jake2InputEvent.KeyRelease:
					Do_Key_Event(XLateKeyCode((KeyEvent)event.ev), event.type == Jake2InputEvent.KeyPress);
					break;
				    
				case Jake2InputEvent.MotionNotify:
//					if (IN.ignorefirst) {
//						IN.ignorefirst = false;
//						break;
//					}
					if (IN.mouse_active) {
						mx = (((MouseEvent)event.ev).getX() - win_w2) * 2;
						my = (((MouseEvent)event.ev).getY() - win_h2) * 2;
					} else {
						mx = 0;
						my = 0;
					}
					break;
					
				case Jake2InputEvent.ButtonPress:
					key = mouseEventToKey((MouseEvent)event.ev); 
					Do_Key_Event(key, true);
					break;
 
				case Jake2InputEvent.ButtonRelease:
					key = mouseEventToKey((MouseEvent)event.ev); 
					Do_Key_Event(key, false);
					break;
					
				case Jake2InputEvent.WheelMoved:
					float dir = ((MouseEvent)event.ev).getRotation()[0];
					if (dir > 0) {
						Do_Key_Event(Key.K_MWHEELDOWN, true);
						Do_Key_Event(Key.K_MWHEELDOWN, false);
					} else {
						Do_Key_Event(Key.K_MWHEELUP, true);
						Do_Key_Event(Key.K_MWHEELUP, false);					    
					}
					break;
					 
				case Jake2InputEvent.CreateNotify :
				case Jake2InputEvent.ConfigureNotify :				    
                                        handleCreateAndConfigureNotify(eventWin);
					break;
			}
		}
            
		if (mx != 0 || my != 0) {
			// move the mouse to the window center again
			c.warpPointer(c.getWidth()/2, c.getHeight()/2);
		}		
	}

        private static void handleCreateAndConfigureNotify(Window component) {
            if(null != component) {
                win_w2 = component.getWidth() / 2;
                win_h2 = component.getHeight() / 2;
            }
        }

	// Different NEWT button numbering:
	// BUTTON1(left) BUTTON2(center) BUTTON3(right)
	// K_MOUSE1      K_MOUSE3        K_MOUSE2
	private static int mouseEventToKey(MouseEvent ev) {
	    switch (ev.getButton()) {
	    case MouseEvent.BUTTON3:
	        return Key.K_MOUSE2;
	    case MouseEvent.BUTTON2:
	        return Key.K_MOUSE3;
	    default:
	        return Key.K_MOUSE1;
	    }
	}

	private static int XLateKeyCode(KeyEvent ev) { 
		int code = ev.getKeyCode();
                int key = 0;
		switch(code) {
//	00626                 case XK_KP_Page_Up:      key = K_KP_PGUP; break;
			case KeyEvent.VK_PAGE_UP: key = Key.K_PGUP; break;
 
//	00629                 case XK_KP_Page_Down: key = K_KP_PGDN; break;
			case KeyEvent.VK_PAGE_DOWN: key = Key.K_PGDN; break;

//	00632                 case XK_KP_Home: key = K_KP_HOME; break;
			case KeyEvent.VK_HOME: key = Key.K_HOME; break;

//	00635                 case XK_KP_End:  key = K_KP_END; break;
			case KeyEvent.VK_END: key = Key.K_END; break;
 
			case KeyEvent.VK_LEFT: key = Key.K_LEFTARROW; break;
 
			case KeyEvent.VK_RIGHT: key = Key.K_RIGHTARROW; break;

			case KeyEvent.VK_DOWN: key = Key.K_DOWNARROW; break;

			case KeyEvent.VK_UP: key = Key.K_UPARROW; break; 

			case KeyEvent.VK_ESCAPE: key = Key.K_ESCAPE; break; 

			
			case KeyEvent.VK_ENTER: key = Key.K_ENTER; break; 
//	00652                 case XK_KP_Enter: key = K_KP_ENTER;     break;

			case KeyEvent.VK_TAB: key = Key.K_TAB; break; 

			case KeyEvent.VK_F1: key = Key.K_F1; break;
			case KeyEvent.VK_F2: key = Key.K_F2; break;
			case KeyEvent.VK_F3: key = Key.K_F3; break;
			case KeyEvent.VK_F4: key = Key.K_F4; break;
			case KeyEvent.VK_F5: key = Key.K_F5; break;
			case KeyEvent.VK_F6: key = Key.K_F6; break;
			case KeyEvent.VK_F7: key = Key.K_F7; break;
			case KeyEvent.VK_F8: key = Key.K_F8; break;
			case KeyEvent.VK_F9: key = Key.K_F9; break;
			case KeyEvent.VK_F10: key = Key.K_F10; break;
			case KeyEvent.VK_F11: key = Key.K_F11; break;
			case KeyEvent.VK_F12: key = Key.K_F12; break; 

			case KeyEvent.VK_BACK_SPACE: key = Key.K_BACKSPACE; break; 

			case KeyEvent.VK_DELETE: key = Key.K_DEL; break; 
//	00683                 case XK_KP_Delete: key = K_KP_DEL; break;

			case KeyEvent.VK_PAUSE: key = Key.K_PAUSE; break; 
	
			case KeyEvent.VK_SHIFT: key = Key.K_SHIFT; break; 
			case KeyEvent.VK_CONTROL: key = Key.K_CTRL; break; 
			
			case KeyEvent.VK_ALT:
			case KeyEvent.VK_ALT_GRAPH: key = Key.K_ALT; break;
 
//	00700                 case XK_KP_Begin: key = K_KP_5; break;
//	00701
			case KeyEvent.VK_INSERT: key = Key.K_INS; break;
			// toggle console for DE and US keyboards
			case KeyEvent.VK_QUOTE:
			case KeyEvent.VK_CIRCUMFLEX:
			case KeyEvent.VK_BACK_QUOTE: key='`'; break;
			case KeyEvent.VK_SPACE: key = Key.K_SPACE; break;
			
			default:
			    if( KeyEvent.VK_0 <= code && code <= KeyEvent.VK_9 ) {
			        key = code - KeyEvent.VK_0 + '0';
			    }
			    if( KeyEvent.VK_A <= code && code <= KeyEvent.VK_Z ) {
			        key = code - KeyEvent.VK_A + 'a';  
			    }
		}
		if (key > 255) key = 0;

		return key;
	}
	
	@Override
    public void Do_Key_Event(int key, boolean down) {
		Key.Event(key, down, Timer.Milliseconds());
	}
	
	public static void centerMouse() {
	    c.warpPointer(c.getWidth()/2, c.getHeight()/2);
	}
	
	@Override
    public void installGrabs()
	{
	    /*
		if (emptyCursor == null) {
			ImageIcon emptyIcon = new ImageIcon(new byte[0]);
			emptyCursor = c.getToolkit().createCustomCursor(emptyIcon.getImage(), new Point(0, 0), "emptyCursor");
		}
		c.setCursor(emptyCursor);
	     */
	    c.setPointerVisible(false);
	    centerMouse();
	}
	
	@Override
    public void uninstallGrabs()
	{
	    /*
		c.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		*/
	    c.setPointerVisible(true);
	}
}

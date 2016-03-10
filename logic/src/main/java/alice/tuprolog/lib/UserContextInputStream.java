package alice.tuprolog.lib;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import alice.tuprolog.event.ReadEvent;
import alice.tuprolog.event.ReadListener;


public class UserContextInputStream extends InputStream {
        
        private boolean avalaible;
        private boolean start;
        private int i;
        private InputStream result;
        /**
         * Changed from a single EventListener to multiple (ArrayList) ReadListeners
         *
         */
        private final ArrayList<ReadListener> readListeners;
        /***/
        
        public UserContextInputStream()
        {
        		this.avalaible = false;
                this.start = true;
                this.readListeners = new ArrayList<>();
        }

        public synchronized InputStream getInput()
        {
                while (!avalaible){
                        try {
                                wait();
                        } catch (InterruptedException e) {
                                e.printStackTrace();
                        }
                }
                avalaible = false;
                notifyAll();
                return this.result;
        }
        
        public synchronized void putInput(InputStream input)
        {
                while (avalaible){
                        try {
                                wait();
                        } catch (InterruptedException e) {
                                e.printStackTrace();
                        }
                }
                if(this.result != input)
                {
                        
                }
                this.result = input;
                avalaible = true;
                notifyAll();
        }
        
        public void setCounter(){
                start = true;
                result = null;
        }

        @Override
        public int read() throws IOException
        {
        	if(start)
        	{
        		fireReadCalled();
        		getInput();
        		start = false;
        	}

        	do {
        		try {
        			i = result.read();

        			if(i == -1)
        			{
        				fireReadCalled();
        				getInput();
        				i = result.read();
        			}
        		} catch (IOException e) {
        			e.printStackTrace();
        		}
        	} while (i < 0x20 && i >= -1);  

        	return i;                                       
        }
        
        /**
         * Changed these methods because there are more readListeners
         * from the previous version
         */
        private void fireReadCalled()
        {
                ReadEvent event = new ReadEvent(this);
               
                for(ReadListener r:readListeners){
                        r.readCalled(event);
                }
                
        }
        
        public void setReadListener(ReadListener r)
        {
                this.readListeners.add(r);
        }
        /***/
}
package nars.op.uibot;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.jfxvnc.net.rfb.render.*;
import org.jfxvnc.net.rfb.render.rect.ImageRect;

/**
 * RDP/VNC Connections
 *
 * To connect to VirtualBox:
         This will allow you to set a vbox vm to have the "console" display sent out over a VNC server.

         The first step is to globally enable the VNC extension pack like this:

         vboxmanage setproperty vrdeextpack VNC
         Then set a password to use from the VNC client (I had to do this, not setting a password prevented me from connecting even when trying what was referred to as the "default" VNC password):

         vboxmanage modifyvm vmNameGoesHere --vrdeproperty VNCPassword=mysecretpw
         Then turn vrde on for that same vm:

         vboxmanage modifyvm vmNameGoesHere --vrde on
         Then you can start the vm like this:

         vboxmanage startvm vmNameGoesHere --type headless
         This will start the vm and return you to the prompt, but with the vm starting up in the background (and it will output a message telling you the vm successfully started - NOTE that it means that is started booting successfully, NOT that it started up all the way successfully). This will leave a VNC server running on the "default" port, which I think was 5900, you can check with netstat -ltdaun | grep LISTEN to see what port(s) are listening for connections. I always set a specific/unique port for each vm so none are stepping on each other's toes with this command before starting up the vm:

         vboxmanage modifyvm vmNameGoesHere --vrdeport 5906

        https://www.virtualbox.org/manual/ch07.html#vrde
 */
public class RemoteDesktop {

    //        public static void main(String[] args) throws Exception {
//
//            new Connection("localhost", 5900);
//        }

}

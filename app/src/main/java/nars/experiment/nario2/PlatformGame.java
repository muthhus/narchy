
package nars.experiment.nario2;


import java.awt.BorderLayout;
import javax.swing.*;

public class PlatformGame extends JFrame
{
    private final GamePanel game;
    private final GameModel model;
    private final NavigationBar navBar;
    
    public PlatformGame()
    {
        super("Mario Survival 2.0");
        setSize(GameModel.screenWidth, GameModel.screenHeight);
        setLayout(new BorderLayout());
        
        navBar= new NavigationBar();
        
        model = new GameModel(navBar);
        game = new GamePanel(model);
        model.setPanel(game);
        
        navBar.setModel(model);
        
        add(navBar, BorderLayout.NORTH);
        add(game,                   BorderLayout.CENTER);
        setVisible(true);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }
    
    public static void main(String[] args) 
    {
        new PlatformGame();
    }
}

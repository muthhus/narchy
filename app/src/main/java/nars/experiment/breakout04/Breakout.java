package nars.experiment.breakout04;

import javax.swing.*;

/*
 * Created on 2007/05/05
 * 
 * �u���b�N�����̃��C���N���X
 */

public class Breakout extends JFrame {
    public Breakout() {
        setTitle("�u���b�N�ƃ{�[���̓����蔻��");
        setResizable(false);

        MainPanel panel = new MainPanel();
        getContentPane().add(panel);

        pack();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    public static void main(String[] args) {
        new Breakout();
    }
}

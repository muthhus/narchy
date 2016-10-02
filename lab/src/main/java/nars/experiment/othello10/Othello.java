package nars.experiment.othello10;/*
 * �쐬��: 2004/12/17
 *
 */

import javax.swing.*;
import java.awt.*;
/**
 * �I�Z���A�v���P�[�V�����B
 * 
 * @author mori
 *  
 */
public class Othello extends JFrame {
    public Othello() {
        // �^�C�g����ݒ�
        setTitle("��-���@");
        // �T�C�Y�ύX���ł��Ȃ�����
        setResizable(false);

        Container contentPane = getContentPane();

        // ���p�l�����쐬����
        InfoPanel infoPanel = new InfoPanel();
        contentPane.add(infoPanel, BorderLayout.NORTH);

        // ���C���p�l�����쐬���ăt���[���ɒǉ�
        MainPanel mainPanel = new MainPanel(infoPanel);
        contentPane.add(mainPanel, BorderLayout.CENTER);

        // �p�l���T�C�Y�ɍ��킹�ăt���[���T�C�Y�������ݒ�
        pack();
    }

    public static void main(String[] args) {
        Othello frame = new Othello();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
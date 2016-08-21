package nars.experiment.breakout04;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

/*
 * Created on 2007/05/05
 * 
 * �u���b�N�����̃Q�[����ʗp�p�l��
 */

public class MainPanel extends JPanel implements Runnable, MouseMotionListener {
    public static final int WIDTH = 360;
    public static final int HEIGHT = 480;

    // �u���b�N�̍s��
    private static final int NUM_BLOCK_ROW = 10;
    // �u���b�N�̗�
    private static final int NUM_BLOCK_COL = 7;
    // �u���b�N��
    private static final int NUM_BLOCK = NUM_BLOCK_ROW * NUM_BLOCK_COL;

    private final Racket racket; // ���P�b�g
    private final Ball ball; // �{�[��
    private final Block[] block; // �u���b�N

    private final Thread gameLoop; // �Q�[�����[�v

    public MainPanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        addMouseMotionListener(this);

        racket = new Racket();
        ball = new Ball();
        block = new Block[NUM_BLOCK];

        // �u���b�N����ׂ�
        for (int i = 0; i < NUM_BLOCK_ROW; i++) {
            for (int j = 0; j < NUM_BLOCK_COL; j++) {
                int x = j * Block.WIDTH + Block.WIDTH;
                int y = i * Block.HEIGHT + Block.HEIGHT;
                block[i * NUM_BLOCK_COL + j] = new Block(x, y);
            }
        }

        gameLoop = new Thread(this);
        gameLoop.start();
    }

    /**
     * �Q�[�����[�v
     * 
     */
    @Override
    public void run() {
        while (true) {
            // �{�[���̈ړ�
            ball.move();

            // �������P�b�g�ƏՓ˂�����{�[�����o�E���h
            if (racket.collideWith(ball)) {
                ball.boundY();
            }

            // �u���b�N�ƃ{�[���̏Փˏ���
            for (int i = 0; i < NUM_BLOCK; i++) {
                // ���łɏ����Ă���u���b�N�͖���
                if (block[i].isDeleted())
                    continue;
                // �u���b�N�̓��������ʒu���v�Z
                int collidePos = block[i].collideWith(ball);
                if (collidePos != Block.NO_COLLISION) { // �u���b�N�ɓ������Ă�����
                    block[i].delete();
                    // �{�[���̓��������ʒu����{�[���̔��˕������v�Z
                    switch (collidePos) {
                        case Block.DOWN :
                        case Block.UP :
                            ball.boundY();
                            break;
                        case Block.LEFT :
                        case Block.RIGHT :
                            ball.boundX();
                            break;
                        case Block.UP_LEFT :
                        case Block.UP_RIGHT :
                        case Block.DOWN_LEFT :
                        case Block.DOWN_RIGHT :
                            ball.boundXY();
                            break;
                    }
                    break; // 1��ɉ󂹂�u���b�N��1��
                }
            }

            repaint();

            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // �w�i
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        racket.draw(g); // ���P�b�g
        ball.draw(g); // �{�[��

        // �u���b�N
        for (int i = 0; i < NUM_BLOCK; i++) {
            if (!block[i].isDeleted()) {
                block[i].draw(g);
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        int x = e.getX(); // �}�E�X��X���W
        racket.move(x); // ���P�b�g���ړ�
        repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }
}

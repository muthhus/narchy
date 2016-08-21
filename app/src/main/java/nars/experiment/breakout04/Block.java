package nars.experiment.breakout04;

import java.awt.*;

/*
 * Created on 2007/05/05
 * 
 * �u���b�N�N���X
 */

public class Block {
    public static final int WIDTH = 40;
    public static final int HEIGHT = 16;

    // �{�[���̓�����ʒu
    public static final int NO_COLLISION = 0; // ���Փ�
    public static final int DOWN = 1;
    public static final int LEFT = 2;
    public static final int RIGHT = 3;
    public static final int UP = 4;
    public static final int DOWN_LEFT = 5;
    public static final int DOWN_RIGHT = 6;
    public static final int UP_LEFT = 7;
    public static final int UP_RIGHT = 8;

    // �ʒu�i������̍��W�j
    private final int x;
    private final int y;

    // �{�[�����������ď����ꂽ��
    private boolean isDeleted;

    public Block(int x, int y) {
        this.x = x;
        this.y = y;
        isDeleted = false;
    }

    /**
     * �u���b�N��`��
     * 
     * @param g
     */
    public void draw(Graphics g) {
        g.setColor(Color.CYAN);
        g.fillRect(x, y, WIDTH, HEIGHT);

        // �g����`��
        g.setColor(Color.BLACK);
        g.drawRect(x, y, WIDTH, HEIGHT);
    }

    /**
     * �{�[���ƏՓ˂�����
     * 
     * @param ball �{�[��
     * @return �Փˈʒu
     */
    public int collideWith(Ball ball) {
        Rectangle blockRect = new Rectangle(x, y, WIDTH, HEIGHT);

        int ballX = ball.getX();
        int ballY = ball.getY();
        int ballSize = ball.getSize();
        if (blockRect.contains(ballX, ballY)
                && blockRect.contains(ballX + ballSize, ballY)) {
            // �u���b�N�̉�����Փˁ��{�[���̍���E�E��̓_���u���b�N��
            return DOWN;
        } else if (blockRect.contains(ballX + ballSize, ballY)
                && blockRect.contains(ballX + ballSize, ballY + ballSize)) {
            // �u���b�N�̍�����Փˁ��{�[���̉E��E�E���̓_���u���b�N��
            return LEFT;
        } else if (blockRect.contains(ballX, ballY)
                && blockRect.contains(ballX, ballY + ballSize)) {
            // �u���b�N�̉E����Փˁ��{�[���̍���E�����̓_���u���b�N��
            return RIGHT;
        } else if (blockRect.contains(ballX, ballY + ballSize)
                && blockRect.contains(ballX + ballSize, ballY + ballSize)) {
            // �u���b�N�̏ォ��Փˁ��{�[���̍����E�E���̓_���u���b�N��
            return UP;
        } else if (blockRect.contains(ballX + ballSize, ballY)) {
            // �u���b�N�̍�������Փˁ��{�[���̉E��̓_���u���b�N��
            return DOWN_LEFT;
        } else if (blockRect.contains(ballX, ballY)) {
            // �u���b�N�̉E������Փˁ��{�[���̍���̓_���u���b�N��
            return DOWN_RIGHT;
        } else if (blockRect.contains(ballX + ballSize, ballY + ballSize)) {
            // �u���b�N�̍��ォ��Փˁ��{�[���̉E���̓_���u���b�N��
            return UP_LEFT;
        } else if (blockRect.contains(ballX, ballY + ballSize)) {
            // �u���b�N�̉E�ォ��Փˁ��{�[���̍����̓_���u���b�N��
            return UP_RIGHT;
        }

        return NO_COLLISION;
    }

    /**
     * �u���b�N������
     * 
     */
    public void delete() {
        // TODO: �����Ńu���b�N��������ʉ�
        // TODO: �����Ŕh��ȃA�N�V����

        isDeleted = true;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isDeleted() {
        return isDeleted;
    }
}

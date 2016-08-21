package nars.experiment.othello10;/*
 * Created on 2004/12/22
 *
 */
import java.awt.*;
/**
 * �Ֆʂ�1��߂����߂̏����܂Ƃ߂��N���X�B
 * @author mori
 *
 */
public class Undo {
    // �΂�łꏊ
    public int x;
    public int y;
    // �Ђ�����Ԃ����΂̐�
    public int count;
    // �Ђ�����Ԃ����΂̏ꏊ
    public Point[] pos;
    
    public Undo(int x, int y) {
        this.x = x;
        this.y = y;
        count = 0;
        pos = new Point[64];
    }
}

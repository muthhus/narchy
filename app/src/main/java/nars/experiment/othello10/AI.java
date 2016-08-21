package nars.experiment.othello10;/*
 * Created on 2004/12/22
 *
 */

/**
 * �I�Z����AI�B
 * 
 * @author mori
 *  
 */
public class AI {
    // �[�ǂ݂��郌�x���i�傫���l���Ƃ��̂��������Ԃ��������Ă��܂��̂Œ��Ӂj
    private static final int SEARCH_LEVEL = 7;
    // ���C���p�l���ւ̎Q��
    private MainPanel panel;
    // �Ֆʂ̊e�ꏊ�̉��l
    private static final int valueOfPlace[][] = {
            {120, -20, 20,  5,  5, 20, -20, 120},
            {-20, -40, -5, -5, -5, -5, -40, -20},
            { 20,  -5, 15,  3,  3, 15,  -5,  20},
            {  5,  -5,  3,  3,  3,  3,  -5,   5},
            {  5,  -5,  3,  3,  3,  3,  -5,   5},
            { 20,  -5, 15,  3,  3, 15,  -5,  20},
            {-20, -40, -5, -5, -5, -5, -40, -20},
            {120, -20, 20,  5,  5, 20, -20, 120}
    };
    
    /**
     * �R���X�g���N�^�B���C���p�l���ւ̎Q�Ƃ�ۑ��B
     * 
     * @param panel ���C���p�l���ւ̎Q�ƁB
     */
    public AI(MainPanel panel) {
        this.panel = panel;
    }

    /**
     * �R���s���[�^�̎�����肷��B
     *  
     */
    public void compute() {
        // ��-���@�Ő΂�łꏊ�����߂�
        // �߂��Ă���l�� bestX+bestY*MASU
        int temp = alphaBeta(true, SEARCH_LEVEL, Integer.MIN_VALUE, Integer.MAX_VALUE);
        
        // �ꏊ�����߂�
        int x = temp % MainPanel.MASU;
        int y = temp / MainPanel.MASU;

        // �ł����ꏊ�A�Ђ�����Ԃ����΂̈ʒu���L�^
        Undo undo = new Undo(x, y);
        // ���̏ꏊ�Ɏ��ۂɐ΂�ł�
        panel.putDownStone(x, y, false);
        // ���ۂɂЂ�����Ԃ�
        panel.reverse(undo, false);
        // �I�����������ׂ�
        if (panel.endGame()) return;
        // ��Ԃ�ς���
        panel.nextTurn();
        // �v���C���[���p�X�̏ꍇ�͂������
        if (panel.countCanPutDownStone() == 0) {
            System.out.println("Player PASS!");
            panel.nextTurn();
            compute();
        }
    }

    /**
     * Min-Max�@�B�őP���T������B�łꏊ��T�������Ŏ��ۂɂ͑ł��Ȃ��B
     * 
     * @param flag AI�̎�Ԃ̂Ƃ�true�A�v���C���[�̎�Ԃ̂Ƃ�false�B
     * @param level ��ǂ݂̎萔�B
     * @return �q�m�[�h�ł͔Ֆʂ̕]���l�B���[�g�m�[�h�ł͍ő�]���l�����ꏊ�ibestX + bestY * MAS�j�B
     */
    private int minMax(boolean flag, int level) {
        // �m�[�h�̕]���l
        int value;
        // �q�m�[�h����`�d���Ă����]���l
        int childValue;
        // Min-Max�@�ŋ��߂��ő�̕]���l�����ꏊ
        int bestX = 0;
        int bestY = 0;

        // �Q�[���؂̖��[�ł͔Ֆʕ]��
        // ���̑��̃m�[�h��MIN or MAX�œ`�d����
        if (level == 0) {
            return valueBoard();
        }
        
        if (flag) {
            // AI�̎�Ԃł͍ő�̕]���l�����������̂ōŏ��ɍŏ��l���Z�b�g���Ă���
            value = Integer.MIN_VALUE;
        } else {
            // �v���C���[�̎�Ԃł͍ŏ��̕]���l�����������̂ōŏ��ɍő�l���Z�b�g���Ă���
            value = Integer.MAX_VALUE;
        }
        
        // �����p�X�̏ꍇ�͂��̂܂ܔՖʕ]���l��Ԃ�
        if (panel.countCanPutDownStone() == 0) {
            return valueBoard();
        }
        
        // �łĂ�Ƃ���͂��ׂĎ����i���������Ŏ��ۂɂ͑ł��Ȃ��j
        for (int y = 0; y < MainPanel.MASU; y++) {
            for (int x = 0; x < MainPanel.MASU; x++) {
                if (panel.canPutDown(x, y)) {
                    Undo undo = new Undo(x, y);
                    // �����ɑł��Ă݂�i�Ֆʕ`��͂��Ȃ��̂�true�w��j
                    panel.putDownStone(x, y, true);
                    // �Ђ�����Ԃ��i�Ֆʕ`��͂��Ȃ��̂�true�w��j
                    panel.reverse(undo, true);
                    // ��Ԃ�ς���
                    panel.nextTurn();
                    // �q�m�[�h�̕]���l���v�Z�i�ċA�j
                    // ���x�͑���̔ԂȂ̂�flag���t�]����
                    childValue = minMax(!flag, level - 1);
                    // �q�m�[�h�Ƃ��̃m�[�h�̕]���l���r����
                    if (flag) {
                        // AI�̃m�[�h�Ȃ�q�m�[�h�̒��ōő�̕]���l��I��
                        if (childValue > value) {
                            value = childValue;
                            bestX = x;
                            bestY = y;
                        }
                    } else {
                        // �v���C���[�̃m�[�h�Ȃ�q�m�[�h�̒��ōŏ��̕]���l��I��
                        if (childValue < value) {
                            value = childValue;
                            bestX = x;
                            bestY = y;
                        }
                    }
                    // �łO�ɖ߂�
                    panel.undoBoard(undo);
                }
            }
        }

        if (level == SEARCH_LEVEL) {
            // ���[�g�m�[�h�Ȃ�ő�]���l�����ꏊ��Ԃ�
            return bestX + bestY * MainPanel.MASU;
        } else {
            // �q�m�[�h�Ȃ�m�[�h�̕]���l��Ԃ�
            return value;
        }
    }

    /**
     * ��-���@�B�őP���T������B�łꏊ��T�������Ŏ��ۂɂ͑ł��Ȃ��B
     * 
     * @param flag AI�̎�Ԃ̂Ƃ�true�A�v���C���[�̎�Ԃ̂Ƃ�false�B
     * @param level ��ǂ݂̎萔�B
     * @param alpha ���l�B���̃m�[�h�̕]���l�͕K�����l�ȏ�ƂȂ�B
     * @param beta ���l�B���̃m�[�h�̕]���l�͕K�����l�ȉ��ƂȂ�B
     * @return �q�m�[�h�ł͔Ֆʂ̕]���l�B���[�g�m�[�h�ł͍ő�]���l�����ꏊ�ibestX + bestY * MAS�j�B
     */
    private int alphaBeta(boolean flag, int level, int alpha, int beta) {
        // �m�[�h�̕]���l
        int value;
        // �q�m�[�h����`�d���Ă����]���l
        int childValue;
        // Min-Max�@�ŋ��߂��ő�̕]���l�����ꏊ
        int bestX = 0;
        int bestY = 0;

        // �Q�[���؂̖��[�ł͔Ֆʕ]��
        // ���̑��̃m�[�h��MIN or MAX�œ`�d����
        if (level == 0) {
            return valueBoard();
        }
        
        if (flag) {
            // AI�̎�Ԃł͍ő�̕]���l�����������̂ōŏ��ɍŏ��l���Z�b�g���Ă���
            value = Integer.MIN_VALUE;
        } else {
            // �v���C���[�̎�Ԃł͍ŏ��̕]���l�����������̂ōŏ��ɍő�l���Z�b�g���Ă���
            value = Integer.MAX_VALUE;
        }
        
        // �����p�X�̏ꍇ�͂��̂܂ܔՖʕ]���l��Ԃ�
        if (panel.countCanPutDownStone() == 0) {
            return valueBoard();
        }
        
        // �łĂ�Ƃ���͂��ׂĎ����i���������Ŏ��ۂɂ͑ł��Ȃ��j
        for (int y = 0; y < MainPanel.MASU; y++) {
            for (int x = 0; x < MainPanel.MASU; x++) {
                if (panel.canPutDown(x, y)) {
                    Undo undo = new Undo(x, y);
                    // �����ɑł��Ă݂�i�Ֆʕ`��͂��Ȃ��̂�true�w��j
                    panel.putDownStone(x, y, true);
                    // �Ђ�����Ԃ��i�Ֆʕ`��͂��Ȃ��̂�true�w��j
                    panel.reverse(undo, true);
                    // ��Ԃ�ς���
                    panel.nextTurn();
                    // �q�m�[�h�̕]���l���v�Z�i�ċA�j
                    // ���x�͑���̔ԂȂ̂�flag���t�]����
                    childValue = alphaBeta(!flag, level - 1, alpha, beta);
                    // �q�m�[�h�Ƃ��̃m�[�h�̕]���l���r����
                    if (flag) {
                        // AI�̃m�[�h�Ȃ�q�m�[�h�̒��ōő�̕]���l��I��
                        if (childValue > value) {
                            value = childValue;
                            // ���l���X�V
                            alpha = value;
                            bestX = x;
                            bestY = y;
                        }
                        // ���̃m�[�h�̌��݂�value���󂯌p�������l���傫��������
                        // ���̎}���I�΂�邱�Ƃ͂Ȃ��̂ł���ȏ�]�����Ȃ�
                        // = for���[�v���ʂ���
                        if (value > beta) {  // ���J�b�g
//                            System.out.println("���J�b�g");
                            // �łO�ɖ߂�
                            panel.undoBoard(undo);
                            return value;
                        }
                    } else {
                        // �v���C���[�̃m�[�h�Ȃ�q�m�[�h�̒��ōŏ��̕]���l��I��
                        if (childValue < value) {
                            value = childValue;
                            // ���l���X�V
                            beta = value;
                            bestX = x;
                            bestY = y;
                        }
                        // ���̃m�[�h��value���e����󂯌p�������l��菬����������
                        // ���̎}���I�΂�邱�Ƃ͂Ȃ��̂ł���ȏ�]�����Ȃ�
                        // = for���[�v���ʂ���
                        if (value < alpha) {  // ���J�b�g
//                            System.out.println("���J�b�g");
                            // �łO�ɖ߂�
                            panel.undoBoard(undo);
                            return value;
                        }
                    }
                    // �łO�ɖ߂�
                    panel.undoBoard(undo);
                }
            }
        }

        if (level == SEARCH_LEVEL) {
            // ���[�g�m�[�h�Ȃ�ő�]���l�����ꏊ��Ԃ�
            return bestX + bestY * MainPanel.MASU;
        } else {
            // �q�m�[�h�Ȃ�m�[�h�̕]���l��Ԃ�
            return value;
        }
    }
    
    /**
     * �]���֐��B�Ֆʂ�]�����ĕ]���l��Ԃ��B�Ֆʂ̏ꏊ�̉��l�����ɂ���B
     * 
     * @return �Ֆʂ̕]���l�B
     */
    private int valueBoard() {
        int value = 0;
        
        for (int y = 0; y < MainPanel.MASU; y++) {
            for (int x = 0; x < MainPanel.MASU; x++) {
                // �u���ꂽ�΂Ƃ��̏ꏊ�̉��l�������đ����Ă���
                value += panel.getBoard(x, y) * valueOfPlace[y][x];
            }
        }

        // ���΁iAI�j���L���ȂƂ��͕��ɂȂ�̂ŕ����𔽓]����
        return -value;
    }
}
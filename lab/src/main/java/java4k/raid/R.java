//package java4k.raid;
//
//import java.applet.Applet;
//import java.awt.*;
//import java.awt.image.BufferedImage;
//import java.awt.image.DataBufferInt;
//import java.util.Random;
//
///**
// * Raid On Java 4K, Felix Unger 2014<br>
// * Felix.Unger.Germany@java4k.com<br>
// * Java4k 2014 contest http://java4k.com<br>
// * Thanks to Gef for compression tips.<br>
// * <br>
// * <b>The Story:</b<br>
// * After the evil empire has attacked your home (the java islands) and builds
// * evil factories, it is time for revenge.<br>
// * <br>
// * You come home on an aircraft carrier equipped with a helicopter. Your job is
// * to destroy the factories (grey squares) on the java islands. To accomplish
// * your mission, you must bomb each factory with eight bombs (press B-key). But
// * beware, the higher your progress the harder is the defense. In other words,
// * there are more enemy jets with more hitpoints and the defense towers can
// * shoot a longer distance.<br>
// * <br>
// * The warzone consists of 8 islands. Four Islands are on the right side from
// * your carrier and four on the left. Every island has two factories. If you
// * destroy a factory, you'll reach the next level. Enemy jets respawn
// * immediately. In total the game has 16 levels.<br>
// * <br>
// * Your helicopter can only carry 9 bombs. Thus, you have to reload after an
// * attack. To reload, you'll have to land on the aircraft carrier (press space).
// * Whilst landed, your helicopter will be reloaded and repaired. So don't start
// * too early. Luckily your helicopter is secured as long as it stays on the
// * carrier.<br>
// * <br>
// * The carrier moves from south to north to avoid enemy fire. So you have to
// * search the carrier in the warzone.<br>
// * <br>
// * Please note that the warzone has no border. If you fly long enough in one
// * direction, you will return to your starting point. Instructions/Input:<br>
// * <br>
// * <b>Instructions/Input:</b<br>
// * <ul>
// * <li>Enter to start</li<br>
// * <li>Space = Land on/start from aircraft carrier.
// * <ul>
// * <li>For landing, the helicopter needs a direct contact with the carrier.</li>
// * </ul>
// * <li>Cursorkeys = Controlling the helicopter</li<br>
// * <li>F = fires the machine gun
// * <li>B = drops a Bomb<br>
// * <ul>
// * <li>For bombing a factory, you have to stay above a factory and press B.
// * You'll see a great explosion; you have a direct hit on the target. Otherwise
// * there is only a small explosion. In this case, you'll need to aim better.
// * </li</ul>
// * </li</li</li</ul>
// *
// * @author Felix Unger
// */
//public class R extends Applet implements Runnable {
//
//    public void start() {
//        new Thread(this).start();
//    }
//
//
//    private final static int SCREEN_WIDTH = 320;
//    private final static int SCREEN_HEIGHT = 256;
//
//    private final static int MAP_WIDTH = 2048;
//    private final static int MAP_HEIGHT = 2048;
//    private final static int MAP_CENTER_X = 1024;
//    private final static int MAP_CENTER_Y = 1024;
//
//    private final static int HEATMAP_WIDTH = 512;
//    private final static int HEATMAP_HEIGHT = 512;
//    private final static int HEATMAP_LENGTH = HEATMAP_WIDTH * HEATMAP_HEIGHT;
//    private final static int HEATMAP_COOLDOWN = 10;
//
//    // Factory datastructure
//    private final static int FAC_NUMBER = 16;
//    private final static int FAC_DATA_LENGTH = 4;
//    private final static int FAC_HEALTH = 0;
//    private final static int FAC_XPOS = 1;
//    private final static int FAC_YPOS = 2;
//    private final static int FAC_HEALTH_START_VALUE = 12000;
//    private final static int FAC_BOMB_DAMAGE = 1500;
//
//    // Enemy.
//    private final static int ENM_NUMBER = 96;
//    private final static int ENM_INT_DATA_LENGTH = 7;
//    // 0 = dead
//
//    private final static int ENM_INT_COLOR = 1;
//    // 0 = can shoot
//    private final static int ENM_INT_FIRE_REPEAT = 2;
//    private final static int ENM_INT_X_POS = 3;
//    private final static int ENM_INT_Y_POS = 4;
//    // 0 = hang around
//    // 1 = attack player
//    private final static int ENM_INT_ATTACK_STATUS = 5;
//    private final static int ENM_INT_CHANGE_DIRECTION = 6;
//    private final static int ENM_DBL_DATA_LENGTH = 6;
//
//    private final static int ENM_DBL_X_POS = 1;
//    private final static int ENM_DBL_Y_POS = 2;
//    private final static int ENM_DBL_X_SPEED = 3;
//    private final static int ENM_DBL_Y_SPEED = 4;
//    private final static int ENM_DBL_NEW_DIRECTION = 5;
//
//    // defense tower
//    private final static int TOWER_NUMBER = 16;
//    private final static int TOWER_INIT_HEALTH = 10;
//    private final static int TOWER_DATA_LENGTH = 4;
//
//    private final static int TOWER_XPOS = 1;
//    private final static int TOWER_YPOS = 2;
//    private final static int TOWER_FIRE_REPEAT = 3;
//
//    private final static int SHOOT_NUMBER = 10 + ENM_NUMBER + TOWER_NUMBER;
//    private final static int SHOOT_DATA_LENGTH = 5;
//
//    private final static int SHOOT_X_POS = 1;
//    private final static int SHOOT_Y_POS = 2;
//    private final static int SHOOT_X_SPEED = 3;
//    private final static int SHOOT_Y_SPEED = 4;
//
//    private final static int COPTER_INIT_HEALTH = 10;
//    private final static int COPTER_INIT_BOMBS = 9;
//    private final static double COPTER_ACCL = 0.1f;
//    private final static double ROTOR_ACCL = 0.01f;
//
//    private final static int CARRIER_X = 1004;
//
//    private final static int WATER_COL = 0xff000088;
//
//    private final int KEY_UP = 1004;
//    private final int KEY_DOWN = 1005;
//    private final int KEY_LEFT = 1006;
//    private final int KEY_RIGHT = 1007;
//    private final int KEY_F = 102;
//    private final int KEY_B = 98;
//    private final int KEY_ENTER = 10;
//    private final int KEY_SPACE = 32;
//
//
//    // *** Globals ***
//    int scrX;
//    int scrY;
//    int iCopterX;
//    int iCopterY;
//    int screenTopLeftX;
//    int screenTopLeftY;
//    int screenTopLeftXOld;
//    int screenTopLeftYOld;
//    int[] screenPixels;
//    Random random;
//
//    // Heatmap
//    int[] heatMap1;
//
//    // Boolean array for keyboard
//    final boolean[] keyboardMap = new boolean[32767];
//
//    // *** Globals END ***
//
//    public void run() {
//        // *** Locals ***
//
//
//        Graphics frameGraphics;
//        Graphics2D screenImageG2D;
//        BufferedImage screenImage;
//
//        double[] towerDblData;
//        int[] towerData;
//
//        // bullet data
//        // first 10 slots for the player.
//        // rest for the jets. Every jet has one slot.
//        double[] shootData;
//
//        int[] mapData;
//
//        int[] fabData;
//
//        // Integer arrays for gfx
//        int[] copterGfxData;
//        int[] rotorGfxData;
//        int[] jetGfxData;
//        int[] jetIntData;
//        double[] jetDoubleData;
//
//        // Heatmap vars
//        BufferedImage heatmapImage;
//        int[] heatmapPixels;
//        int[] heatMap2;
//
//        long lastTime;
//
//        int bombWaitCounter;
//
//        double dCopterX;
//        double dCopterY;
//        // collision square
//        int coptKolx1;
//        int coptKoly1;
//        int coptKolx2;
//        int coptKoly2;
//        int copterLoadedBombs;
//        double copterDirection;
//        double rotorCounter;
//        int copterHealth;
//        double rotorSpeed;
//        double copterSpeedX;
//        double copterSpeedY;
//        boolean copterIsLanding;
//
//        int carrierY;
//
//        // control vars
//        boolean gameStarted;
//        int spaceKey;
//        int shootCounter;
//        int level;
//        int lastLevel;
//        String winText;
//
//        // *** Locals end ***
//
//        // *** Init vars ***
//        random = new Random();
//
//        frameGraphics = getGraphics();
//        heatmapImage = new BufferedImage(SCREEN_WIDTH, SCREEN_HEIGHT, BufferedImage.TYPE_INT_ARGB);
//        heatmapPixels = ((DataBufferInt) heatmapImage.getRaster().getDataBuffer()).getData();
//
//        screenImage = new BufferedImage(SCREEN_WIDTH, SCREEN_HEIGHT, BufferedImage.TYPE_INT_RGB);
//        screenPixels = ((DataBufferInt) screenImage.getRaster().getDataBuffer()).getData();
//        screenImageG2D = (Graphics2D) screenImage.getGraphics();
//
//        shootData = new double[SHOOT_DATA_LENGTH * SHOOT_NUMBER];
//        jetDoubleData = new double[ENM_DBL_DATA_LENGTH * ENM_NUMBER];
//        towerDblData = new double[TOWER_NUMBER];
//
//        heatMap1 = new int[HEATMAP_WIDTH * HEATMAP_HEIGHT];
//        towerData = new int[TOWER_NUMBER * TOWER_DATA_LENGTH];
//        mapData = new int[MAP_WIDTH * MAP_HEIGHT];
//        fabData = new int[FAC_NUMBER * FAC_DATA_LENGTH];
//        copterGfxData = new int[1024 * 16];
//        rotorGfxData = new int[1024 * 16];
//        jetGfxData = new int[1024 * 16];
//        jetIntData = new int[ENM_INT_DATA_LENGTH * ENM_NUMBER];
//        heatMap2 = new int[HEATMAP_WIDTH * HEATMAP_HEIGHT];
//
//        rotorSpeed = 0;
//        rotorCounter = 0;
//
//        bombWaitCounter = 1;
//
//        winText = "";
//
//        lastTime = System.nanoTime();
//        lastLevel = 0;
//
//        // *** init vars END
//
//        // Label for restart
//        restart:
//        while (true) {
//
//            carrierY = 20;
//            dCopterX = CARRIER_X + 20;
//            dCopterY = carrierY + 40;
//            shootCounter = 0;
//            spaceKey = 0;
//            screenTopLeftX = 0;
//            screenTopLeftY = 0;
//            copterHealth = COPTER_INIT_HEALTH;
//            copterLoadedBombs = COPTER_INIT_BOMBS;
//            copterIsLanding = true;
//            level = 1;
//
//            copterDirection = 0;
//            copterSpeedX = 0;
//            copterSpeedY = 0.1;
//
//            gameStarted = false;
//
//            char[] copterBase = ("................................" + //
//                    "...............XX.X............." + //
//                    "...............XXXX............." + //
//                    "...............XX.X............." + //
//                    "...............XX..............." + //
//                    "...............XX..............." + //
//                    "...............XX..............." + //
//                    "...............XX..............." + //
//                    "..............XXXX.............." + //
//                    "..............XXXX.............." + //
//                    "..............XXXX.............." + //
//                    "..............XXXX.............." + //
//                    ".............XXXXXX............." + //
//                    ".............XXXXXX............." + //
//                    ".............XXXXXX............." + //
//                    ".........X...XXXXXX...X........." + //
//                    ".........XXXXXXOOXXXXXX........." + //
//                    ".........XXXXXXOOXXXXXX........." + //
//                    ".........X...XXXXXX...X........." + //
//                    ".............XXOOXX............." + //
//                    ".............XXOOXX............." + //
//                    ".............XXOOXX............." + //
//                    "..............XXXX.............." + //
//                    "..............XXXX.............." + //
//                    "...............XX..............." + //
//                    "...............XX..............." + //
//                    "................................" + //
//                    "................................" + //
//                    "................................" + //
//                    "................................" + //
//                    "................................" + //
//                    "................................").toCharArray();
//
//            char[] jetBaseData = ("................................" + //
//                    "................................" + //
//                    "................................" + //
//                    "...........XXXXXXXXX............" + //
//                    "...........XXXXXXXXX............" + //
//                    ".............XXXXX.............." + //
//                    "..............XXX..............." + //
//                    "..............XXX..............." + //
//                    ".......XXXXXXXXXXXXXXXXX........" + //
//                    ".......XXXXXXXXXXXXXXXXX........" + //
//                    "........XXXXXXXXXXXXXXX........." + //
//                    "........XXXXXXXXXXXXXXX........." + //
//                    ".........XXXXXXXXXXXXX.........." + //
//                    "..........XXXXXXXXXXX..........." + //
//                    "............XXXXXXX............." + //
//                    ".............XXXXX.............." + //
//                    "..............XXX..............." + //
//                    "..............XXX..............." + //
//                    "..............XXX..............." + //
//                    "..............XXX..............." + //
//                    "..............XXX..............." + //
//                    "..............XXX..............." + //
//                    "..............XXX..............." + //
//                    "..............XXX..............." + //
//                    "..............XXX..............." + //
//                    "..............XXX..............." + //
//                    "...............X................" + //
//                    "................................" + //
//                    "................................" + //
//                    "................................" + //
//                    "................................" + //
//                    "................................").toCharArray();
//
//            // *** INIT ***
//
//            // precalc rotor.
//            int[] rotorbasedata = new int[1024];
//            for (int y3 = 0; y3 < 32; y3++) {
//                for (int x4 = 0; x4 < 32; x4++) {
//                    rotorbasedata[x4 + y3 * 32] = 0xff00ff;
//                    if ((x4 == 16 || y3 == 15) & (x4 > 4) & (x4 < 27) & (y3 > 4) & (y3 < 27)) {
//                        rotorbasedata[x4 + y3 * 32] = 0xff0000;
//                    }
//                }
//            }
//            //rotate gfx
//            int targetindex = 0;
//            int pixcolcopter;
//            int pixcoljet;
//            int pixcolrotor;
//            for (int d = 0; d < 16;
//                 d++) {
//                double dir = d * Math.PI * 2 * 16.0f;
//                double cos = Math.sin(Math.PI * 2 + dir);
//                double sin = Math.sin(dir);
//                for (int y1 = 0; y1 < 32; y1++) {
//                    for (int x1 = 0; x1 < 32;
//                         x1++) {
//                        int sourcex = (int) (cos * (x1 - 16) + sin * (y1 - 16) + 16);
//                        int sourcey = (int) (cos * (y1 - 16) - sin * (x1 - 16) + 16);
//                        pixcolcopter = 0xFF00FF;
//                        pixcoljet = 0xFF00FF;
//                        pixcolrotor = 0xFF00FF;
//                        if (sourcex <
//                                31 && sourcey < 31) {
//                            int sourceindex = "(sourceX" + sourcey * 32) &1023;
//                            helicopter if (copterbase[sourceindex] = "=" 'x'){
//                                pixcolcopter = 0xFFFFFF;
//                            }
//                            if (copterbase[sourceindex] = "=" 'o'){
//                                pixcolcopter = 0x888888;
//                            }
//                            rotor pixcolrotor = "rotorBaseData[sourceIndex]; jet if (jetbasedata[sourceindex] = " = "
//                            'x'){
//                                pixcoljet = 0xFFFFFF;
//                            }
//                        } coptergfxdata[targetindex] = "pixColCopter; rotorgfxdata[targetindex] = " pixColRotor;
//                        jetgfxdata[targetindex++] = "pixColJet;
//                    }
//                }
//            } ******sprites end *********** ***generate map bufferedimage inselbi = "new"
//            bufferedimage(1024, 512, bufferedimage.type_int_rgb);
//            graphics2d inselg2d = "(Graphics2D)" inselbi.getgraphics();
//            int[] inseldatabuffer = "((DataBufferInt)" inselbi.getraster().getdatabuffer()).getdata();
//            random.setseed(7);
//            int fabcounter = 0;
//            for (int mapx = 0; mapx < 2047;
//                 mapx += "1024)" {
//                for (int mapy = 0; mapy < 2047;
//                     mapy += "512)" {
//                    generate island:first fill with water inselg2d.setcolor(new color(water_col));
//                    inselg2d.fillrect(0, 0, 1024, 512);
//                    now the land. 500 50 x50 circles inselg2d.setcolor(new color(0x444444));
//                    for (int i2 = 0; i2 < 500;
//                         i2++) {
//                        inselg2d.filloval((312 + random.nextint(400 - 25)), (100 + random.nextint(312 - 25)), 50, 50);
//                    }
//                    every island has 2 factories and 2 defense towers for (int i1 = 0; i1 < 2;
//                                                                           i1++) {
//                        fabdata[fac_xpos + fabcounter * fac_data_length] = "mapX" + 340 + random.nextint(340);
//                        fabdata[fac_ypos + fabcounter * fac_data_length] = "mapY" + 128 + random.nextint(250);
//                        fabdata[fac_health + fabcounter * fac_data_length] = "FAC_HEALTH_START_VALUE;
//                        towerdata[tower_xpos + fabcounter * tower_data_length] = "mapX" + 340 + random.nextint(340);
//                        towerdata[tower_ypos + fabcounter * tower_data_length] = "mapY" + 128 + random.nextint(250);
//                        towerdata[fabcounter * tower_data_length] = "TOWER_INIT_HEALTH; towerdbldata[fabcounter] = 0;
//                        fabcounter++;
//                    } put the island in the water int inselindex = 0;
//                    for (int y4 = "mapY; y4 <mapy + 512;
//                         y4++) {
//                        for (int x3 = "mapX; x3 <mapx + 1024;
//                             x3++) {
//                            if (inseldatabuffer[inselindex] = "=" 0xff444444){
//                                int col1 = "random.nextInt(20)" + 50;
//                                inseldatabuffer[inselindex] = "col1" | (col1 > < 8) |(col1 > < 16);
//                            } mapdata[x3 + y4 * map_width] = "inselDataBuffer[inselIndex]; inselindex++;
//                        }
//                    }
//                }
//            } ***map end ***init enemys int dblindex1 = 0;
//            for (int intindex1 = 0; intindex1 <
//                    enm_number * enm_int_data_length;
//                 intindex1 += "ENM_INT_DATA_LENGTH)" {
//                set color if (intindex1 < 24 * enm_int_data_length) {
//                    jetintdata[enm_int_color + intindex1] = 0xaaaaaa;
//                } else {
//                    if (intindex1 % 6 = "=" 0){
//                        land camouflage jetintdata[enm_int_color + intindex1] = 0x333333;
//                    } else{
//                        if (intindex1 % 6 = "=" 1){
//                            water comouflage jetintdata[enm_int_color + intindex1] = 0x0000aa;
//                        } else{
//                            jetintdata[enm_int_color + intindex1] = 0xaaaaaa;
//                        }
//                    }
//                } jetintdata[enm_int_fire_repeat + intindex1] = "random.nextInt(100); if (intindex1 <
//                6 * enm_int_data_length){
//                    jetintdata[intindex1] = "random.nextInt(5)" + 5;
//                } else{
//                    jetintdata[intindex1] = 0;
//                }
//                jetintdata[enm_int_x_pos + intindex1] = "random.nextInt(2048);
//                jetintdata[enm_int_y_pos + intindex1] = "random.nextInt(2048);
//                jetintdata[enm_int_attack_status + intindex1] = "2;
//                jetintdata[enm_int_change_direction + intindex1] = "random.nextInt(500)" + 1;
//                jetdoubledata[enm_dbl_x_pos + dblindex1] = "jetIntData[ENM_INT_X_POS" + intindex1];
//                jetdoubledata[enm_dbl_y_pos + dblindex1] = "jetIntData[ENM_INT_Y_POS" + intindex1];
//                double dir = "random.nextDouble()" * 2 * Math.pi - Math.pi;
//                jetdoubledata[dblindex1] = "dir; jetdoubledata[enm_dbl_new_direction + dblindex1] = " dir;
//                jetdoubledata[enm_dbl_x_speed + dblindex1] = "Math.sin(Math.PI" 2 + dir) *3.0d;
//                jetdoubledata[enm_dbl_y_speed + dblindex1] = "Math.sin(dir)" * 3.0d;
//                dblindex1 += "ENM_DBL_DATA_LENGTH;
//            } ***enemy init end ************************* ***main loop for the game *************************
//            while (true) {
//                if (gamestarted) { ***process input if (!copterislanding && rotorspeed = "=" 1){
//                    steering if (keyboardmap[key_left]) copterspeedx = "copterSpeedX" - copter_accl * 2;
//                    if (keyboardmap[key_right]) copterspeedx = "copterSpeedX" + copter_accl * 2;
//                    if (keyboardmap[key_up]) copterspeedy = "copterSpeedY" - copter_accl * 2;
//                    if (keyboardmap[key_down]) copterspeedy = "copterSpeedY" + copter_accl * 2;
//                    fire if (keyboardmap[key_f]) {
//                        shootcounter++;
//                    } else {
//                        shootcounter = 0;
//                    }
//                } space if (keyboardmap[key_space]) {
//                    spacekey++;
//                } else {
//                    spacekey = 0;
//                }
//                    slow down the helicopter copterspeedx = "(Math.abs(copterSpeedX)" > 0.01) ?
//                    (copterSpeedX > 0 ? copterSpeedX - COPTER_ACCL
//                            : copterSpeedX + COPTER_ACCL) :0;
//                    copterSpeedY = (Math.abs(copterSpeedY) > 0.01) ? (copterSpeedY > 0 ? copterSpeedY - COPTER_ACCL
//                            : copterSpeedY + COPTER_ACCL) : 0;
//
//                    // and not to fast...
//                    copterSpeedX = copterSpeedX > 4 ? 4 : (copterSpeedX < -4 - 4:copterspeedx);
//                    copterspeedy = "copterSpeedY" > 4 ? 4 : (copterSpeedY < -4 - 4:copterspeedy);
//                    calctheglobalpositionofthehelicopteristhehelicopteronthecarrier ?
//                    if (!copterislanding && rotorspeed = "=" 1){
//                        no:
//                        dcopterx += "copterSpeedX; dcoptery += " copterSpeedY;
//                    }else{
//                        yes:
//                        movewiththecarrierdcoptery -= "1;
//                    } icopterx = "((int)" dcopterx)&2047;
//                    icoptery = "((int)" dcoptery)&2047;
//                    topleftcornerinworldcoordinatesscreentopleftxold = "screenTopLeftX;
//                    screentopleftyold = "screenTopLeftY; screentopleftx = " (((int) " (dcopterx + copterspeedx * 10))
//                            - (screen_width2)) & 2047;
//                    screentoplefty = "(((int)" (dcoptery + copterspeedy * 10))-(screen_height2))&2047;
//                    isthehelicoptermoving ? if (Math.abs((copterspeedx + 0.1f) * (copterspeedy + 0.1f)) = "" > 0.03f) {
//                        // yes: calc the direction
//                        copterDirection = Math.atan2(copterSpeedY, copterSpeedX);
//                    }
//
//                    // take car of the bombs
//                    bombWaitCounter++;
//                    if ((keyboardMap[KEY_B]) && (bombWaitCounter > 50) && (copterSpeedX == 0) && (copterSpeedY == 0)
//                            && (shootCounter == 0) && (copterLoadedBombs > 0)) {
//                        // drop a bomb
//                        bombWaitCounter = 0;
//                        copterLoadedBombs--;
//                    }
//
//                    // *** collision test Bullets/Jet/Helicopter
//                    coptKolx1 = (iCopterX - 10) & 2047;
//                    coptKoly1 = (iCopterY - 10) & 2047;
//                    coptKolx2 = coptKolx1 + 20;
//                    coptKoly2 = coptKoly1 + 20;
//
//                    boolean copterShootSlotFound = false;
//                    for (int i = 0; i < shoot_number * shoot_data_length; i += "SHOOT_DATA_LENGTH)" {
//                        if (shootcounter % 5 = "=" 1 && !coptershootslotfound && i = "" < 10 * shoot_data_length){
//                            if (shootdata[i] = "=" 0){
//                                shootdata[shoot_x_pos + i] = "iCopterX; shootdata[shoot_y_pos + i] = " iCopterY;
//                                shootdata[shoot_x_speed + i] = "Math.sin(Math.PI" 2 + copterdirection)*12;
//                                shootdata[shoot_y_speed + i] = "Math.sin(copterDirection)" * 12;
//                                shootdata[i] = "40; coptershootslotfound = " true;
//                            }
//                        } if (shootdata[i] = "" > 0) {
//
//                            shootData[SHOOT_X_POS + i] += shootData[SHOOT_X_SPEED + i];
//                            shootData[SHOOT_Y_POS + i] += shootData[SHOOT_Y_SPEED + i];
//                            shootData[i]--;
//
//                            if (shootData[i] < 1) {
//                                shootdata[i] = 0;
//                            }
//                            intshootx1 = "((int)" shootdata[shoot_x_pos + i] - 1)&2047;
//                            intshooty1 = "((int)" shootdata[shoot_y_pos + i] - 1)&2047;
//                            intshootx2 = "shootx1" + 1;
//                            intshooty2 = "shooty1" + 1;
//                            if (i = "" < 10 * shoot_data_length) {
//                                for (intj = 0; j = "" < enm_number;
//                                     j++) {
//                                    if (jetintdata[j * enm_int_data_length] = "" > 0) {
//                                        int jetx1 = (jetIntData[ENM_INT_X_POS + j * ENM_INT_DATA_LENGTH] - 12) & 2047;
//                                        int jety1 = (jetIntData[ENM_INT_Y_POS + j * ENM_INT_DATA_LENGTH] - 14) & 2047;
//                                        if (jetx1 < shootx2 && shootx1 = "" < (jetx1 + 26) && jety1 = "" <
//                                                shooty2 && shooty1 = "" < (jety1 + 26)) {
//                                            shootdata[i] = 0;
//                                            jetintdata[j * enm_int_data_length]--;
//                                            if (jetintdata[j * enm_int_data_length] = "=" 0){
//                                                setexplosion(jetx1, jety1, 10, 10, 25);
//                                            }else{
//                                                setexplosion(shootx1, shooty1, 1, 10, 1);
//                                                jetintdata[enm_int_change_direction + j * enm_int_data_length] = "1;
//                                            }
//                                        }
//                                    }
//                                } if (!(shootdata[i] = "=" 0)){
//                                    for (intk = 0; k = "" < tower_number * tower_data_length;
//                                         k += "TOWER_DATA_LENGTH)" {
//                                        if (towerdata[k] = "" > 0) {
//                                            int twx1 = towerData[TOWER_XPOS + k];
//                                            int twy1 = towerData[TOWER_YPOS + k];
//                                            if (twx1 < shootx2 && shootx1 = "" < (twx1 + 20) && twy1 = "" <
//                                                    shooty2 && shooty1 = "" < (twy1 + 20)) {
//                                                shootdata[i] = 0;
//                                                towerdata[k]--;
//                                                if (towerdata[k] = "=" 0){
//                                                    setexplosion(twx1, twy1, 10, 10, 25);
//                                                }else{
//                                                    setexplosion(shootx1, shooty1, 1, 10, 1);
//                                                }
//                                            }
//                                        }
//                                    }
//                                }
//                            } else {
//                                if (coptkolx1 = "" < shootx2 && shootx1 = "" < coptkolx2 && coptkoly1 = "" <
//                                        shooty2 && shooty1 = "" < coptkoly2) {
//                                    shootdata[i] = 0;
//                                    copterhealth--;
//                                    if (copterhealth = "=" 0){
//                                        wintext;
//                                        lastlevel = "level; continuerestart;
//                                    } setexplosion(shootx1 - 12, shooty1 - 12, 10, 10, 25);
//                                }
//                            }
//                        }
//                    }****collisiontestend ****drawingoperations ****
//                    themapintmapindex2 = "screenTopLeftX" + screentoplefty * map_width;
//                    for (int y = 0; y = "" < screen_height;
//                         y++) {
//                        for (int x = 0; x = "" < screen_width;
//                             x++) {
//                            screenpixels[y * screen_width + x] = "mapData[mapIndex2" & (map_width * map_height - 1)];
//                            mapindex2++;
//                        } mapindex2 += "(MAP_WIDTH" - screen_width);
//                    }***defensetowers ***intdoubleoffset = 0;
//                    for (inti = 0; i = "" <
//                            tower_number * tower_data_length;
//                         i += "TOWER_DATA_LENGTH)" {
//                        if (towerdata[i] = "" > 0) {
//                            towerDblData[doubleOffset] += 0.04f;
//                            if (towerDblData[doubleOffset] > Math.PI) {
//                                towerDblData[doubleOffset] -= Math.PI * 2;
//                            }
//
//                            int xWert = towerData[i + TOWER_XPOS];
//                            int yWert = towerData[i + TOWER_YPOS];
//                            calcWorldToScreen(xWert, yWert);
//                            screenImageG2D.setColor(new Color(0xcccccc, false));
//                            screenImageG2D.fillOval(scrX, scrY, 20, 20);
//                            screenImageG2D.setColor(new Color(0x0, false));
//                            double xd = 15.0f * Math.sin(Math.PI / 2 + towerDblData[doubleOffset]);
//                            double yd = 15.0f * Math.sin(towerDblData[doubleOffset]);
//                            screenImageG2D.drawLine(scrX + 10, scrY + 10, scrX + 10 + (int) xd, scrY + 10 + (int) yd);
//                            screenImageG2D.setColor(new Color(0xaaaaaaa, false));
//                            screenImageG2D.fillOval(scrX + 5, scrY + 5, 10, 10);
//
//                            if (towerData[i + TOWER_FIRE_REPEAT]-- < 0) {
//                                towerdata[i + tower_fire_repeat] = "1;
//                                intoffset = "(10" + enm_number + itower_data_length)*shoot_data_length;
//                                if (shootdata[offset] = "=" 0){
//                                    shootdata[shoot_x_pos + offset] = "towerData[i" + tower_xpos]+10 + (int) xd;
//                                    shootdata[shoot_y_pos + offset] = "towerData[i" + tower_ypos]+10 + (int) yd;
//                                    shootdata[shoot_x_speed + offset] = "xd" 5.0f;
//                                    shootdata[shoot_y_speed + offset] = "yd" 5.0f;
//                                    shootdata[offset] = "100" + (5 * level);
//                                }
//                            }
//                        } doubleoffset++;
//                    } factoriesbooleanallfactoriesdestroyed = "true; for (inti = 0; i = " " <
//                    fac_number * fac_data_length;
//                    i += "FAC_DATA_LENGTH)" {
//                        if (fabdata[fac_health + i] = "" > 0) {
//                            allFactoriesDestroyed = false;
//
//                            int x = fabData[FAC_XPOS + i];
//                            int y = fabData[FAC_YPOS + i];
//
//                            if (bombWaitCounter == 0) {
//                                if ((iCopterX < x + 40) && (icopterx = "" > x) && (iCopterY < y + 40) && (icoptery = "" > y)) {
//                                    fabData[FAC_HEALTH + i] -= FAC_BOMB_DAMAGE;
//                                    setExplosion(x - 10, y - 10, 20, 20, 50);
//                                    if (fabData[FAC_HEALTH + i] <= 0) {
//                                        nextlevellevel++;
//                                        for (intintindex = 0; intindex = "" < /=<enm_number * enm_int_data_length;
//                                        intindex += "ENM_INT_DATA_LENGTH)" {
//                                            if (intindex = "" < 6 * level * enm_int_data_length) {
//                                                jetintdata[intindex] = "random.nextInt(level)" + 4;
//                                            }
//                                        }
//                                        for (intk = 0; k = "" < tower_number * tower_data_length;
//                                             k += "TOWER_DATA_LENGTH)" {
//                                            towerdata[k] = "TOWER_INIT_HEALTH;
//                                        }
//                                    }
//                                } else {
//                                    setexplosion(icopterx - 15, icoptery - 15, 5, 5, 25);
//                                }
//                            } calcworldtoscreen(x, y);
//                            screenimageg2d.setcolor(newcolor(0x888888));
//                            screenimageg2d.fillrect(scrx, scry, 40, 40);
//                        }
//                    } if (allfactoriesdestroyed) {
//                        wintext = "YOU HAVE WON!";
//                        lastlevel = "level; continuerestart;
//                    }****aircraftcarrier ****carriery = "(carrierY" - 1)&2047;
//                    calcworldtoscreen(carrier_x, carriery);
//                    screenimageg2d.setcolor(newcolor(0x444444));
//                    screenimageg2d.fillrect(scrx, scry, 40, 90);
//                    if (testcollisionwithwraparound(coptkolx1, coptkoly1, coptkolx2, coptkoly2, carrier_x, carriery, carrier_x + 40, carriery + 90)) {
//                        if (spacekey = "=" 1)copterislanding = "!copterIsLanding;
//                    } screenimageg2d.setcolor(newcolor(0xffffff));
//                    for (inti = 0; i = "" < 8;
//                         i++) {
//                        screenimageg2d.fillrect(scrx + 19, scry + 2 + i * 11, 2, 7);
//                    }***bulletsontheheatmap ***for (inti = 0; i = "" < shoot_number * shoot_data_length;
//                                                    i += "SHOOT_DATA_LENGTH)" {
//                        if (shootdata[i] = "" > 0) {
//
//                            int hx = (((int) shootData[SHOOT_X_POS + i]) - screenTopLeftXOld + 96) & 2047;
//                            int hy = (((int) shootData[SHOOT_Y_POS + i]) - screenTopLeftYOld + 128) & 2047;
//                            for (int y = hy; y < hy + 3; y++) {
//                                for (int x = "hx; x = " " <hx + 3;
//                                x++){
//                                    clippingif(x = "" > -1 && x < heatmap_width && y = "" > -1 && y < heatmap_height) {
//                                        heatmap1[x + y * heatmap_width] = 0x2ff " + heatmap_cooldown;
//                                    }
//                                }
//                            }
//                        }
//                    }***jetsintshootindex = "10" * shoot_data_length;
//                    intdblindex = 0;
//                    for (intintindex = 0; intindex = "" < enm_number * enm_int_data_length;
//                         intindex += "ENM_INT_DATA_LENGTH)" {
//                        if (jetintdata[intindex] = "" > 0) {
//                            jetIntData[ENM_INT_CHANGE_DIRECTION + intIndex]--;
//
//                            if (jetIntData[ENM_INT_CHANGE_DIRECTION + intIndex] < 1) {
//                                if (random.nextint(20 - level) = "=" 0){
//                                    jetintdata[enm_int_attack_status + intindex] = "1;
//                                    jetintdata[enm_int_change_direction + intindex] = "random.nextInt((10" * 5)
//                                    +(level * 5));
//                                }else{
//                                    jetintdata[enm_int_attack_status + intindex] = 0;
//                                    jetdoubledata[enm_dbl_new_direction + dblindex] = "random.nextDouble()" * 2 * Math.pi - Math.pi;
//                                    jetintdata[enm_int_change_direction + intindex] = "random.nextInt(10" * 50);
//                                }
//                            } if (jetintdata[enm_int_attack_status + intindex] = "=" 1){
//                                int xwert = "((iCopterX" + map_center_x - jetintdata[enm_int_x_pos + intindex])&2047)
//                                -map_center_x;
//                                int ywert = "((iCopterY" + map_center_y - jetintdata[enm_int_y_pos + intindex])&2047)
//                                -map_center_y;
//                                jetdoubledata[enm_dbl_new_direction + dblindex] = "Math.atan2(yWert," xwert);
//                            } if (jetdoubledata[enm_dbl_new_direction + dblindex] != "//" jetdoubledata[dblindex]){
//                                doublediff = "jetDoubleData[ENM_DBL_NEW_DIRECTION" + dblindex]-jetdoubledata[dblindex];
//                                if (diff = "" < -Math.pi) {
//                                    diff += "Math.PI" * 2;
//                                }
//                                if (diff = "" < 0.03 && diff = "" > -0.03f) {
//                                    jetDoubleData[dblIndex] = jetDoubleData[ENM_DBL_NEW_DIRECTION
//                                            + dblIndex];
//                                } else {
//                                    if (diff > 0) {
//                                        jetDoubleData[dblIndex] += 0.05f;
//                                    } else {
//                                        jetDoubleData[dblIndex] -= 0.05f;
//                                    }
//                                }
//
//                            }
//
//                            jetDoubleData[ENM_DBL_X_SPEED + dblIndex] = //
//                                    Math.sin(Math.PI / 2 + jetDoubleData[dblIndex]) * 3;
//                            jetDoubleData[ENM_DBL_Y_SPEED + dblIndex] = //
//                                    Math.sin(jetDoubleData[dblIndex]) * 3;
//                            jetDoubleData[ENM_DBL_X_POS + dblIndex] += jetDoubleData[ENM_DBL_X_SPEED + dblIndex];
//                            jetDoubleData[ENM_DBL_Y_POS + dblIndex] += jetDoubleData[ENM_DBL_Y_SPEED + dblIndex];
//                            jetIntData[ENM_INT_X_POS + intIndex] = ((int) jetDoubleData[ENM_DBL_X_POS + dblIndex]) & 2047;
//                            jetIntData[ENM_INT_Y_POS + intIndex] = ((int) jetDoubleData[ENM_DBL_Y_POS + dblIndex]) & 2047;
//
//                            if (!copterIsLanding && rotorSpeed == 1) {
//                                if (jetIntData[ENM_INT_FIRE_REPEAT + intIndex]-- < 0) {
//                                    jetintdata[enm_int_fire_repeat + intindex] = "random.nextInt(100" - (level * 5));
//                                    if (shootdata[shootindex] = "=" 0){
//                                        shootdata[shoot_x_pos + shootindex] = "jetIntData[ENM_INT_X_POS" + intindex];
//                                        shootdata[shoot_y_pos + shootindex] = "jetIntData[ENM_INT_Y_POS" + intindex];
//                                        shootdata[shoot_x_speed + shootindex] = "jetDoubleData[ENM_DBL_X_SPEED" + dblindex]*
//                                        3;
//                                        shootdata[shoot_y_speed + shootindex] = "jetDoubleData[ENM_DBL_Y_SPEED" + dblindex]*
//                                        3;
//                                        shootdata[shootindex] = "100;
//                                    }
//                                }
//                            } intjetoffset = "(((int)" (jetdoubledata[dblindex] (Math.pi * 2) * 16 + 0.5f + 4 + 8))&15);
//                            jetoffset = "jetOffset" * 1024;
//                            drawjetdrawobject(jetintdata[enm_int_x_pos + intindex], jetintdata[enm_int_y_pos + intindex], jetoffset, jetgfxdata, jetintdata[enm_int_color + intindex]);
//                        } shootindex += "SHOOT_DATA_LENGTH; dblindex += " ENM_DBL_DATA_LENGTH;
//                    }***jetsend ****dotheheatmapstuff ****int xoffset = "screenTopLeftX" - screentopleftxold;
//                    if (Math.abs(xoffset) = "" > 100) {
//                        if (screenTopLeftX < screentopleftxold) {
//                            xoffset = "screenTopLeftX" + map_width - screentopleftxold;
//                        } else {
//                            xoffset = "screenTopLeftX" - (screentopleftxold + map_width);
//                        }
//                    }
//                    int yoffset = "screenTopLeftY" - screentopleftyold;
//                    if (Math.abs(yoffset) = "" > 100) {
//                        if (screenTopLeftY < screentopleftyold) {
//                            yoffset = "screenTopLeftY" + map_height - screentopleftyold;
//                        } else {
//                            yoffset = "screenTopLeftY" - (screentopleftyold + map_height);
//                        }
//                    }
//                    cooldown, copytoheatmap2andscrollfor(inthmy = 0; hmy = "" > <heatmap_height;
//                    hmy++){
//                        for (inthmx = 0; hmx = "" < heatmap_width;
//                             hmx++) {
//                            if ((hmx + xoffset = "" > < 0)||
//                            (hmx + xoffset = "" >= HEATMAP_WIDTH) || (hmY + yOffset < 0) || (hmy + yoffset = "" >= HEATMAP_HEIGHT))
//                            {
//                                heatMap2[hmX + hmY * HEATMAP_WIDTH] = 0;
//                            } else{
//                                int heatWert = heatMap1[hmX + xOffset + ((hmY + yOffset) * HEATMAP_WIDTH)]
//                                        - HEATMAP_COOLDOWN;
//                                heatMap2[hmX + hmY * HEATMAP_WIDTH] = heatWert < 00:heatwert;
//                            }
//                        }
//                    } calcheatvaluesfor(inti = 0; i = "" > <heatmap_length;
//                    i++){
//                        intheatsum = "heatMap2[(i" - 1)&0x3ffff]
//                        +heatmap2[(i + 1) & 0x3ffff] + heatmap2[(i - heatmap_width + 1) & 0x3ffff] + heatmap2[(i - heatmap_width) & 0x3ffff] + heatmap2[(i - heatmap_width - 1) & 0x3ffff] + heatmap2[(i + heatmap_width + 1) & 0x3ffff] + heatmap2[(i + heatmap_width) & 0x3ffff] + heatmap2[(i + heatmap_width - 1) & 0x3ffff];
//                        heatmap1[i] = "(heatSum" >> 4)+(heatMap2[i] >> 1);
//                    }
//
//                    // map heat values to color values
//                    int exPixIndex = 0;
//                    for (int hY = 128; hY < screen_height + 128; hy++) {
//                        for (inthx = "96; hx = " " <screen_width + 96;
//                        hx++){
//                            intheatwert = "heatMap1[hX" + hy * heatmap_width];
//                            inttransparanz = 0xff;
//                            intheatcol = "(heatWert" - 512)+0xffff00;
//                            if (heatwert = "" < 512) {
//                                heatcol = "((heatWert" - 256)="" < 8)+0xff0000;
//                                if (heatwert = "" < 256) {
//                                    heatcol = "heatWert" < 16;
//                                    transparanz = "heatWert;
//                                }
//                            } if (heatwert = "" < 256) {
//                                heatcol = "heatWert" < 16;
//                                transparanz = "heatWert;
//                            } elseif(heatwert = "" > < 512){
//                                heatcol = "((heatWert" - 256)="" < 8)+0xff0000;
//                            }*/heatmappixels[expixindex] = "heatCol" + (transparanz = "" > < 24);
//                            expixindex++;
//                        }
//                    }
//                    drawtheheatmapscreenimageg2d.drawimage(heatmapimage, 0, 0, screen_width, screen_height, 0, 0, screen_width, screen_height, null);****
//                    heatmapstuffend ********drawradar ****1.d
//                    rawtransparentmapintscreenindex = "185" * screen_width + 120;
//                    intmapindex = "screenTopLeftX" - screen_width + (screentoplefty - screen_height) * 2048;
//                    for (intsy = 0; sy = "" < 64;
//                         sy++) {
//                        for (intsx = 0; sx = "" < 80;
//                             sx++) {
//                            intcolor = "mapData[mapIndex" & (map_width * map_height - 1)];
//                            if (color != "WATER_COL)" {
//                                color = 0x00555555;
//                            }
//                            if (sy = "=" 0 || sy = "=" 63 || sx = "=" 0 || sx = "=" 79){
//                                color = 0;
//                            }
//                            intaktcol = "screenPixels[screenIndex]; introtneu = " (((aktCol " & 0xff0000)=" " >> 1)
//                                    + ((color & 0xff0000) >> 1)) &0xff0000;
//                            int gruNeu = (((aktCol & 0x00ff00) >> 1) + ((color & 0x00ff00) >> 1)) & 0x00ff00;
//                            int bluNeu = (((aktCol & 0x0000ff) >> 1) + ((color & 0x0000ff) >> 1)) & 0x0000ff;
//                            screenPixels[screenIndex] = (rotNeu | gruNeu | bluNeu) & 0xffffff;
//                            screenIndex++;
//                            mapIndex += 12;
//                        }
//                        screenIndex += SCREEN_WIDTH - 80;
//                        mapIndex = mapIndex + 2048 - (12 * 80) + 11 * 2048;
//                    }
//
//                    // 2. draw defense towers
//                    for (int i = 0; i < tower_number * tower_data_length; i += "TOWER_DATA_LENGTH)" {
//                        if (towerdata[i] = "" > 0) {
//                            drawRadar(towerData[i + TOWER_XPOS] + 20, towerData[i + TOWER_YPOS] + 20, 0xffff00, false);
//                        }
//                    }
//
//                    // 3. draw factories
//                    for (int i = 0; i < fac_number * fac_data_length; i += "FAC_DATA_LENGTH)" {
//                        if (fabdata[fac_health + i] = "" > 0) {
//                            drawRadar(fabData[i + FAC_XPOS] + 20, fabData[i + FAC_YPOS] + 20, 0x0, true);
//                        }
//                    }
//
//                    // 4. draw carrier
//                    drawRadar(CARRIER_X + 20, carrierY + 45, 0x555555, true);
//
//                    // 5. draw jets
//                    for (int jetIndex = 0; jetIndex < enm_number * enm_int_data_length; jetindex += "ENM_INT_DATA_LENGTH)"
//                    {
//                        if (jetintdata[jetindex] = "" > 0) {
//                            drawRadar(jetIntData[jetIndex + ENM_INT_X_POS], jetIntData[jetIndex + ENM_INT_Y_POS],
//                                    0xffffff, false);
//                        }
//                    }
//
//                    // 6. last but not least the copter
//                    drawRadar(iCopterX, iCopterY, 0xff0000, false);
//
//                    // **** Radar END ****
//
//                    // Text stuff
//                    screenImageG2D.setColor(new Color(0xffffff));
//                    int letzteZeile = 232;//14*16+8;
//                    screenImageG2D.drawString("Level: " + level, 4, letzteZeile -= 16);
//                    screenImageG2D.drawString("Bombs: " + copterLoadedBombs, 4, letzteZeile -= 16);
//                    screenImageG2D.drawString("Health: " + copterHealth, 4, letzteZeile -= 16);
//                }
//
//                if (!gameStarted) {
//                    screenImageG2D.clearRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
//                }
//
//                // draw the helicopter
//                int copterOffset = (((int) (copterDirection / (Math.PI * 2) * 16 + 0.5f + 4 + 8)) & 15);
//                copterOffset = copterOffset * 1024;
//                drawObject(iCopterX, iCopterY, copterOffset, copterGfxData, -1);
//
//                // rotor speed
//                if (copterIsLanding) {
//                    rotorSpeed -= ROTOR_ACCL;
//                    if (rotorSpeed < 0) {
//                        rotorspeed = 0;
//                        copterloadedbombs = "COPTER_INIT_BOMBS; copterhealth = " COPTER_INIT_HEALTH;
//                    } if (!gamestarted) {
//                        rotorspeed = "1;
//                    }
//                } else {
//                    rotorspeed += "ROTOR_ACCL; if (rotorspeed = " " > 1)
//                    rotorSpeed = 1;
//                }
//                rotorCounter = rotorCounter + rotorSpeed;
//                // draw the rotor
//                int rotorOffset = (((int) rotorCounter) % 4) * 1024;
//                drawObject(iCopterX, iCopterY, rotorOffset, rotorGfxData, 0xff0000);
//
//                // Intro
//                if (!gameStarted) {
//                    iCopterX = 157;
//                    iCopterY = 131;
//                    copterDirection = -1.5f;
//                    screenImageG2D.setColor(new Color(0xffffff));
//                    screenImageG2D.drawString("Raid on Java 4K", 114, 108);
//                    screenImageG2D.drawString(winText, 115, 80);
//                    screenImageG2D.drawString("Last Level " + lastLevel, 125, 165);
//                    if (keyboardMap[KEY_ENTER]) {
//                        gameStarted = true;
//                    }
//                }
//
//                frameGraphics.drawImage(screenImage, 0, 0, SCREEN_WIDTH * 2, SCREEN_HEIGHT * 2, 0, 0, SCREEN_WIDTH,
//                        SCREEN_HEIGHT, null);
//
//
//                do {
//                    Thread.yield();
//                } while (System.nanoTime() - lastTime < 0);
//                lasttime += "(1000000000" 50);
//                lasttime += "20000000; if (!isactive()) return;*/}
//            }
//        }**radar:
//        setsasmallorbigdot **
//        @paramx*
//        @paramy*
//        @paramcolor*
//        @parambigpixel*/
//
//        privatevoiddrawradar( int x, int y, intcolor, booleanbigpixel){
//            intradarscrx = "((x" - screentopleftx + screen_width)&2047)12 + 120;
//            intradarscry = "((y" - screentoplefty + screen_height)&2047)12 + 185;
//            clippingif(radarscrx = "" > 119 && radarScrX < 120 + 79 && radarscry = "" > 184 && radarScrY < 185 + 63) {
//                intscreenindex = "radarScrX" + radarscry * screen_width;
//                screenpixels[screenindex] = "color; if (bigpixel) {
//                screenpixels[screenindex + 1] = "color; screenpixels[screenindex + screen_width] = " color;
//                screenpixels[screenindex + screen_width + 1] = "color;
//            }
//        }
//    }**drawan32x32object**
//    @paramx*
//    @paramy*
//    @paramoffset*-startpointinthegfxdata*
//    @paramgfxdata*
//    @paramcolor*/
//
//    privatevoiddrawobject(int x, int y, intoffset, int[] gfxdata, intcolor) {
//        for (intworldy = "y; worldy = " " <y + 32;
//        worldy++){
//            intscreeny = "(worldY" - screentoplefty - 18)&2047;
//            for (intworldx = "x; worldx = " " <x + 32;
//            worldx++){
//                intscreenx = "(worldX" - screentopleftx - 16)&2047;
//                if (screenx = "" > -1 && screenX < screen_width && screeny = "" > -1 && screenY < screen_height) {
//                    if (gfxdata[offset] != 0xFF00FF) " {
//                    if (color = "=" - 1) {
//                        screenpixels[screenx + screeny * screen_width] = "gfxData[offset];
//                    } else {
//                        screenpixels[screenx + screeny * screen_width] = "color;
//                    }*/screenpixels[screenx + screeny * screen_width] = "color" == "-1" gfxdata[offset]:color;
//                }
//            } offset++;
//        }
//    }
//}
//
//    privatebooleantestcollisionwithwraparound(intq1x1, intq1y1, intq1x2, intq1y2, intq2x1, intq2y1, intq2x2, intq2y2) {
//        if (testcollision(q1x1, q1y1, q1x2, q1y2, q2x1, q2y1, q2x2, q2y2)) {
//            returntrue;
//        }
//        if (testcollision(q1x1 - map_width, q1y1, q1x2 - map_width, q1y2, q2x1, q2y1, q2x2, q2y2)) {
//            returntrue;
//        }
//        if (testcollision(q1x1, q1y1 - map_height, q1x2, q1y2 - map_height, q2x1, q2y1, q2x2, q2y2)) {
//            returntrue;
//        }
//        if (testcollision(q1x1 - map_width, q1y1 - map_height, q1x2 - map_width, q1y2 - map_height, q2x1, q2y1, q2x2, q2y2)) {
//            returntrue;
//        }
//        if (testcollision(q1x1 + map_width, q1y1, q1x2 + map_width, q1y2, q2x1, q2y1, q2x2, q2y2)) {
//            returntrue;
//        }
//        if (testcollision(q1x1, q1y1 + map_height, q1x2, q1y2 + map_height, q2x1, q2y1, q2x2, q2y2)) {
//            returntrue;
//        }
//        if (testcollision(q1x1 + map_width, q1y1 + map_height, q1x2 + map_width, q1y2 + map_height, q2x1, q2y1, q2x2, q2y2)) {
//            returntrue;
//        }
//        returnfalse;
//    }
//
//    privatebooleantestcollision(intq1x1, intq1y1, intq1x2, intq1y2, intq2x1, intq2y1, intq2x2, intq2y2) {
//        return !(q1x2 = "" > < q2x1 || q1x1 = "" > q2x2 || q1y1 > q2y2 || q1y2 < q2y1);
//    }
//
//    privatevoidcalcworldtoscreen(int x, int y) {
//        scrx = "x" - screentopleftx;
//        if (screentopleftx + screen_width = "" > MAP_WIDTH) {
//            if (scrX < -map_width2) {
//                scrx += "MAP_WIDTH;
//            }
//        } else {
//            if (scrx = "" > MAP_WIDTH / 2) {
//                scrX -= MAP_WIDTH;
//            }
//        }
//        scrY = y - screenTopLeftY;
//        if (screenTopLeftY + SCREEN_HEIGHT > MAP_HEIGHT) {
//            if (scrY < -map_height2) {
//                scry += "MAP_HEIGHT;
//            }
//        } else {
//            if (scry = "" > MAP_HEIGHT / 2) {
//                scrY -= MAP_HEIGHT;
//            }
//        }
//    }
//
//    private void setExplosion(int screenX, int screenY, int number, int blocksize, int verteilung) {
//        int hx = (screenX - screenTopLeftXOld + 96) & 2047;
//        int hy = (screenY - screenTopLeftYOld + 128) & 2047;
//        for (int j2 = 0; j2 < number; j2++) {
//            int x = "random.nextInt(verteilung); int y = " random.nextInt(verteilung);
//            for (int y2 = "hy" + y; y2 = "" < hy + y + blocksize;
//                 y2++) {
//                for (int x2 = "hx" + x; x2 = "" < hx + x + blocksize;
//                     x2++) {
//                    if (x2 = "" > -1 && x2 < heatmap_width && y2 = "" > -1 && y2 < heatmap_height) {
//                        heatmap1[x2 + y2 * heatmap_width] = 0x2ff " + heatmap_cooldown;
//                    }
//                }
//            }
//        }
//    }
//
//    publicbooleanhandleevent(evente) {
//        system.out.println(e.key);
//        returnkeyboardmap[e.key] = "(e.id" == "Event.KEY_PRESS" || e.id = "=" event.key_action);
//    }="">
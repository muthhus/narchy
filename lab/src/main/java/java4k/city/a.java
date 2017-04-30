//package java4k.city;
//
//import java.applet.Applet;
//import java.awt.Color;
//import java.awt.Event;
//import java.awt.Graphics2D;
//import java.awt.image.BufferedImage;
//import java.util.ArrayList;
//
///**
// *
// * @author Russ and Maggie
// */
//public class a extends Applet implements Runnable {
//
//    @Override
//    public void start() {
//        //enableEvents(8);
//        //enableEvents(Event.MOUSE_MOVE);
//        new Thread(this).start();
//
//    }
//    final int BOTTOM = 480;
//    final int EDGE = 640;
//    final int SQUAREHEIGHT = 24;
//    final int SQUAREWIDTH = 32;
//    final int SQUAREPERWINDOW = 20;
//    final short CONTROLLENGTH = 8;
//    final short[] CONTROLCOLOR = {0,
//        12,
//        1,
//        11,
//        6,
//        // 7,
//        2,
//        9,
//        7};
//    final short[] CONTROLZONES = {3,
//        0,
//        4,
//        1,
//        0,
//        //1,
//        3,
//        3,
//        1};
//    final short[] CONTROLOFFSETS = {
//        10, 10,
//        10, 420,
//        10, 80,
//        80, 80,
//        80, 420,
//        //10, 280,
//        80, 10,
//        150, 10,
//        150, 80};
//    final short[] CONTROLZONETYPE = {zonedResidental,
//        0,
//        powerPlant,
//        powerLines,
//        0,
//        //* noBuilding,
//        zonedCommerical,
//        zonedIndustrial,
//        road};
//    final char[] CONTROLLABELS = {'R', 'V', 'P', 'L', 'T', /*
//         * 'B',
//         */ 'C', 'I', 'S'};
//    final short CONTROLSIZE = 45;
//    static short controlSelected = -1;
//    final short NOSELECTION = -1;
//    final short RESCONTROL = 0;
//    final short LANDVALUECONTROL = 1;
//    final short POWERPLANTCONTROL = 2;
//    final short POWERLINECONTROL = 3;
//    final short VIEWTRAFFICCONTROL = 4;
//    final short BULLDOZECONTROL = 5;
//    final short COMCONTROL = 5;
//    final short INDCONTROL = 6;
//    final short ROADCONTROL = 7;
//    final float MAXLANDVALUE = 70;
//    //ScrollShorts
//    static short scrollDelay = 80;
//    //static Point mLocation = new Point();
//    static int mx, my;
//    static boolean[] scroll = {false, false, false, false};
//    static boolean leftClick = false;
//    static boolean rightClick = false;
//    static int VIEWPORTTOP = 32;
//    static int VIEWPORTLEFT = 32;
//    static int money = 1000;
//    final String S = "aaaaaaaaaaaaaaaaaaaaaaabaaaaaabbaaaaaaaacccccccccccccccccccccccc" + //64
//            "deebdeebafdggdfaadddddgbaffffabbaffffffachhhhhbcccijijiccckkckkc" + //64
//            "ddfbddfbafdggdfaaffffdgbafddfabbafgdgdfackkkkkhcccjijijcckhhkhhc" + //64
//            "ddfbddfbafddddfalllafdgbafddfabbafdgdgfackhkhkhccbjijijcckhhkhhc" + //64
//            "ddfbddfbafddddfalmlafdgbaffffaffaffffffackkkkkhccccccccccckkbkkc" + //64
//            "aaabaaabaaabbaaalllaaaabaaaaaaddaaaabbaacccccccccccccccccccccccc" + //64
//            "bbbbbbbbkkbbbbkkkkbbbbkkbbbjbbbbaaaaaaaacccccccccccccccccccccccc" + //64
//            "bbbbbbbbkbbeebbkkbbeebbkbbbbjbbbaannnnnacbkkkkbccmhkkhmcchkbbkhc" + //64
//            "bjjbjbbjkbeeeebkkbeoiebkbbbbjbbbannnnnnncbkhhkbcckkggkkcchkbbkhc" + //64
//            "jbbjbjjbkbeeeebkkbeioebkbbbjbbbbangnngnncbbhhbbcckkggkkcchkbbkhc" + //64
//            "bbbbbbbbkbbeebbkkbbeebbkbbbjbbbbaaggaggacbbbbbbccmhkkhmcchkbbkhc" + //64
//            "bbbbbbbbkkbbbbkkkkbbbbkkbbbbjbbbaaggaggacccccccccccccccccccccccc" + //64
//            "ppppppppaaaaaaaaqqqqqqqqccccccccppppppppqqqqqqqqqqqqqqqqqqqqqqqq" + //64
//            "ppiiooppaaaeaaaaqqqeeeqqcceeeeccpeippeipqkbkqkkqqbbbbkkqqbkbkbkq" + //64
//            "pipoopipaaeaeaaaqqqqeqqqccecccccppppppppqbebkkkqqbeebbbqqbkbkbeq" + //64
//            "pipoopipaaeeaaaaqqqqeqqqccecccccppppppppqkbkqkkqqbeebkkqqbkbkbkq" + //64
//            "ppooiippaaeaeaaaqqqeeeqqcceeeeccpieppiepqqqqqqqqqbbbbbbqqbkbkbkq" + //64
//            "ppppppppaaaaaaaaqqqqqqqqccccccccppppppppqqqqqqqqqqqqqqqqqqqqqqqq" + //64
//            "bbbbbbbbrrrrrrkrpppepeppsasasasapippppppqqqqqqqqqqqqqqqqqqqqqqqq" + //64
//            "bbbbbbbbrrrrrkrkpppepeppasasasaspeppppepqkkkkkeqqbbqbbqqqqbebkkq" + //64
//            "bbbjjbbbrrrkrrrreeeeeeeesasasasappppppipqbbkbbeqqbbqbbqqqbegebkq" + //64
//            "bbbjjbbbrrkrkrrrpppepeppasasasaspippppppqbbkbbeqqqkqkqqqqbegebkq" + //64
//            "bbbbbbbbrrrrrrrreeeeeeeesasasasapeppppepqkkkkkeqqbkkkbqqqqbebkkq" + //64
//            "bbbbbbbbrrrrrrrrpppepeppasasasasppppppipqqqqqqqqqqqqqqqqqqqqqqqq";// + //64
//    int[] COLORS = {
//        0xff79932a, //a0
//        0xff938e93,//b1
//        0xffc4ede1,//c2
//        0xff7e4c0a,//d3
//        0xff000000,//e4
//        0xffb26b0e,//f5
//        0xff503006,//g6
//        0xffe8edeb,//h7
//        0xffe51f24,//i8
//        0xffe5fb24,//j9
//        0xffb4b8b6,//k10
//        0xff37913f,//l11
//        0xff91378c,//m12
//        0xff1d5d00,//n13
//        0xffe85d00,//o14
//        0x00f5932a,//p15
//        0xffd5e10d,//q16
//        0xff0e6ab8,//r17
//        0xff9ebd40//s18
//    };
//    //StartX,StartY,EndX,EndY,Color,Movement?
//    //Level init delay
//    int delay = 0;
//    final static int MAPWIDTH = 128;
//    final static int MAPHEIGHT = 128;
//    final short CELLSTOUPDATE = MAPWIDTH * 8;
//    final static int[] RIGHTLEFTDOWNUP = {1, -1, MAPWIDTH, -MAPWIDTH};
//    final static int ARRAYSIZE = MAPHEIGHT * MAPWIDTH;
//    static float[] newLandValue;
//    static float[] landValue;
//    final static short plainGround = 2;
//    final static short waterDeep = 1;
//    static short[] buildType;
//    final static short noBuilding = 0;
//    final static short zonedResidental = 1;
//    final static short zonedIndustrial = 4;
//    final static short waterTile = 3;
//    final static short treeTile = 5;
//    final static short zonedCommerical = 2;
//    final static short road = 15;
//    //THINGS THAT CAN TRANSMIT ELECTRICITY // START AT 25
//    final static short TRANSMITSPOWER = 25;
//    final static short powerPlant = 25;
//    final static short powerLines = 26;
//    final static short builtResidental = 27;
//    final static short roadWithPowerLINE = 28;
//    final static short builtIndustrial = 29;
//    final static short builtCommerical = 30;
//    BufferedImage[] sprites;
//    static short[] graphicArray;
//    final static short[] resImage = {0, 1, 2, 3, 4};
//    final static short[] comImage = {5, 6, 7, 13, 14, 15};
//    final static short[] indImage = {21, 22, 23, 29, 30, 31};
//    final static short watImage = 25;
//    final static short roadImageLeftRight = 8;
//    final static short roadImageUpDown = 11;
//    final static short roadImageAllway = 24;
//    final static short zonedResidentialImage = 17;
//    final static short zonedCommericalImage = 19;
//    final static short zonedIndustrialImage = 18;
//    final static short treeImage = 12;
//    final static short powerLineImage = 26;
//    final static short TRAFFICUP = 28;
//    final static short TRAFFICLEFT = 20;
//    final static short NOPOWERIMAGE = 16;
//    final static short groundImage = 27;
//    final static short gANEEDSUPDATE = -1;
//    final static short[] powerPlantImage = {9, 10};
//    static short lastPowerPlantImage = 0;
//    static short[] jobs;
//    static boolean[] power;
//    static boolean updatePower = false;
//    final static short POWERPLANTOUPUT = 30;
//    static short[] roads;
//    static int numRoads = 0;
//    final static short NOROAD = -1;
//    static short lastDraw = 0;
//    static short[] traffic;
//    static int numComm = 0;
//    static int numInd = 0;
//    final static short MAXTRAFFIC = 20;
//    static short demand[] = {20, 20, 20};
//    static int[] sList = new int[ARRAYSIZE];
//    static short[] sCheck = new short[ARRAYSIZE];
//    static int net = 0;
//    static int population = 0;
//    static int i, j;
//    static int offset;
//    static short btype;
//    static int neighbor;
//    static Color[] defC = new Color[19];
//    static final short SSWIDTH = 64, SSHEIGHT = 24;
//    static final short numSprite = 8;
//    static final short SPRITEWIDTH = 8, SPRITEHEIGHT = 6;
//    static long nextFrameStartTime;
//    static int squareIndex;
//    static int gX;
//    static int gY;
//    static int ticks;
//    static int indBegin;
//    static int indEnd;
//    static int currCell;
//    static int hasRoad;
//    static float added;
//    static int cell;
//
//    /*
//     *
//     * static final void markPower(int cell) { //Walk through the grid if (cell
//     * < 0="" ||="" cell="">= ARRAYSIZE) { return; } if (power[cell] == 10) { return; //
//     * Don't run on marked cells } power[cell] = 10; // Cell Marked
//     * //ySstem.out.println("MarkPower "+cell);
//     *
//     * if (((cell % MAPWIDTH) + 1) <= mapwidth)="" {="" *="" *="" if="" ((buildtype[cell="" +="" 1]="">= TRANSMITSPOWER) && power[cell + 1] < 5)="" {="" *="" markpower(cell="" +="" 1);="" }="" }="" if="" (((cell="" %="" mapwidth)="" -="" 1)=""> 0) { if
//     * ((buildType[cell - 1] >= TRANSMITSPOWER) && power[cell - 1] < 5)="" {="" *="" markpower(cell="" -="" 1);="" }="" }="" if="" (buildtype[cell="" +="" mapwidth]="">= TRANSMITSPOWER
//     * && power[cell + MAPWIDTH] < 5)="" {="" markpower(cell="" +="" mapwidth);="" }="" if="" *="" (buildtype[cell="" -="" mapwidth]="">= TRANSMITSPOWER && power[cell - MAPWIDTH] < *="" 5)="" {="" markpower(cell="" -="" mapwidth);="" }="" power[cell]="POWERPLANTOUPUT;" *="" *="" *="" }="" */="" *="" static="" final="" void="" clearzone(int="" cell)="" {="" *="" *="" if="" (buildtype[cell]="=" nobuilding)="" {="" return;="" }="" boolean="" centraltile="*" (edgearray[cell]="=" building);="" buildtype[cell]="noBuilding;" *="" edgearray[cell]="noBuilding;" graphicarray[cell]="gANEEDSUPDATE;" if="" at="" *="" an="" edge="" find="" all="" central="" tiles="" and="" run="" on="" that="" do="" nothing="" for="" yourself="" *="" for="" (i="-1;" i="">< 2;="" i++)="" {="" for="" (j="-1;" j="">< 2;="" j++)="" {="" int="" neighbor="cell" *="" +="" i="" +="" j="" *="" mapwidth;="" system.out.println("checking="" "+neighbor="" +"="" *="" ("+i+","+j+")");="" *="" *="" if="" ((cell="" %="" mapwidth="" +="" i)="">< 0="" ||="" (cell="" %="" mapwidth="" +="" i)="">= MAPWIDTH) {
//     * System.out.println("Not touching " + neighbor); continue; }
//     *
//     * if (neighbor < 0="" ||="" neighbor="">= ARRAYSIZE) { continue; } if (cell ==
//     * neighbor) { continue; } if (edgeArray[neighbor] == BUILDING) {
//     * System.out.println("CALLING AGAIN on " + neighbor); clearZone(neighbor);
//     * } if (edgeArray[neighbor] == BUILDINGEDGE && centralTile) {
//     * buildType[neighbor] = noBuilding; edgeArray[neighbor] = noBuilding;
//     * graphicArray[neighbor] = gANEEDSUPDATE; } } }
//     *
//     * }
//     */
//    public void run() {
//        boolean zoneOK;
//        //init Colors and sprites
//
//        for (i = 0; i < 19;="" i++)="" {="" defc[i]="new" color(colors[i]);="" }="" decompress="" spritesheet="" ------------------------------------------------="" bufferedimage="" ss="new" bufferedimage(sswidth,="" ssheight,="" bufferedimage.type_int_argb);="" for="" (i="0;" i="">< ssheight;="" i++)="" {="" for="" (j="0;" j="">< sswidth;="" j++)="" {="" ss.setrgb(j,="" i,="" colors[s.charat(j="" +="" i="" *="" sswidth)="" -="" 'a']);="" }="" }="" sprites="new" bufferedimage[32];="" for="" (i="0;" i="">< 8;="" i++)="" {="" for="" (j="0;" j="">< 4;="" j++)="" {="" sprites[i="" +="" j="" *="" 8]="ss.getSubimage(i" *="" spritewidth,="" j="" *="" spriteheight,="" spritewidth,="" spriteheight);="" }="" }="" end="" decompress="" spritesheet="" init="" map="" data="" ---------------------------------------="" jobs="new" short[arraysize];="" newlandvalue="new" float[arraysize];="" landvalue="new" float[arraysize];="" traffic="new" short[arraysize];="" buildtype="new" short[arraysize];="" roads="new" short[arraysize];="" power="new" boolean[arraysize];="" graphicarray="new" short[arraysize];="" for="" (i="0;" i="">< (arraysize);="" i++)="" {="" graphicarray[i]="gANEEDSUPDATE;" jobs[i]="0;" gx="i" %="" mapwidth;="" gy="i" mapwidth;="" if="" (gx=""><= 4="" ||="" gx="">= MAPWIDTH - 3 || gY <= 3="" |="" gy="">= MAPHEIGHT - 3) {
//                buildType[i] = waterTile;
//
//                continue;
//            }
//            landValue[i] = 20.f;
//
//            buildType[i] = noBuilding;
//            //roads[i] = NOROAD;
//            //power[i] = 0;
//
////            edgeArray[i] = noBuilding;
//            double t = Math.random();
//            if (t > .5) {
//
//                buildType[i] = treeTile;
//
//            } else if (t < .01f)="" {="" buildtype[i]="waterTile;" }="" }="" end="" end="" map="" init="" -------------------------------------------="" int="" lastcellupdated="0;" get="" image="" for="" doing="" double="" buffering="" bufferedimage="" image="new" bufferedimage(edge,="" bottom,="" 1);="" graphics2d="" g="(Graphics2D)" image.getgraphics();="" graphics2d="" g2="null;" *="" *="" run="" the="" game="" loop="" as="" many="" times="" as="" possible="" until="" next="" frame="" time="" *="" has="" come.="" then="" render.="" */="" game="" loop="" nextframestarttime="System.nanoTime();" while="" (true)="" {="" system.out.println("framestart");="" game="" logic="" scrolldelay--;="" neighbor="0;" this="" indicates="" how="" many="" cells="" we="" should="" work="" on="" before="" we="" try="" to="" render="" again="" int="" cellend="(((lastCellUpdated" +="" cellstoupdate)=""> ARRAYSIZE) ? ARRAYSIZE : lastCellUpdated + CELLSTOUPDATE);
//
//            //clear power
//
//
//            //System.out.println(lastCellUpdated + " " + CELLEND);
//            for (cell = lastCellUpdated; cell < cellend;="" cell++)="" {="" update="" graphic="" representation="" lastdraw++;="" btype="buildType[cell];" if="" (lastdraw="">< 0)="" {="" updatepower="true;" lastdraw="0;" }="" first="" set="" terrain="" if="" its="" there="" graphic="" array="" updates="" if="" (graphicarray[cell]="=" ganeedsupdate)="" {="" if="" (btype="=" watertile)="" {="" graphicarray[cell]="watImage;" }="" else="" {="" graphicarray[cell]="groundImage;" }="" overwrite="" terrain="" with="" building="" if="" (btype="=" powerplant)="" {="" graphicarray[cell]="powerPlantImage[lastPowerPlantImage];" lastpowerplantimage++;="" lastpowerplantimage="" %="2;" }="" if="" (btype="=" zonedresidental)="" {="" graphicarray[cell]="zonedResidentialImage;" }="" if="" (btype="=" builtresidental)="" {="" graphicarray[cell]="resImage[lastDraw" %="" 5];="" }="" if="" (btype="=" builtindustrial)="" {="" graphicarray[cell]="indImage[lastDraw" %="" 6];="" }="" if="" (btype="=" builtcommerical)="" {="" graphicarray[cell]="comImage[lastDraw" %="" 6];="" }="" if="" (btype="=" zonedcommerical)="" {="" graphicarray[cell]="zonedCommericalImage;" }="" if="" (btype="=" zonedindustrial)="" {="" graphicarray[cell]="zonedIndustrialImage;" }="" if="" (btype="=" treetile)="" {="" graphicarray[cell]="treeImage;" }="" if="" (btype="=" road="" ||="" btype="=" roadwithpowerline)="" {="" boolean="" updown="false;" boolean="" leftright="false;" if="" (cell="" -="" 1=""> 0 && buildType[cell - 1] == road) {
//                            leftright = true;
//                        }
//                        if ((cell + 1) % MAPWIDTH != 0 && buildType[cell + 1] == road) {
//                            leftright = true;
//                        }
//                        if (cell - MAPWIDTH > 0 && buildType[cell - MAPWIDTH] == road) {
//                            updown = true;
//                        }
//                        if (cell + MAPWIDTH < arraysize="" &&="" buildtype[cell="" +="" mapwidth]="=" road)="" {="" updown="true;" }="" if="" (leftright="" &&="" updown)="" {="" graphicarray[cell]="roadImageAllway;" }="" else="" if="" (updown)="" {="" graphicarray[cell]="roadImageUpDown;" }="" else="" if="" (leftright)="" {="" graphicarray[cell]="roadImageLeftRight;" }="" else="" {="" graphicarray[cell]="roadImageAllway;" }="" }="" }="" end="" graphic="" array="" update="" mark="" power="" power="" propagation="" visitednodes.clear();="" do="" a="" power="" update="" if="" we="" need="" one="" -----------------="" *="" *="" slist="" functions="" as="" a="" stack="" with="" pointer="" start="" and="" j="" *="" start="" indicates="" the="" beginning="" of="" the="" stack="" and="" j="" the="" end="" since="" *="" we="" never="" visit="" more="" cells="" than="" there="" are="" nodes="" in="" the="" array="" this="" should="" work="" *="" bfs="" by="" appending="" to="" the="" stack.="" once="" the="" stack="" is="" empty="" we="" are="" done="" */="" if="" (btype="=" powerplant="" &&="" updatepower)="" {="" updatepower="false;" for="" (i="0;" i="">< arraysize;="" i++)="" {="" power[i]="false;" clear="" power="" array="" }="" power[cell]="POWERPLANTOUPUT;" power[cell]="true;" slist[0]="cell;" int="" start="0;" j="1;" end="" of="" stack="" while="" (start="" !="j)" {="" currcell="sList[start];" start++;="" for="" (i="0;" i="">< 4;="" i++)="" {="" neighbor="currCell" +="" rightleftdownup[i];="" all="" building="" types="" greater="" than="" transmitspower="" are="" allowed="" to="" pass="" electricity="" if="" (buildtype[neighbor]="">= TRANSMITSPOWER) {
//                                if (!power[neighbor]) {
//                                    power[neighbor] = true;
//                                    sList[j] = neighbor;
//                                    j++;
//                                }
//                            }
//                        }
//
//                    }
//                }
//                //End power update --------------------------------
//
//                //Update Land Values ----------------------------------
//                newLandValue[cell] = 0;
//                if (btype == waterTile) {
//                    newLandValue[cell] = 80;
//
//                } else if (btype == treeTile) {
//                    newLandValue[cell] = 50;
//
//                } else if (btype == builtCommerical) {
//                    newLandValue[cell] = 85;
//                } else if (btype == builtResidental) {
//                    newLandValue[cell] = 65;
//                } else if (btype == builtIndustrial) {
//                    newLandValue[cell] = 5;
//                } else if (btype == powerPlant) {
//                    newLandValue[cell] = -200;
//                } else if (btype >= TRANSMITSPOWER && !power[cell]) {
//                    newLandValue[cell] -= 30;
//
//                } else {
//                    newLandValue[cell] = landValue[cell];
//                }
//                //Start Precomputing stuff to for move-ins
//                hasRoad = -1;
//                added = 0;
//                for (i = -1; i < 2;="" i++)="" {="" for="" (j="-1;" j="">< 2;="" j++)="" {="" neighbor="cell" +="" i="" +="" j="" *="" mapwidth;="" if="" ((cell="" %="" mapwidth="" +="" i)="">< 0="" ||="" (cell="" %="" mapwidth="" +="" i)="">= MAPWIDTH) {
//                        //    continue;
//                        //}
//                        if (neighbor == cell) {
//                            continue;
//                        }
//                        if (neighbor < 0="" ||="" neighbor="">= ARRAYSIZE) {
//                            continue;
//                        }
//
//                        newLandValue[cell] += landValue[neighbor];
//                        added++;
//
//
//
//                    }
//                }
//                added++;
//
//                // Done with adding neighbors, Normalize
//                newLandValue[cell] /= added;
//                for (i = -2; i < 3="" &&="" hasroad="=" -1;="" i++)="" {="" for="" (j="-2;" j="">< 3="" &&="" hasroad="=" -1;="" j++)="" {="" neighbor="cell" +="" i="" +="" j="" *="" mapwidth;="" if="" ((cell="" %="" mapwidth="" +="" i)="">< 0="" ||="" (cell="" %="" mapwidth="" +="" i)="">= MAPWIDTH || neighbor < 0="" ||="" neighbor="">= ARRAYSIZE) {
//                            continue;
//                        }
//                        if (buildType[neighbor] == road) {
//                            hasRoad = neighbor;
//                        }
//                    }
//                }
//
//
//                // Does this square have power
//                boolean hasPower = false;
//                for (i = 0; i < 4;="" i++)="" {="" neighbor="cell" +="" rightleftdownup[i];="" if="" (neighbor="">< 0="" ||="" neighbor="">= ARRAYSIZE) {
//                        continue;
//                    }
//
//                    if (power[neighbor]) {
//                        hasPower = true;
//                    }
//                }
//
//
//                if (hasRoad >= 0 && hasPower) {
//
//                    // MOVE INS
//                    if (btype == zonedIndustrial) {
//                        if (demand[2] > 0) { // && cell has power) && has job
//                            buildType[cell] = builtIndustrial;
//                            jobs[cell] = 2;
//                            demand[2]--;
//                            demand[0]++;
//                            numInd++;
//                            graphicArray[cell] = gANEEDSUPDATE;
//
//                        }
//                    }
//
//
//
//                    if (btype == zonedResidental) {
//                        if (demand[0] > 0 && landValue[cell] > 40.0) { // && cell has power) && has job
//                            //Find Job
//                            // System.out.println("Starting Road Search");
//                            for (i = 0; i < arraysize;="" i++)="" {="" scheck[i]="-1;//Clear" backtrace="" }="" boolean="" foundjob="false;" int="" start="0;" slist[start]="hasRoad;" int="" end="1;" int="" jobnum="-1;" finding="" a="" job="" this="" should="" probably="" be="" checked="" more="" often="" and="" traffic="" recalculated="" ...="" while="" (start="" !="end" &&="" !foundjob)="" {="" int="" currcell="sList[start];" system.out.println(currcell);="" start++;="" for="" (i="-2;" i="">< 3;="" i++)="" {="" for="" (j="-2;" j="">< 3;="" j++)="" {="" neighbor="currCell" +="" i="" +="" j="" *="" mapwidth;="" if="" (jobs[neighbor]=""> 0) {
//                                            foundJob = true;
//                                            jobs[neighbor]--;
//                                            jobnum = currCell; //LAST ROAD TOUCHED
//                                            i = j = 3;
//                                        }
//                                    }
//                                }
//
//                                for (i = 0; i < 4;="" i++)="" {="" neighbor="currCell" +="" rightleftdownup[i];="" if="" (neighbor=""> 0 && (buildType[neighbor] == road || buildType[neighbor] == roadWithPowerLINE)) {
//                                        if (traffic[neighbor] < maxtraffic="" &&="" scheck[neighbor]="=" -1)="" {="" scheck[neighbor]="(short)" i;//="" direction="" of="" how="" we="" got="" to="" this="" square="" dp="" style="" slist[end]="neighbor;" end++;="" }="" }="" }="" }="" if="" (!foundjob)="" {="" system.out.println("no="" job="" found");="" system.out.println(visitednodes);="" continue;="" }="" population="" +="5;" while="" (jobnum="" !="hasRoad)" until="" we="" are="" back="" at="" the="" start="" walk="" backwards="" along="" the="" marks="" and="" do="" traffic="" {="" system.out.println(jobnum);="" traffic[jobnum]++;="" jobnum="" -="RIGHTLEFTDOWNUP[sCheck[jobnum]];" }="" for="" (i="0;" i="">< visitednodes.size();="" i++)="" {="" traffic[visitednodes.get(i)]++;="" }="" buildtype[cell]="builtResidental;" demand[0]--;="" demand[1]++;="" graphicarray[cell]="gANEEDSUPDATE;" }="" }="" if="" (btype="=" zonedcommerical)="" {="" if="" (demand[1]=""> 0 && landValue[cell] > 50.0) { // && cell has power) && has job
//                            buildType[cell] = builtCommerical;
//                            jobs[cell] = 2;
//                            demand[1]--;
//                            demand[2]++;
//                            numComm++;
//                            graphicArray[cell] = gANEEDSUPDATE;
//
//                        }
//                    }
//                }
//                //DONE with MOVE INS
//
//
//
//            }
//            lastCellUpdated = CELLEND;
//
//            //ALL CELLS UPDATED FLIP LAND VALUE ARRAYS
//            if (CELLEND == ARRAYSIZE) {
//                lastCellUpdated = 0;
//                float[] temp = landValue;
//                landValue = newLandValue;
//                newLandValue = temp;
//
//
//
//            }
//
//
//
//
//
//
//
//            int mouseCellX = -1;;
//            int mouseCellY = -1;
//            int mouseCell;
//            //Mouse Handeling
//            if (mx > SQUAREWIDTH * 7) {
//                //SCROLL CONTROLS
//                mouseCellX = mx / (SQUAREWIDTH) + VIEWPORTLEFT;
//                mouseCellY = my / (SQUAREHEIGHT) + VIEWPORTTOP;
//                mouseCell = (mouseCellX + mouseCellY * MAPWIDTH);
//                //Handle Scroll Stuff
//                scroll[0] = false;
//                scroll[2] = false;
//                //System.out.println("Mouse Location (" + mx + "," + my + ")");
//                if (mx < (scrollwidth="" +="" 7="" *="" scrollwidth))="" {="" scroll[0]="true;" scroll[1]="false;" system.out.println("scroll="" left");="" }="" else="" if="" (mx=""> (EDGE - SCROLLWIDTH)) {
//                    scroll[0] = true;
//                    scroll[1] = true;
//                }
//
//                if (my < scrollheight)="" {="" scroll[2]="true;" scroll[3]="false;" }="" else="" if="" (my=""> (BOTTOM - SCROLLWIDTH)) {
//                    scroll[2] = true;
//                    scroll[3] = true;
//                }
//
//
//                //SCROLL
//                if (scrollDelay < 0="" &&="" (scroll[0]="" ||="" scroll[2]))="" {="" if="" (scroll[0])="" {="" scrolldelay="45;" if="" (scroll[1])="" {="" viewportleft++;="" if="" (viewportleft=""> (MAPWIDTH - SQUAREPERWINDOW)) {
//                                VIEWPORTLEFT = (MAPWIDTH - SQUAREPERWINDOW);
//                            }
//                        } else {
//                            VIEWPORTLEFT--;
//                            if (VIEWPORTLEFT < -5)="" {="" viewportleft="-5;" }="" }="" }="" if="" (scroll[2])="" {="" scrolldelay="45;" if="" (scroll[3])="" {="" viewporttop++;="" if="" (viewporttop=""> (MAPHEIGHT - SQUAREPERWINDOW)) {
//                                VIEWPORTTOP = (MAPHEIGHT - SQUAREPERWINDOW);
//                            }
//                        } else {
//                            VIEWPORTTOP--;
//                            if (VIEWPORTTOP < 0)="" {="" viewporttop="0;" }="" }="" }="" }="" }="" else="" {="" mousecell="-1;" }="" now="" lets="" check="" if="" this="" cell="" is="" acceptable="" for="" zoning="" zoneok="true;" for="" 3by="" 3s="" if="" (controlselected="">= 0) {
//                int indBegin = CONTROLZONES[controlSelected] / 2;
//                int indEnd = CONTROLZONES[controlSelected] - indBegin;
//                for (i = -indBegin; i < indend;="" i++)="" {="" for="" (j="-indBegin;" j="">< indend;="" j++)="" {="" neighbor="mouseCell" +="" i="" +="" j="" *="" mapwidth;="" if="" (neighbor="">< 0="" ||="" neighbor="">= ARRAYSIZE) {
//                            zoneOK = false;
//                            continue;
//                        }
//                        if ((mouseCellX + i) < 0="" ||="" (mousecellx="" +="" i)=""> MAPWIDTH) {
//                            zoneOK = false;
//                            continue;
//
//                        }
//                        if (buildType[neighbor] == road && controlSelected == POWERLINECONTROL) {
//                            continue;
//
//                        }
//                        if (buildType[neighbor] == powerLines) {
//                            continue;
//                        }
//                        if ((buildType[neighbor] != noBuilding && buildType[neighbor] != treeTile) || !zoneOK) {
//                            zoneOK = false;
//                        }
//                    }
//                }
//            } else {
//                zoneOK = false;
//            }
//            //if (controlSelected == POWERPLANTCONTROL && numPlants == MAXPLANTS) {
//            //    zoneOK = false;
//            //}
//            /*
//             * if (controlSelected == BULLDOZECONTROL) { zoneOK = true; }
//             */
//
//
//            //Mouse Click
//            //System.out.println(mouseClick[0]+"  "+mouseClick[1]);
//            if (rightClick) {
//                //System.out.println("RIGHTCLICK");
//                controlSelected = NOSELECTION;
//                rightClick = leftClick = false;
//
//            }
//            if (leftClick) {
//                //Click HANDELING
//                //System.out.println("CLICK");
//                rightClick = leftClick = false;
//
//
//                if (mx < squarewidth="" *="" 7)="" {="" clicking="" in="" the="" control="" panel="" for="" (short="" control="0;" control="">< controllength;="" control++)="" {="" if="" (mx=""> CONTROLOFFSETS[2 * control]
//                                && mx < (controloffsets[2="" *="" control]="" +="" controlsize)="" &&="" my=""> (CONTROLOFFSETS[2 * control + 1])
//                                && my < (controloffsets[2="" *="" control="" +="" 1]="" +="" controlsize))="" {="" controlselected="control;" system.out.println("controlselected="" "="" +="" control);="" }="" }="" }="" else="" {="" if="" (zoneok="" &&="" controlzones[controlselected]="" *="" controlzones[controlselected]="" *="" 10="">< money)="" {="" *="" if="" (controlselected="=" bulldozecontrol)="" {="" if="" *="" (buildtype[mousecell]="=" powerplant)="" {="" numplants--;="" }="" *="" clearzone(mousecell);="" *="" *="" continue;="" }="" system.out.println("hit="" you="" two");="" */="" if="" (controlselected="=" powerlinecontrol="" &&="" buildtype[mousecell]="=" road)="" {="" buildtype[mousecell]="roadWithPowerLINE;" graphicarray[mousecell]="gANEEDSUPDATE;" continue;="" }="" if="" (controlselected="=" powerplantcontrol)="" {="" numplants++;="" }="" if="" (controlselected="=" roadcontrol)="" {="" numroads++;="" for="" (i="0;" i="">< 4;="" i++)="" {="" neighbor="mouseCell" +="" rightleftdownup[i];="" if="" (neighbor=""> 0 && buildType[neighbor] == road) {
//
//                                    graphicArray[neighbor] = gANEEDSUPDATE;
//                                }
//
//                            }
//                        }
//
//
//                        indBegin = CONTROLZONES[controlSelected] / 2;
//                        indEnd = CONTROLZONES[controlSelected] - indBegin;
//                        for (i = -indBegin; i < indend;="" i++)="" {="" for="" (j="-indBegin;" j="">< indend;="" j++)="" {="" neighbor="mouseCell" +="" i="" +="" j="" *="" mapwidth;="" buildtype[neighbor]="CONTROLZONETYPE[controlSelected];" graphicarray[neighbor]="gANEEDSUPDATE;" money="" -="10;" if="" (i="" !="-indBegin" &&="" i="" !="(indEnd" -="" 1)="" &&="" j="" !="-indBegin" &&="" j="" !="(indEnd" -="" 1))="" {="" edgearray[neighbor]="BUILDING;" }="" else="" {="" edgearray[neighbor]="BUILDINGEDGE;" }="" }="" }="" }="" }="" }="" if="" it="" is="" time="" to="" redraw="" lets="" do="" that="" ,="" otherwise="" lets="" try="" to="" process="" more="" cells="" if="" (nextframestarttime=""> System.nanoTime()) {
//                ticks++;
//                if (ticks % 3000 == 0) {
//                    money += net = population * 2 + numInd * 3 + numComm * 10 - numRoads * 7;
//                }
//                // Draw Background
//                g.setColor(Color.DARK_GRAY);
//
//                g.fillRect(0, 0, EDGE, BOTTOM);
//
//                //Now Render
//                //
//
//                for (short squareX = 7; squareX < squareperwindow;="" squarex++)="" {="" for="" (short="" squarey="0;" squarey="">< squareperwindow;="" squarey++)="" {="" squareindex="(squareY" +="" viewporttop)="" *="" mapwidth="" +="" viewportleft="" +="" squarex;="" gx="squareX" *="" squarewidth;="" gy="squareY" *="" squareheight;="" btype="buildType[squareIndex];" draw="" squares="" if="" (controlselected="=" landvaluecontrol)="" {="" float="" lvhue="(landValue[squareIndex]" (maxlandvalue="" *="" 4));="" if="" (lvhue="">< 0)="" {="" lvhue="0;" }="" g.setcolor(color.gethsbcolor(lvhue,="" .7f,="" .7f));="" g.fillrect(gx,="" gy,="" squarewidth,="" squareheight);="" }="" else="" if="" (controlselected="=" viewtrafficcontrol)="" {="" if="" (buildtype[squareindex]="" !="road" &&="" buildtype[squareindex]="" !="roadWithPowerLINE)" {="" continue;="" }="" float="" lvhue="0.25f" -="" ((float)="" traffic[squareindex]="" (maxtraffic="" *="" 4));="" if="" (lvhue="">< 0)="" {="" lvhue="0;" }="" g.setcolor(color.gethsbcolor(lvhue,="" .7f,="" .7f));="" g.fillrect(gx,="" gy,="" squarewidth,="" squareheight);="" }="" else="" {="" short="" grapint="graphicArray[squareIndex];" if="" (grapint="=" ganeedsupdate)="" {="" continue;="" }="" if=""><7) system.out.println(grapx="" +"="" "+="" grapy="" +="" "="" "+grapint);="" g.drawimage(sprites[grapint],="" gx,="" gy,="" squarewidth,="" squareheight,="" null);="" if="" (btype="=" powerlines="" ||="" btype="=" roadwithpowerline)="" {="" g.drawimage(sprites[powerlineimage],="" gx,="" gy,="" squarewidth,="" squareheight,="" null);="" }="" if="" ((ticks="" 80="" %="" 2="=" 0))="" {="" if="" (buildtype[squareindex]=""> TRANSMITSPOWER && !power[squareIndex]) {
//                                    g.drawImage(sprites[NOPOWERIMAGE], gX, gY, SQUAREWIDTH, SQUAREHEIGHT, null);
//                                }
//                                if (buildType[squareIndex] == road && traffic[squareIndex] >= MAXTRAFFIC) {
//                                    g.drawImage(sprites[(graphicArray[squareIndex] == roadImageUpDown ? TRAFFICUP : TRAFFICLEFT)], gX, gY, SQUAREWIDTH, SQUAREHEIGHT, null);
//                                }
//
//
//                            }
//
//                            g.setColor(defC[3]);
//                            g.drawRect(gX, gY, SQUAREWIDTH, SQUAREHEIGHT);
//
//                            //DEBUG STRINGS
//                            g.setColor(Color.WHITE);
//                            //g.drawString(Boolean.toString(power[((squareY + VIEWPORTTOP) * MAPWIDTH + VIEWPORTLEFT + squareX)]), squareX * SQUAREWIDTH, (squareY + 1) * SQUAREHEIGHT);
//                            //g.drawString(Integer.toString(traffic[((squareY + VIEWPORTTOP) * MAPWIDTH + VIEWPORTLEFT + squareX)]), squareX * SQUAREWIDTH, (squareY + 1) * SQUAREHEIGHT);
//                            //g.drawString(Float.toString(landValue[((squareY + VIEWPORTTOP) * MAPWIDTH + VIEWPORTLEFT + squareX)]), squareX * SQUAREWIDTH, (squareY + 1) * SQUAREHEIGHT);
//                            //g.drawString(Integer.toString(edgeArray[((squareY + VIEWPORTTOP) * MAPWIDTH + VIEWPORTLEFT + squareX)]), squareX * SQUAREWIDTH, (squareY + 1) * SQUAREHEIGHT);
//                            //g.drawString(Integer.toString((squareY + VIEWPORTTOP) * MAPWIDTH + VIEWPORTLEFT + squareX), squareX * SQUAREWIDTH, (squareY + 1) * SQUAREHEIGHT);
//                            //g.drawString(Integer.toString(graphicArray[((squareY + VIEWPORTTOP) * MAPWIDTH + VIEWPORTLEFT + squareX)]), squareX * SQUAREWIDTH, (squareY + 1) * SQUAREHEIGHT);
//                            //g.drawString(Integer.toString(jobs[((squareY + VIEWPORTTOP) * MAPWIDTH + VIEWPORTLEFT + squareX)]), squareX * SQUAREWIDTH, (squareY + 1) * SQUAREHEIGHT);
//
//                        }
//                    }
//                }
//                //Draw
//                for (short squareX = 7; squareX < squareperwindow;="" squarex++)="" {="" for="" (short="" squarey="0;" squarey="">< squareperwindow;="" squarey++)="" {="" int="" squareindex="(squareY" +="" viewporttop)="" *="" mapwidth="" +="" viewportleft="" +="" squarex;="" if="" (squareindex="=" mousecell="" &&="" controlselected="">= 0 && zoneOK) {
//                            g.setColor(defC[CONTROLCOLOR[controlSelected]]);
//                            int indSpread = CONTROLZONES[controlSelected] / 2;
//                            g.fill3DRect((squareX - indSpread) * SQUAREWIDTH, (squareY - indSpread) * SQUAREHEIGHT, (CONTROLZONES[controlSelected]) * SQUAREWIDTH, CONTROLZONES[controlSelected] * SQUAREHEIGHT, zoneOK);
//
//
//                        }
//                    }
//                }
//
//
//                //Draw Controls
//                g.setColor(Color.DARK_GRAY);
//
//                g.fillRect(0, 0, SQUAREWIDTH * 7, BOTTOM);
//                for (short control = 0; control < controllength;="" control++)="" {="" g.setcolor(defc[controlcolor[control]]);="" g.fill3drect(controloffsets[control="" *="" 2],="" controloffsets[control="" *="" 2="" +="" 1],="" controlsize,="" controlsize,="" (controlselected="=" control)="" false="" :="" true);="" g.setcolor(color.black);="" g.drawstring(character.tostring(controllabels[control]),="" controloffsets[control="" *="" 2]="" +="" controlsize="" 2,="" controloffsets[control="" *="" 2="" +="" 1]="" +="" controlsize="" 2);="" g.setcolor(color.pink);="" }="" g.setcolor(color.black);="" g.drawstring("r="" c="" i",="" 10,="" 180);="" g.setcolor(defc[controlcolor[rescontrol]]);="" g.fillrect(10,="" 170="" -="" demand[0],="" 10,="" (int)="" (demand[0]));="" g.setcolor(defc[controlcolor[comcontrol]]);="" g.fillrect(30,="" 170="" -="" demand[1],="" 10,="" (int)="" demand[1]);="" g.setcolor(defc[controlcolor[indcontrol]]);="" g.fillrect(50,="" 170="" -="" demand[2],="" 10,="" (int)="" demand[2]);="" g.setcolor(color.white);="" g.drawstring("$:"="" +="" money,="" 20,="" 260);="" g.drawstring("pop:"="" +="" population,="" 20,="" 280);="" g.drawstring("rs:"="" +="" numroads,="" 20,="" 300);="" g.drawstring("net$:"="" +="" net,="" 20,="" 320);="" g.fillrect(20,="" 360,="" (30)="" -="" (ticks="" %="" 3000)="" 100,="" 10);="" g.drawimage(ss,="" 0,="" 0,320,240,="" null);="" for="" (j="0;" j="">< mapheight;="" j++)="" {="" for="" (i="0;" i="">< mapwidth;="" i++)="" {="" neighbor="i" +="" j="" *="" mapwidth;="" int="" color="COLORS[10];" btype="buildType[neighbor];" if="" (btype="=" zonedcommerical="" ||="" btype="=" builtcommerical)="" {="" color="COLORS[2];" }="" if="" (btype="=" zonedresidental="" ||="" btype="=" builtresidental)="" {="" color="COLORS[0];" }="" if="" (btype="=" zonedindustrial="" ||="" btype="=" builtindustrial)="" {="" color="COLORS[9];" }="" if="" (btype="=" watertile)="" {="" color="COLORS[17];" }="" if="" (btype="=" road="" ||="" btype="=" roadwithpowerline="" ||="" btype="=" powerplant)="" {="" color="COLORS[5];" }="" image.setrgb(90="" +="" i,="" 250="" +="" j,="" color);="" }="" }="" g.drawrect(97="" +="" viewportleft,="" 250="" +="" viewporttop,="" 12,="" 20);="" and="" draw="" if="" (g2="=" null)="" {="" g2="(Graphics2D)" getgraphics();="" requestfocus();="" }="" else="" {="" g2.drawimage(image,="" 0,="" 0,="" edge,="" bottom,="" null);="" }="" }="" nextframestarttime="System.nanoTime()" +="" 1666666666;="" }="" }="" final="" short="" scrollwidth="30;" final="" short="" scrollheight="30;" @override="" public="" boolean="" handleevent(event="" mevent)="" {="" check="" for="" scroll="" switch="" (mevent.id)="" {="" case="" event.mouse_move:="" mx="mEvent.x;" my="mEvent.y;" break;="" case="" event.mouse_down:="" leftclick="true;" rightclick="(mEvent.modifiers" =="Event.META_MASK);" break;="" }="" return="" true;="" }=""></7)></=></=></=>
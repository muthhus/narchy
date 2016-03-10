package nars.util;

import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * Hsom Self Organizing Map by patham9
 */
public class Hsom {

    @NotNull
    final float[][][] links;
    @NotNull
    final float[] inputs;
    @NotNull
    final float[][] coords1;
    @NotNull
    final float[][] coords2;
    // final float[][][] vis;
    final int numInputs;
    final int SomSize;
    final boolean Leaky = true;
    float gamma;
    float eta = 0.1f;
    float outmul = 1.0f;
    int winnerx;
    int winnery;
    float Leak = 0.1f;
    float InMul = 1.0f;

    public Hsom(int numInputs, int SomSize, @NotNull Random rng) {
        links = new float[SomSize][SomSize][numInputs];
        // vis = new float[SomSize][SomSize][numInputs];
        inputs = new float[numInputs];
        coords1 = new float[SomSize][SomSize];
        coords2 = new float[SomSize][SomSize];
        gamma = SomSize / 2f;
        this.numInputs = numInputs;
        this.SomSize = SomSize;
        for (int i1 = 0; i1 < SomSize; i1++) {
            for (int i2 = 0; i2 < SomSize; i2++) {
                coords1[i1][i2] = (float) ((float) i1 * 1.0); // Kartenkoords
                coords2[i1][i2] = (float) ((float) i2 * 1.0);
            }
        }
        for (int x = 0; x < SomSize; x++) {
            for (int y = 0; y < SomSize; y++) {
                for (int z = 0; z < numInputs; z++) {
                    links[x][y][z] = (float) ((rng.nextFloat()/**
                     * 2.0-1.0
                     */
                    ) * 0.1);
                }
            }
        }
    }

    void input(float[] input) {
        int j;

        for (j = 0; j < numInputs; j++) {
            if (!Leaky) {
                this.inputs[j] = input[j] * InMul;
            } else {
                this.inputs[j] += (-Leak * this.inputs[j]) + input[j];
            }
        }
        float minv = Float.MAX_VALUE;
        for (int i1 = 0; i1 < SomSize; i1++) {
            for (int i2 = 0; i2 < SomSize; i2++) {
                float summe = 0.0f;
                float[] ll = links[i1][i2];
                for (j = 0; j < numInputs; j++) {
                    float ij = inputs[j];
                    float lljminij = ll[j] - ij;
                    // vis[i1][i2][j] = val;
                    summe += lljminij * lljminij;
                }
                if (summe <= minv) // get winner
                {
                    minv = summe;
                    winnerx = i1;
                    winnery = i2;
                }
            }
        }
    }

    public int winner() {
        return winnerx + (winnery * SomSize);
    }
    void get(float[] outarr) {
        int x = winnerx;
        int y = winnery;
        for (int i = 0; i < numInputs; i++) {
            outarr[i] = links[x][y][i] * outmul;
        }
    }

    float hsit(int i1, int i2) { // neighboorhood-function
        float[][] cc = this.coords1;
        int winnerx = this.winnerx;
        int winnery = this.winnery;
        float diff1 = (cc[i1][i2] - cc[winnerx][winnery])
                * (cc[i1][i2] - cc[winnerx][winnery]);
        float[][] dd = this.coords2;
        float diff2 = (dd[i1][i2] - dd[winnerx][winnery])
                * (dd[i1][i2] - dd[winnerx][winnery]);
        float gammaSq = 2 * gamma * gamma;
        return (1.0f / ((float) Math.sqrt(Math.PI * gammaSq)))
                * ((float) Math.exp((diff1 + diff2) / (-gammaSq)));
    }

    /**
     * inputs and trains it
     */
    public void learn(float[] input) {
        input(input);

        float eta = this.eta;
        if (eta != 0.0f) {
            float[][][] l = this.links;
            float[] ii = this.inputs;

            for (int i1 = 0; i1 < SomSize; i1++) {
                for (int i2 = 0; i2 < SomSize; i2++) {
                    float h = hsit(i1, i2);
                    float[] ll = l[i1][i2];
                    for (int j = 0; j < numInputs; j++) { // adaption
                        float lx = l[i1][i2][j];
                        ll[j] = lx + (eta * h * (ii[j] - lx));
                    }
                }
            }
        }
    }

    void set(float AdaptionStrenght, float AdaptioRadius) {
        eta = AdaptionStrenght;
        gamma = AdaptioRadius;
    }

    // static int quantify(float val, int quantsteps) {
    // float step = 1 / ((float) quantsteps);
    // float wander = 0.0f;
    // int ind = -1;
    // while (wander <= val) {
    // wander += step;
    // ind++;
    // }
    // return ind;
    // }
    // void Draw(int x,int y,int RenderSize)
    // {
    // hsom_DrawSOM(som,RenderSize,x,y+RenderSize*6,false,0);
    // pushMatrix();
    // translate(x,y);
    // hamlib.Draw1DLine(som.inputs,10);
    // translate(0,10);
    // translate(0,RenderSize);
    // for(int i=0;i<nStates;i++)
    // {
    // for(int j=0;j<nStates;j++)
    // {
    // for(int a=0;a<nActions;a++)
    // {
    // hamlib.FillDependendOnVal(Q[i][j][a]);
    // rect((nStates+1)*RenderSize+i*RenderSize+(a*(nStates+1)*RenderSize),j*RenderSize,RenderSize,RenderSize);
    // }
    // }
    // }
    // popMatrix();
    // }
    // String GetWinnerCoordinatesWordFromAnalogInput(float[] input) {
    // learn(input);
    // return "x" + String.valueOf(winnerx) + "y" + String.valueOf(winnery);
    // }
    // void GetActivationForRendering(float[][] input, boolean
    // forSpecialInput, int specialInputIndex) {
    // if (input == null) {
    // input = new float[SomSize][SomSize];
    // }
    // for (int x = 0; x < SomSize; x++) {
    // for (int y = 0; y < SomSize; y++) {
    // float curval = (float) 0.0;
    // if (!forSpecialInput) {
    // for (int i = 0; i < numInputs; i++) {
    // curval += vis[x][y][i];
    // }
    // } else {
    // curval = vis[x][y][specialInputIndex];
    // }
    // input[x][y] = curval;
    // }
    // }
    //
    // //minimum for better visualisation:
    // float mini = 99999999;
    // float maxi = -99999999;
    // for (int x = 0; x < SomSize; x++) {
    // for (int y = 0; y < SomSize; y++) {
    // float t = input[x][y];
    // if (t < mini) {
    // mini = t;
    // }
    // if (t > maxi) {
    // maxi = t;
    // }
    // }
    // }
    // float diff = maxi - mini;
    // for (int x = 0; x < SomSize; x++) {
    // for (int y = 0; y < SomSize; y++) {
    // input[x][y] = (float) ((input[x][y] /*- mini*/) /
    // Math.max(0.00000001, diff));
    // }
    // }
    // }
}

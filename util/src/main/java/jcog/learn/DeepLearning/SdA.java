package jcog.learn.DeepLearning;

import java.util.Random;

import static jcog.learn.DeepLearning.utils.sigmoid;

public class SdA {
    public int n_ins;
    public int[] hidden_layer_sizes;
    public int outs;
    public int n_layers;
    public HiddenLayer[] sigmoid_layers;
    public dA[] dA_layers;
    public LogisticRegression log_layer;
    public Random rng;


    public SdA(int n_ins, int[] hidden_layer_sizes, int outs, Random rng) {
        int ins;

        this.n_ins = n_ins;
        this.hidden_layer_sizes = hidden_layer_sizes;
        this.outs = outs;
        this.n_layers = hidden_layer_sizes.length;

        this.sigmoid_layers = new HiddenLayer[n_layers];
        this.dA_layers = new dA[n_layers];

        if(rng == null)	this.rng = new Random(1234);
        else this.rng = rng;

        // construct multi-layer
        for(int i=0; i<this.n_layers; i++) {
            if(i == 0) {
                ins = this.n_ins;
            } else {
                ins = this.hidden_layer_sizes[i-1];
            }

            // construct sigmoid_layer
            this.sigmoid_layers[i] = new HiddenLayer(ins, this.hidden_layer_sizes[i], null, null, rng, null);

            // construct dA_layer
            this.dA_layers[i] = new dA(ins, this.hidden_layer_sizes[i], this.sigmoid_layers[i].W, this.sigmoid_layers[i].b, null, rng);
        }

        // layer for output using Logistic Regression
        this.log_layer = new LogisticRegression(this.hidden_layer_sizes[this.n_layers-1], this.outs);
    }

    public double[] pretrain(double[] train_X, double lr, double corruption_level, int epochs) {
        double[] y = new double[outs];
        pretrain(new double[][] { train_X }, lr, corruption_level, epochs);
        return dA_layers[n_layers-1].encode(train_X, y);
    }

    public void pretrain(double[][] train_X, double lr, double corruption_level, int epochs) {
        double[] layer_input = new double[0];
        int prev_layer_input_size;
        double[] prev_layer_input;
                int N = train_X.length;

        for(int i=0; i<n_layers; i++) {  // layer-wise
            for(int epoch=0; epoch<epochs; epoch++) {  // training epochs
                for(int n = 0; n< N; n++) {  // input x1...xN
                    // layer input
                    for(int l=0; l<=i; l++) {

                        if(l == 0) {
                            layer_input = new double[n_ins];
                            for(int j=0; j<n_ins; j++) layer_input[j] = train_X[n][j];
                        } else {
                            if(l == 1) prev_layer_input_size = n_ins;
                            else prev_layer_input_size = hidden_layer_sizes[l-2];

                            prev_layer_input = new double[prev_layer_input_size];
                            for(int j=0; j<prev_layer_input_size; j++) prev_layer_input[j] = layer_input[j];

                            layer_input = new double[hidden_layer_sizes[l-1]];

                            sigmoid_layers[l-1].
                                    forward(prev_layer_input, layer_input);
                                    //sample_h_given_v(prev_layer_input, layer_input);
                        }
                    }

                    dA_layers[i].train(layer_input, lr/N, corruption_level);
                }
            }
        }

    }

    public void finetune(double[][] train_X, double[][] train_Y, double lr, int epochs) {
        double[] layer_input = new double[0];
        // int prev_layer_input_size;
        double[] prev_layer_input = new double[0];

        for(int epoch=0; epoch<epochs; epoch++) {
            int N = train_X.length;
            for(int n = 0; n< N; n++) {

                // layer input
                for(int i=0; i<n_layers; i++) {
                    if(i == 0) {
                        prev_layer_input = new double[n_ins];
                        for(int j=0; j<n_ins; j++) prev_layer_input[j] = train_X[n][j];
                    } else {
                        prev_layer_input = new double[hidden_layer_sizes[i-1]];
                        for(int j=0; j<hidden_layer_sizes[i-1]; j++) prev_layer_input[j] = layer_input[j];
                    }

                    layer_input = new double[hidden_layer_sizes[i]];
                    sigmoid_layers[i].
                            forward(prev_layer_input, layer_input);
                            //sample_h_given_v(prev_layer_input, layer_input);
                }

                log_layer.train(layer_input, train_Y[n], lr/N);
            }
            // lr *= 0.95;
        }
    }

    public void predict(double[] x, double[] y) {
        double[] layer_input = new double[0];
        // int prev_layer_input_size;
        double[] prev_layer_input = new double[n_ins];
        for(int j=0; j<n_ins; j++) prev_layer_input[j] = x[j];

        double linear_output;


        // layer activation
        for(int i=0; i<n_layers; i++) {
            layer_input = new double[sigmoid_layers[i].n_out];

            for(int k=0; k<sigmoid_layers[i].n_out; k++) {
                linear_output = 0.0;

                for(int j=0; j<sigmoid_layers[i].n_in; j++) {
                    linear_output += sigmoid_layers[i].W[k][j] * prev_layer_input[j];
                }
                linear_output += sigmoid_layers[i].b[k];
                layer_input[k] = sigmoid(linear_output);
            }

            if(i < n_layers-1) {
                prev_layer_input = new double[sigmoid_layers[i].n_out];
                for(int j=0; j<sigmoid_layers[i].n_out; j++)
                    prev_layer_input[j] = layer_input[j];
            }
        }

        for(int i=0; i<log_layer.n_out; i++) {
            y[i] = 0;
            for(int j=0; j<log_layer.n_in; j++) {
                y[i] += log_layer.W[i][j] * layer_input[j];
            }
            y[i] += log_layer.b[i];
        }

        log_layer.softmax(y);
    }


    private static void test_sda() {
        Random rng = new Random(123);

        double pretrain_lr = 0.1;
        double corruption_level = 0.3;
        int pretraining_epochs = 1000;
        double finetune_lr = 0.1;
        int finetune_epochs = 500;

        int test_N = 4;
        int n_ins = 28;
        int n_outs = 2;
        int[] hidden_layer_sizes = {15, 15};


        // training data
        double[][] train_X = {
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {1, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 0, 1},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 1, 1, 1, 0, 1, 1, 0, 1, 1},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1, 0, 1, 1, 1},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1}
        };

        double[][] train_Y = {
                {1, 0},
                {1, 0},
                {1, 0},
                {1, 0},
                {1, 0},
                {0, 1},
                {0, 1},
                {0, 1},
                {0, 1},
                {0, 1}
        };

        // construct SdA
        SdA sda = new SdA(n_ins, hidden_layer_sizes, n_outs, rng);

        // pretrain
        sda.pretrain(train_X, pretrain_lr, corruption_level, pretraining_epochs);

        // finetune
        sda.finetune(train_X, train_Y, finetune_lr, finetune_epochs);


        // test data
        double[][] test_X = {
                {1, 1, 1, 1, 1, 1, 0, 1, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {1, 1, 1, 1, 1, 0, 1, 0, 1, 1, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 1, 1, 0, 1, 1, 1, 1, 1},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1}
        };

        double[][] test_Y = new double[test_N][n_outs];

        // test
        for(int i=0; i<test_N; i++) {
            sda.predict(test_X[i], test_Y[i]);
            for(int j=0; j<n_outs; j++) {
                System.out.print(test_Y[i][j] + " ");
            }
            System.out.println();
        }
    }

    /** encode */
    public double[] put(double[] x, double learnRate) {
        return pretrain(x, learnRate, 0, 1);
//
//        double[] output = new double[outs];
//        predict(x, output);
//        return output;
    }


    public static void main(String[] args) {
        test_sda();
    }

}

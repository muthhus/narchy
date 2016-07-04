package nars.vision;


public interface PixelCamera {

    void update(PerPixelRGB p);

    static float rgbToMono(int r, int g, int b) {
        return (r+g+b)/256f/3f;
    }


    @FunctionalInterface interface PerPixelRGB {
        void pixel(int x, int y, int aRGB);
    }
    @FunctionalInterface interface PerPixelMono {
        void pixel(int x, int y, float whiteLevel);
    }
    @FunctionalInterface interface PerIndexMono {
        void pixel(int index, float whiteLevel);
    }

}

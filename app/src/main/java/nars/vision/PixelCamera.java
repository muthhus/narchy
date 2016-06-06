package nars.vision;


public interface PixelCamera {

    void update(PerPixel p);

    @FunctionalInterface interface PerPixel {
        void pixel(int x, int y, int rgb);
    }

}

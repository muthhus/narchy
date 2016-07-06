package nars.vision;


public interface PixelCamera {

    void update(PerPixelRGB p);

    static float rgbToMono(int r, int g, int b) {
        return (r+g+b)/255f/3f;
    }

    int width();
    int height();

    @FunctionalInterface interface PerPixelRGB {
        void pixel(int x, int y, int aRGB);
    }
    @FunctionalInterface interface PerPixelRGBf {
        void pixel(int x, int y, float r, float g, float b, float a);
    }
    @FunctionalInterface interface PerPixelMono {
        void pixel(int x, int y, float whiteLevel);
    }
    @FunctionalInterface interface PerIndexMono {
        void pixel(int index, float whiteLevel);
    }

    default void update(PerPixelRGBf m) {
        update((x,y,p)-> {
            intToFloat(m, x, y, p);
        });
    }

    default void intToFloat(PerPixelRGBf m, int x, int y, int p) {
        //int a = (p & 0xff000000) >> 24;
        int a = 255;
        float r = decodeRed(p);
        float g = decodeGreen(p);
        float b = decodeBlue(p);
        m.pixel(x, y, r, g, b, a/255f);
    }

    default float decodeRed(int p) {
        return ((p & 0x00ff0000) >> 16)/255f;
    }
    default float decodeGreen(int p) {
        return ((p & 0x0000ff00) >> 8)/255f;
    }
    default float decodeBlue(int p) {
        return ((p & 0x000000ff))/255f;
    }

    default void updateMono(PerPixelMono m) {
        update((x,y,p)-> {
            int r = (p & 0x00ff0000) >> 16;
            int g = (p & 0x0000ff00) >> 8;
            int b = (p & 0x000000ff);

            m.pixel(x, y, ((r+g+b) / 256f)/3f);
        });
    }


    default void updateMono(PerIndexMono m) {
        updateMono((x,y,p)-> {
            m.pixel(width() * y + x, p);
        });
    }

}

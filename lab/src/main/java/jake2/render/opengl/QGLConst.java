package jake2.render.opengl;


public interface QGLConst {

    /*
     * alpha functions
     */
    int GL_NEVER = 0x0200;

    int GL_LESS = 0x0201;

    int GL_EQUAL = 0x0202;

    int GL_LEQUAL = 0x0203;

    int GL_GREATER = 0x0204;

    int GL_NOTEQUAL = 0x0205;

    int GL_GEQUAL = 0x0206;

    int GL_ALWAYS = 0x0207;

    /*
     * attribute masks
     */
    int GL_DEPTH_BUFFER_BIT = 0x00000100;

    int GL_STENCIL_BUFFER_BIT = 0x00000400;

    int GL_COLOR_BUFFER_BIT = 0x00004000;

    /*
     * begin modes
     */
    int GL_POINTS = 0x0000;

    int GL_LINES = 0x0001;

    int GL_LINE_LOOP = 0x0002;

    int GL_LINE_STRIP = 0x0003;

    int GL_TRIANGLES = 0x0004;

    int GL_TRIANGLE_STRIP = 0x0005;

    int GL_TRIANGLE_FAN = 0x0006;

    int GL_QUADS = 0x0007;

    int GL_QUAD_STRIP = 0x0008;

    int GL_POLYGON = 0x0009;

    /*
     * blending factors
     */
    int GL_ZERO = 0;

    int GL_ONE = 1;

    int GL_SRC_COLOR = 0x0300;

    int GL_ONE_MINUS_SRC_COLOR = 0x0301;

    int GL_SRC_ALPHA = 0x0302;

    int GL_ONE_MINUS_SRC_ALPHA = 0x0303;

    int GL_DST_ALPHA = 0x0304;

    int GL_ONE_MINUS_DST_ALPHA = 0x0305;

    /*
     * boolean
     */
    int GL_TRUE = 1;

    int GL_FALSE = 0;

    /*
     * data types
     */
    int GL_BYTE = 0x1400;

    int GL_UNSIGNED_BYTE = 0x1401;

    int GL_SHORT = 0x1402;

    int GL_UNSIGNED_SHORT = 0x1403;

    int GL_INT = 0x1404;

    int GL_UNSIGNED_INT = 0x1405;

    int GL_FLOAT = 0x1406;

    /*
     * draw buffer modes
     */
    int GL_FRONT = 0x0404;

    int GL_BACK = 0x0405;

    int GL_FRONT_AND_BACK = 0x0408;

    /*
     * errors
     */
    int GL_NO_ERROR = 0;

    int GL_POINT_SMOOTH = 0x0B10;

    int GL_CULL_FACE = 0x0B44;

    int GL_DEPTH_TEST = 0x0B71;

    int GL_MODELVIEW_MATRIX = 0x0BA6;

    int GL_ALPHA_TEST = 0x0BC0;

    int GL_BLEND = 0x0BE2;

    int GL_SCISSOR_TEST = 0x0C11;

    int GL_PACK_ALIGNMENT = 0x0D05;

    int GL_TEXTURE_2D = 0x0DE1;

    /*
     * hints
     */
    int GL_PERSPECTIVE_CORRECTION_HINT = 0x0C50;

    int GL_DONT_CARE = 0x1100;

    int GL_FASTEST = 0x1101;

    int GL_NICEST = 0x1102;

    /*
     * matrix modes
     */
    int GL_MODELVIEW = 0x1700;

    int GL_PROJECTION = 0x1701;

    /*
     * pixel formats
     */
    int GL_COLOR_INDEX = 0x1900;

    int GL_RED = 0x1903;

    int GL_GREEN = 0x1904;

    int GL_BLUE = 0x1905;

    int GL_ALPHA = 0x1906;

    int GL_RGB = 0x1907;

    int GL_RGBA = 0x1908;

    int GL_LUMINANCE = 0x1909;

    int GL_LUMINANCE_ALPHA = 0x190A;

    /*
     * polygon modes
     */

    int GL_POINT = 0x1B00;

    int GL_LINE = 0x1B01;

    int GL_FILL = 0x1B02;

    /*
     * shading models
     */
    int GL_FLAT = 0x1D00;

    int GL_SMOOTH = 0x1D01;

    int GL_REPLACE = 0x1E01;

    /*
     * string names
     */
    int GL_VENDOR = 0x1F00;

    int GL_RENDERER = 0x1F01;

    int GL_VERSION = 0x1F02;

    int GL_EXTENSIONS = 0x1F03;

    /*
     * TextureEnvMode
     */
    int GL_MODULATE = 0x2100;

    /*
     * TextureEnvParameter
     */
    int GL_TEXTURE_ENV_MODE = 0x2200;

    int GL_TEXTURE_ENV_COLOR = 0x2201;

    /*
     * TextureEnvTarget
     */
    int GL_TEXTURE_ENV = 0x2300;

    int GL_NEAREST = 0x2600;

    int GL_LINEAR = 0x2601;

    int GL_NEAREST_MIPMAP_NEAREST = 0x2700;

    int GL_LINEAR_MIPMAP_NEAREST = 0x2701;

    int GL_NEAREST_MIPMAP_LINEAR = 0x2702;

    int GL_LINEAR_MIPMAP_LINEAR = 0x2703;

    /*
     * TextureParameterName
     */
    int GL_TEXTURE_MAG_FILTER = 0x2800;

    int GL_TEXTURE_MIN_FILTER = 0x2801;

    int GL_TEXTURE_WRAP_S = 0x2802;

    int GL_TEXTURE_WRAP_T = 0x2803;

    /*
     * TextureWrapMode
     */
    int GL_CLAMP = 0x2900;

    int GL_REPEAT = 0x2901;

    /*
     * texture
     */
    int GL_LUMINANCE8 = 0x8040;

    int GL_INTENSITY8 = 0x804B;

    int GL_R3_G3_B2 = 0x2A10;

    int GL_RGB4 = 0x804F;

    int GL_RGB5 = 0x8050;

    int GL_RGB8 = 0x8051;

    int GL_RGBA2 = 0x8055;

    int GL_RGBA4 = 0x8056;

    int GL_RGB5_A1 = 0x8057;

    int GL_RGBA8 = 0x8058;

    /*
     * vertex arrays
     */
    int GL_VERTEX_ARRAY = 0x8074;

    int GL_COLOR_ARRAY = 0x8076;

    int GL_TEXTURE_COORD_ARRAY = 0x8078;

    int GL_T2F_V3F = 0x2A27;

    /*
     * OpenGL 1.2, 1.3 constants
     */
    int GL_SHARED_TEXTURE_PALETTE_EXT = 0x81FB;

    int GL_TEXTURE0 = 0x84C0;

    int GL_TEXTURE1 = 0x84C1;

    int GL_TEXTURE0_ARB = 0x84C0;

    int GL_TEXTURE1_ARB = 0x84C1;

    int GL_BGR = 0x80E0;

    int GL_BGRA = 0x80E1;

    /*
     * point parameters
     */
    int GL_POINT_SIZE_MIN_EXT = 0x8126;

    int GL_POINT_SIZE_MAX_EXT = 0x8127;

    int GL_POINT_FADE_THRESHOLD_SIZE_EXT = 0x8128;

    int GL_DISTANCE_ATTENUATION_EXT = 0x8129;

}
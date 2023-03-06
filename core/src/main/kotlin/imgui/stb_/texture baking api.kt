package imgui.stb_

//////////////////////////////////////////////////////////////////////////////
//
// TEXTURE BAKING API
//
// If you use this API, you only have to call two functions ever.
//

//    typedef struct
//            {
//                unsigned short x0,y0,x1,y1; // coordinates of bbox in bitmap
//                float xoff,yoff,xadvance;
//            } stbtt_bakedchar;
//
//    STBTT_DEF int stbtt_BakeFontBitmap(const unsigned char *data, int offset,  // font location (use offset=0 for plain .ttf)
//            float pixel_height,                     // height of font in pixels
//            unsigned char *pixels, int pw, int ph,  // bitmap to be filled in
//            int first_char, int num_chars,          // characters to bake
//            stbtt_bakedchar *chardata);             // you allocate this, it's num_chars long
// if return is positive, the first unused row of the bitmap
// if return is negative, returns the negative of the number of characters that fit
// if return is 0, no characters fit and no rows were used
// This uses a very crappy packing.

class AlignedQuad {

    // top-left

    var x0 = 0f
    var y0 = 0f
    var s0 = 0f
    var t0 = 0f

    // bottom-right

    var x1 = 0f
    var y1 = 0f
    var s1 = 0f
    var t1 = 0f
}

//    STBTT_DEF void stbtt_GetBakedQuad(const stbtt_bakedchar *chardata, int pw, int ph,  // same data as above
//            int char_index,             // character to display
//            float *xpos, float *ypos,   // pointers to current position in screen pixel space
//            stbtt_aligned_quad *q,      // output: quad to draw
//            int opengl_fillrule);       // true if opengl fill rule; false if DX9 or earlier
// Call GetBakedQuad with char_index = 'character - first_char', and it
// creates the quad you need to draw and advances the current position.
//
// The coordinate system used assumes y increases downwards.
//
// Characters will extend both above and below the current position;
// see discussion of "BASELINE" above.
//
// It's inefficient; you might want to c&p it and optimize it.
//
//    STBTT_DEF void stbtt_GetScaledFontVMetrics(const unsigned char *fontdata, int index, float size, float *ascent, float *descent, float *lineGap);
//// Query the font vertical metrics without having to create a font first.
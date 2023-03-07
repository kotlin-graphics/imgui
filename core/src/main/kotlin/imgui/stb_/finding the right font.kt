package imgui.stb_

//////////////////////////////////////////////////////////////////////////////
//
// Finding the right font...
//
// You should really just solve this offline, keep your own tables
// of what font is what, and don't try to get it out of the .ttf file.
// That's because getting it out of the .ttf file is really hard, because
// the names in the file can appear in many possible encodings, in many
// possible languages, and e.g. if you need a case-insensitive comparison,
// the details of that depend on the encoding & language in a complex way
// (actually underspecified in truetype, but also gigantic).
//
// But you can use the provided functions in two possible ways:
//     stbtt_FindMatchingFont() will use *case-sensitive* comparisons on
//             unicode-encoded names to try to find the font you want;
//             you can run this before calling stbtt_InitFont()
//
//     stbtt_GetFontNameString() lets you get any of the various strings
//             from the file yourself and do your own comparisons on them.
//             You have to have called stbtt_InitFont() first.


//STBTT_DEF int stbtt_FindMatchingFont(const unsigned char *fontdata, const char *name, int flags);
//// returns the offset (not index) of the font that matches, or -1 if none
////   if you use STBTT_MACSTYLE_DONTCARE, use a font name like "Arial Bold".
////   if you use any other flag, use a font name like "Arial"; this checks
////     the 'macStyle' header field; i don't know if fonts set this consistently
//#define STBTT_MACSTYLE_DONTCARE     0
//#define STBTT_MACSTYLE_BOLD         1
//#define STBTT_MACSTYLE_ITALIC       2
//#define STBTT_MACSTYLE_UNDERSCORE   4
//#define STBTT_MACSTYLE_NONE         8   // <= not same as 0, this makes us check the bitfield is 0
//
//STBTT_DEF int stbtt_CompareUTF8toUTF16_bigendian(const char *s1, int len1, const char *s2, int len2);
//// returns 1/0 whether the first string interpreted as utf8 is identical to
//// the second string interpreted as big-endian utf16... useful for strings from next func
//
//STBTT_DEF const char *stbtt_GetFontNameString(const stbtt_fontinfo *font, int *length, int platformID, int encodingID, int languageID, int nameID);
//// returns the string (which may be big-endian double byte, e.g. for unicode)
//// and puts the length in bytes in *length.
//
//// some of the values for the IDs are below; for more see the truetype spec:
////     http://developer.apple.com/textfonts/TTRefMan/RM06/Chap6name.html
////     http://www.microsoft.com/typography/otspec/name.htm

enum class PlatformID {
    UNICODE, MAC, ISO, MICROSOFT;

    val i = ordinal
}

/** encodingID for STBTT_PLATFORM_ID_UNICODE */
enum class UnicodeEID {
    UNICODE_1_0, UNICODE_1_1, ISO_10646, UNICODE_2_0_BMP, UNICODE_2_0_FULL;

    val i = ordinal
}

/** encodingID for STBTT_PLATFORM_ID_MICROSOFT */
enum class MS_EID(val i: Int) { SYMBOL(0), UNICODE_BMP(1), SHIFTJIS(2), UNICODE_FULL(10) }

/** encodingID for STBTT_PLATFORM_ID_MAC; same as Script Manager codes */
enum class MAC_EID {
    ROMAN, JAPANESE, CHINESE_TRAD, KOREAN, ARABIC, HEBREW, GREEK, RUSSIAN;

    val i = ordinal
}

/** languageID for STBTT_PLATFORM_ID_MICROSOFT; same as LCID... */
enum class MS_LANG(val i: Int) {
    // problematic because there are e.g. 16 english LCIDs and 16 arabic LCIDs
    ENGLISH(0x0409),
    ITALIAN(0x0410), CHINESE(0x0804), JAPANESE(0x0411), DUTCH(0x0413), KOREAN(0x0412),
    FRENCH(0x040c), RUSSIAN(0x0419), GERMAN(0x0407), SPANISH(0x0409), HEBREW(0x040d), SWEDISH(0x041D)
}

enum class MAC_LANG(val i: Int) { // languageID for STBTT_PLATFORM_ID_MAC
    ENGLISH(0), JAPANESE(11),
    ARABIC(12), KOREAN(23),
    DUTCH(4), RUSSIAN(32),
    FRENCH(1), SPANISH(6),
    GERMAN(2), SWEDISH(5),
    HEBREW(10), CHINESE_SIMPLIFIED(33),
    ITALIAN(3), CHINESE_TRAD(19)
}
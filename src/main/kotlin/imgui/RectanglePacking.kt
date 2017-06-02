package imgui

object RectanglePacking {

    /** // 16 bytes, nominally  */
    class Rect    {
        /** reserved for your use:  */
        var id = 0

        /** input:  */
        var w = 0
        /** input:  */
        var h = 0

        /** output:  */
        var x = 0
        /** output:  */
        var y = 0

        /** non-zero if valid packing   */
        var wasPacked = 0
    }
}
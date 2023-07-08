package plot.api

interface Getter {
    val count: Int
    operator fun get(idx: Int): PlotPoint
}

abstract class GetterXY<T>: Getter {
    abstract val x: T
    abstract val y: T
}

class GetterXY_float(override val x: FloatArray,
                     override val y: FloatArray): GetterXY<FloatArray>() {
    override val count: Int
        get() = x.size

    override fun get(idx: Int): PlotPoint = PlotPoint(x[idx], y[idx])
}

class GetterOverrideY(val getter: Getter, val y: Double): Getter {
    override val count: Int
        get() = getter.count

    override fun get(idx: Int): PlotPoint = getter[idx].also { it.y = y }
}

//template <typename _Getter>
class GetterLoop(val getter: Getter): Getter {

    override val count = getter.count + 1

    override fun get(idx: Int): PlotPoint = getter[idx % (count - 1)]
}
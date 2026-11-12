package app.simple.felicity.engine.jamesdsp

/**
 * JNI bridge to the bundled JamesDSP native engine (see
 * `src/main/cpp/jamesdsp/` for the engine sources and
 * `src/main/cpp/wrapper/JamesDspWrapper.cpp` for the JNI glue).
 *
 * Ported from RootlessJamesDSP's `interop.JamesDspWrapper`. Every `external`
 * signature below is unchanged from upstream — only the package path changed,
 * which is why the native side's JNI symbol names
 * (`Java_app_simple_felicity_engine_jamesdsp_JamesDspWrapper_*`) had to be
 * renamed to match. Do not rename functions here without renaming the
 * corresponding native symbol.
 */
typealias JamesDspHandle = Long

object JamesDspWrapper {
    // Memory management
    external fun alloc(callbacks: JamesDspCallbacks): JamesDspHandle
    external fun free(self: JamesDspHandle)
    external fun isHandleValid(self: JamesDspHandle): Boolean

    // Benchmarking
    external fun getBenchmarkSize(): Int
    external fun runBenchmark(c0: DoubleArray, c1: DoubleArray)
    external fun loadBenchmark(c0: DoubleArray, c1: DoubleArray)

    // Processing (interleaved). The native engine hard-assumes stereo
    // interleaved PCM (processFloatMultiplexd divides the input length by 2
    // internally) — callers must only invoke this for 2-channel audio.
    external fun processInt16(self: JamesDspHandle, input: ShortArray, output: ShortArray, offset: Int = -1, length: Int = -1)
    external fun processInt8U24(self: JamesDspHandle, input: IntArray): IntArray
    external fun processInt24Packed(self: JamesDspHandle, input: BooleanArray): BooleanArray
    external fun processInt32(self: JamesDspHandle, input: IntArray, output: IntArray, offset: Int = -1, length: Int = -1)
    external fun processFloat(self: JamesDspHandle, input: FloatArray, output: FloatArray, offset: Int = -1, length: Int = -1)

    // Engine config
    external fun setSamplingRate(self: JamesDspHandle, sampleRate: Float, forceRefresh: Boolean)

    // Effect config — one call per effect module, mirroring the native
    // engine's own effect boundaries (Effects/*.c in the jamesdsp module).
    external fun setLimiter(self: JamesDspHandle, threshold: Float, release: Float): Boolean
    external fun setPostGain(self: JamesDspHandle, postGain: Float): Boolean
    external fun setMultiEqualizer(self: JamesDspHandle, enable: Boolean, filterType: Int, interpolationMode: Int, bands: DoubleArray): Boolean
    external fun setVdc(self: JamesDspHandle, enable: Boolean, vdcContents: String): Boolean
    external fun setCompander(self: JamesDspHandle, enable: Boolean, timeConstant: Float, granularity: Int, tfResolution: Int, bands: DoubleArray): Boolean
    external fun setReverb(self: JamesDspHandle, enable: Boolean, preset: Int): Boolean
    external fun setConvolver(self: JamesDspHandle, enable: Boolean, impulseResponse: FloatArray, irChannels: Int, irFrames: Int): Boolean
    external fun setGraphicEq(self: JamesDspHandle, enable: Boolean, graphicEq: String): Boolean
    external fun setCrossfeed(self: JamesDspHandle, enable: Boolean, mode: Int, customFcut: Int, customFeed: Int): Boolean
    external fun setBassBoost(self: JamesDspHandle, enable: Boolean, maxGain: Float): Boolean
    external fun setStereoEnhancement(self: JamesDspHandle, enable: Boolean, level: Float): Boolean
    external fun setVacuumTube(self: JamesDspHandle, enable: Boolean, level: Float): Boolean
    external fun setLiveprog(self: JamesDspHandle, enable: Boolean, id: String, liveprogContent: String): Boolean

    // EEL VM utilities (liveprog scripting)
    external fun enumerateEelVariables(self: JamesDspHandle): ArrayList<EelVmVariable>
    external fun manipulateEelVariable(self: JamesDspHandle, name: String, value: Float): Boolean
    external fun freezeLiveprogExecution(self: JamesDspHandle, freeze: Boolean)
    external fun eelErrorCodeToString(errorCode: Int): String

    /** Error codes the native convolver can report via [JamesDspCallbacks.onConvolverParseError]. */
    enum class ConvolverErrorCode(val value: Int) {
        Unknown(0),
        Corrupted(1),
        NoFrames(2),
        AdvParamsInvalid(3);

        companion object {
            fun fromInt(value: Int) = entries.first { it.value == value }
        }
    }

    /** Callbacks the native wrapper invokes back into Kotlin; passed to [alloc]. */
    interface JamesDspCallbacks {
        fun onLiveprogOutput(message: String)
        fun onLiveprogExec(id: String)
        fun onLiveprogResult(resultCode: Int, id: String, errorMessage: String?)
        fun onVdcParseError()
        fun onConvolverParseError(errorCode: ConvolverErrorCode)
    }

    init {
        System.loadLibrary("jamesdsp-wrapper")
    }
}

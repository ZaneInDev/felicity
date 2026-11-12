package app.simple.felicity.engine.processors

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import app.simple.felicity.engine.jamesdsp.EelVmVariable
import app.simple.felicity.engine.jamesdsp.JamesDspHandle
import app.simple.felicity.engine.jamesdsp.JamesDspWrapper
import app.simple.felicity.engine.utils.PcmUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * An [AudioProcessor] that runs the bundled JamesDSP native engine (see the
 * `:jamesdsp` module — the engine sources under `src/main/cpp/jamesdsp/` and
 * the JNI bridge under `src/main/cpp/wrapper/`) as the final effects stage in
 * Felicity's processor chain, right before [VisualizerProcessor].
 *
 * This is deliberately a thin adapter: all of the actual DSP (EQ, reverb,
 * convolver, compressor, crossfeed, bass boost, stereo widening, vacuum tube,
 * liveprog scripting, etc.) happens inside the native engine, reached through
 * [JamesDspWrapper]'s JNI surface. This class's only two jobs are (1) owning
 * the native handle's lifecycle across format changes, and (2) converting
 * PCM in and out of the interleaved [FloatArray] the engine expects.
 *
 * The native `processFloatMultiplexd` entry point hard-assumes **stereo**
 * interleaved PCM (it computes frame count as `inputLength / 2`). This
 * processor therefore only activates for 2-channel audio — for any other
 * channel count it reports [AudioProcessor.AudioFormat.NOT_SET], exactly the
 * way [NativeDspAudioProcessor] bypasses itself for unsupported encodings.
 *
 * Every `setXxx` call below caches its parameters and re-applies them via
 * [pushAllParameters] whenever the native handle is (re)created — so effect
 * settings survive sample-rate changes, seeks, and track transitions without
 * the user having to touch a control to "reload" them.
 *
 * @author Hamza417
 */
@OptIn(UnstableApi::class)
class JamesDspAudioProcessor : AudioProcessor, JamesDspWrapper.JamesDspCallbacks {

    private var inputFormat: AudioProcessor.AudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var active = false
    private var inputEnded = false
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER

    private var handle: JamesDspHandle? = null

    /** Reusable interleaved float work buffer; only reallocated when the chunk size changes. */
    private var workBuf = FloatArray(0)

    @Volatile
    var enabled: Boolean = true

    // --- Cached effect parameters, re-applied by pushAllParameters() ---

    private var limiterThreshold = -0.1f
    private var limiterRelease = 60f
    private var postGain = 0f

    private var eqEnabled = false
    private var eqFilterType = 0
    private var eqInterpolationMode = 1
    private var eqBands: DoubleArray = DoubleArray(0)

    private var graphicEqEnabled = false
    private var graphicEqString = ""

    private var vdcEnabled = false
    private var vdcContents = ""

    private var companderEnabled = false
    private var companderTimeConstant = 0f
    private var companderGranularity = 0
    private var companderTfResolution = 0
    private var companderBands: DoubleArray = DoubleArray(0)

    private var reverbEnabled = false
    private var reverbPreset = 0

    private var convolverEnabled = false
    private var convolverIr: FloatArray = FloatArray(0)
    private var convolverChannels = 0
    private var convolverFrames = 0

    private var crossfeedEnabled = false
    private var crossfeedMode = 0
    private var crossfeedFcut = 700
    private var crossfeedFeed = 60

    private var bassBoostEnabled = false
    private var bassBoostMaxGain = 0f

    private var stereoEnhEnabled = false
    private var stereoEnhLevel = 0f

    private var vacuumTubeEnabled = false
    private var vacuumTubeLevel = 0f

    private var liveprogEnabled = false
    private var liveprogId = ""
    private var liveprogContent = ""

    /** Listener surface for liveprog / convolver / VDC diagnostics bubbled up from the engine. */
    var listener: JamesDspWrapper.JamesDspCallbacks? = null

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        val supported = PcmUtils.isEncodingSupported(inputAudioFormat.encoding) &&
                inputAudioFormat.channelCount == 2

        active = supported

        if (!supported) {
            inputFormat = AudioProcessor.AudioFormat.NOT_SET
            releaseHandle()
            return AudioProcessor.AudioFormat.NOT_SET
        }

        val formatChanged = inputAudioFormat.sampleRate != inputFormat.sampleRate ||
                inputAudioFormat.channelCount != inputFormat.channelCount
        inputFormat = inputAudioFormat

        if (handle == null) {
            handle = JamesDspWrapper.alloc(this).also {
                if (!JamesDspWrapper.isHandleValid(it)) {
                    handle = null
                }
            }
        }

        if (handle != null && formatChanged) {
            JamesDspWrapper.setSamplingRate(handle!!, inputAudioFormat.sampleRate.toFloat(), true)
        }

        pushAllParameters()

        return inputAudioFormat
    }

    override fun isActive(): Boolean = active

    override fun queueInput(inputBuffer: ByteBuffer) {
        val self = handle
        if (!active || !enabled || self == null) {
            outputBuffer = inputBuffer
            return
        }

        val encoding = inputFormat.encoding
        val bps = PcmUtils.bytesPerSample(encoding)
        val remaining = inputBuffer.remaining()
        val numSamples = remaining / bps

        if (numSamples == 0) {
            outputBuffer = inputBuffer
            return
        }

        if (workBuf.size != numSamples) {
            workBuf = FloatArray(numSamples)
        }

        if (encoding == C.ENCODING_PCM_FLOAT) {
            inputBuffer.asFloatBuffer().get(workBuf)
            inputBuffer.position(inputBuffer.limit())
        } else {
            for (i in 0 until numSamples) {
                workBuf[i] = PcmUtils.readFloat(inputBuffer, encoding)
            }
        }

        // In-place: engine writes processed samples back into the same array.
        JamesDspWrapper.processFloat(self, workBuf, workBuf, 0, numSamples)

        val buf = acquireOutputBuffer(remaining)
        if (encoding == C.ENCODING_PCM_FLOAT) {
            buf.asFloatBuffer().put(workBuf)
            buf.position(remaining)
        } else {
            for (i in 0 until numSamples) {
                PcmUtils.writeFloat(buf, workBuf[i], encoding)
            }
        }
        buf.flip()
        outputBuffer = buf
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {
        val out = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return out
    }

    override fun isEnded(): Boolean = inputEnded && outputBuffer === AudioProcessor.EMPTY_BUFFER

    @Suppress("OVERRIDE_DEPRECATION")
    override fun flush() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
    }

    override fun reset() {
        flush()
        inputFormat = AudioProcessor.AudioFormat.NOT_SET
        active = false
        workBuf = FloatArray(0)
        releaseHandle()
    }

    /**
     * Direct-path entry point used by [FelicityAudioSink] for the USB DAC / AAudio / Oboe
     * output paths, which bypass [DefaultAudioSink]'s [AudioProcessor] chain entirely and
     * push native float buffers straight to hardware. Mirrors
     * [NativeDspAudioProcessor.processInPlace] so JamesDSP still applies on those paths
     * exactly as it does via [queueInput] on the standard AudioTrack path.
     *
     * No-ops when inactive, disabled, or no native handle exists yet.
     *
     * @param samples Interleaved stereo float buffer, processed in place.
     * @param length  Number of valid elements in [samples] (may be < samples.size).
     */
    fun processInPlace(samples: FloatArray, length: Int = samples.size) {
        val self = handle ?: return
        if (!active || !enabled) return

        if (samples.size == length) {
            JamesDspWrapper.processFloat(self, samples, samples, 0, length)
        } else {
            val slice = samples.copyOf(length)
            JamesDspWrapper.processFloat(self, slice, slice, 0, length)
            System.arraycopy(slice, 0, samples, 0, length)
        }
    }

    private fun releaseHandle() {
        handle?.let { JamesDspWrapper.free(it) }
        handle = null
    }

    private fun acquireOutputBuffer(capacity: Int): ByteBuffer {
        return if (outputBuffer === AudioProcessor.EMPTY_BUFFER || outputBuffer.capacity() < capacity) {
            ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder())
        } else {
            outputBuffer.clear()
            outputBuffer.limit(capacity)
            outputBuffer
        }
    }

    // ------------------------------------------------------------------
    // Effect setters — each caches its parameters and applies immediately
    // if a native handle already exists; pushAllParameters() re-applies
    // every cached value after a (re)configure.
    // ------------------------------------------------------------------

    fun setLimiter(threshold: Float, release: Float) {
        limiterThreshold = threshold; limiterRelease = release
        handle?.let { JamesDspWrapper.setLimiter(it, threshold, release) }
    }

    fun setPostGain(gain: Float) {
        postGain = gain
        handle?.let { JamesDspWrapper.setPostGain(it, gain) }
    }

    fun setMultiEqualizer(enable: Boolean, filterType: Int, interpolationMode: Int, bands: DoubleArray) {
        eqEnabled = enable; eqFilterType = filterType; eqInterpolationMode = interpolationMode; eqBands = bands
        handle?.let { JamesDspWrapper.setMultiEqualizer(it, enable, filterType, interpolationMode, bands) }
    }

    fun setGraphicEq(enable: Boolean, graphicEq: String) {
        graphicEqEnabled = enable; graphicEqString = graphicEq
        handle?.let { JamesDspWrapper.setGraphicEq(it, enable, graphicEq) }
    }

    fun setVdc(enable: Boolean, contents: String) {
        vdcEnabled = enable; vdcContents = contents
        handle?.let { JamesDspWrapper.setVdc(it, enable, contents) }
    }

    fun setCompander(enable: Boolean, timeConstant: Float, granularity: Int, tfResolution: Int, bands: DoubleArray) {
        companderEnabled = enable; companderTimeConstant = timeConstant
        companderGranularity = granularity; companderTfResolution = tfResolution; companderBands = bands
        handle?.let { JamesDspWrapper.setCompander(it, enable, timeConstant, granularity, tfResolution, bands) }
    }

    fun setReverb(enable: Boolean, preset: Int) {
        reverbEnabled = enable; reverbPreset = preset
        handle?.let { JamesDspWrapper.setReverb(it, enable, preset) }
    }

    fun setConvolver(enable: Boolean, impulseResponse: FloatArray, irChannels: Int, irFrames: Int) {
        convolverEnabled = enable; convolverIr = impulseResponse
        convolverChannels = irChannels; convolverFrames = irFrames
        handle?.let {
            val ok = JamesDspWrapper.setConvolver(it, enable, impulseResponse, irChannels, irFrames)
            if (!ok) onConvolverParseError(JamesDspWrapper.ConvolverErrorCode.Corrupted)
        }
    }

    fun setCrossfeed(enable: Boolean, mode: Int, customFcut: Int, customFeed: Int) {
        crossfeedEnabled = enable; crossfeedMode = mode; crossfeedFcut = customFcut; crossfeedFeed = customFeed
        handle?.let { JamesDspWrapper.setCrossfeed(it, enable, mode, customFcut, customFeed) }
    }

    fun setBassBoost(enable: Boolean, maxGain: Float) {
        bassBoostEnabled = enable; bassBoostMaxGain = maxGain
        handle?.let { JamesDspWrapper.setBassBoost(it, enable, maxGain) }
    }

    fun setStereoEnhancement(enable: Boolean, level: Float) {
        stereoEnhEnabled = enable; stereoEnhLevel = level
        handle?.let { JamesDspWrapper.setStereoEnhancement(it, enable, level) }
    }

    fun setVacuumTube(enable: Boolean, level: Float) {
        vacuumTubeEnabled = enable; vacuumTubeLevel = level
        handle?.let { JamesDspWrapper.setVacuumTube(it, enable, level) }
    }

    fun setLiveprog(enable: Boolean, id: String, content: String) {
        liveprogEnabled = enable; liveprogId = id; liveprogContent = content
        handle?.let { JamesDspWrapper.setLiveprog(it, enable, id, content) }
    }

    fun enumerateEelVariables(): ArrayList<EelVmVariable> =
        handle?.let { JamesDspWrapper.enumerateEelVariables(it) } ?: ArrayList()

    fun manipulateEelVariable(name: String, value: Float): Boolean =
        handle?.let { JamesDspWrapper.manipulateEelVariable(it, name, value) } ?: false

    fun freezeLiveprogExecution(freeze: Boolean) {
        handle?.let { JamesDspWrapper.freezeLiveprogExecution(it, freeze) }
    }

    private fun pushAllParameters() {
        val self = handle ?: return
        JamesDspWrapper.setLimiter(self, limiterThreshold, limiterRelease)
        JamesDspWrapper.setPostGain(self, postGain)
        if (eqBands.isNotEmpty()) {
            JamesDspWrapper.setMultiEqualizer(self, eqEnabled, eqFilterType, eqInterpolationMode, eqBands)
        }
        if (graphicEqString.isNotEmpty()) {
            JamesDspWrapper.setGraphicEq(self, graphicEqEnabled, graphicEqString)
        }
        if (vdcContents.isNotEmpty()) {
            JamesDspWrapper.setVdc(self, vdcEnabled, vdcContents)
        }
        if (companderBands.isNotEmpty()) {
            JamesDspWrapper.setCompander(self, companderEnabled, companderTimeConstant, companderGranularity, companderTfResolution, companderBands)
        }
        JamesDspWrapper.setReverb(self, reverbEnabled, reverbPreset)
        if (convolverIr.isNotEmpty()) {
            JamesDspWrapper.setConvolver(self, convolverEnabled, convolverIr, convolverChannels, convolverFrames)
        }
        JamesDspWrapper.setCrossfeed(self, crossfeedEnabled, crossfeedMode, crossfeedFcut, crossfeedFeed)
        JamesDspWrapper.setBassBoost(self, bassBoostEnabled, bassBoostMaxGain)
        JamesDspWrapper.setStereoEnhancement(self, stereoEnhEnabled, stereoEnhLevel)
        JamesDspWrapper.setVacuumTube(self, vacuumTubeEnabled, vacuumTubeLevel)
        if (liveprogContent.isNotEmpty()) {
            JamesDspWrapper.setLiveprog(self, liveprogEnabled, liveprogId, liveprogContent)
        }
    }

    // --- JamesDspWrapper.JamesDspCallbacks: forwarded to an optional external listener ---

    override fun onLiveprogOutput(message: String) {
        listener?.onLiveprogOutput(message)
    }

    override fun onLiveprogExec(id: String) {
        listener?.onLiveprogExec(id)
    }

    override fun onLiveprogResult(resultCode: Int, id: String, errorMessage: String?) {
        listener?.onLiveprogResult(resultCode, id, errorMessage)
    }

    override fun onVdcParseError() {
        listener?.onVdcParseError()
    }

    override fun onConvolverParseError(errorCode: JamesDspWrapper.ConvolverErrorCode) {
        listener?.onConvolverParseError(errorCode)
    }
}

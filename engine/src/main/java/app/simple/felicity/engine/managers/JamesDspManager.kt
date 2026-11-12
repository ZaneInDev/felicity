package app.simple.felicity.engine.managers

import app.simple.felicity.engine.processors.JamesDspAudioProcessor
import app.simple.felicity.preferences.JamesDspPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton manager for the bundled JamesDSP native engine.
 *
 * Bridges [JamesDspPreferences] (persisted settings) and the live
 * [JamesDspAudioProcessor] that lives inside [AudioProcessorManager]'s chain — the same
 * attach/detach pattern [EqualizerManager] uses for Felicity's own built-in EQ.
 *
 * Every `setXxx` here: persists to [JamesDspPreferences], applies to the live processor
 * if attached, and updates the relevant [StateFlow] so the UI stays in sync regardless of
 * whether the change came from a knob, a preset load, or [attachProcessor] restoring state
 * at startup.
 *
 * Usage:
 * ```kotlin
 * // In FelicityPlayerService.onCreate, after AudioProcessorManager is ready:
 * JamesDspManager.attachProcessor(audioProcessorManager.jamesDspProcessor)
 *
 * // In a JamesDSP effect fragment:
 * lifecycleScope.launch {
 *     JamesDspManager.masterEnabledFlow.collect { enabled -> masterSwitch.isChecked = enabled }
 * }
 * ```
 *
 * @author Hamza417
 */
object JamesDspManager {

    /** The live [JamesDspAudioProcessor] registered by the player service. */
    private var processor: JamesDspAudioProcessor? = null

    private val _masterEnabledFlow = MutableStateFlow(JamesDspPreferences.isMasterEnabled())
    val masterEnabledFlow: StateFlow<Boolean> = _masterEnabledFlow.asStateFlow()

    private val _eqEnabledFlow = MutableStateFlow(JamesDspPreferences.isEqEnabled())
    val eqEnabledFlow: StateFlow<Boolean> = _eqEnabledFlow.asStateFlow()

    private val _bassBoostEnabledFlow = MutableStateFlow(JamesDspPreferences.isBassBoostEnabled())
    val bassBoostEnabledFlow: StateFlow<Boolean> = _bassBoostEnabledFlow.asStateFlow()

    private val _reverbEnabledFlow = MutableStateFlow(JamesDspPreferences.isReverbEnabled())
    val reverbEnabledFlow: StateFlow<Boolean> = _reverbEnabledFlow.asStateFlow()

    private val _companderEnabledFlow = MutableStateFlow(JamesDspPreferences.isCompanderEnabled())
    val companderEnabledFlow: StateFlow<Boolean> = _companderEnabledFlow.asStateFlow()

    private val _convolverEnabledFlow = MutableStateFlow(JamesDspPreferences.isConvolverEnabled())
    val convolverEnabledFlow: StateFlow<Boolean> = _convolverEnabledFlow.asStateFlow()

    private val _crossfeedEnabledFlow = MutableStateFlow(JamesDspPreferences.isCrossfeedEnabled())
    val crossfeedEnabledFlow: StateFlow<Boolean> = _crossfeedEnabledFlow.asStateFlow()

    private val _stereoEnhancementEnabledFlow = MutableStateFlow(JamesDspPreferences.isStereoEnhancementEnabled())
    val stereoEnhancementEnabledFlow: StateFlow<Boolean> = _stereoEnhancementEnabledFlow.asStateFlow()

    private val _vacuumTubeEnabledFlow = MutableStateFlow(JamesDspPreferences.isVacuumTubeEnabled())
    val vacuumTubeEnabledFlow: StateFlow<Boolean> = _vacuumTubeEnabledFlow.asStateFlow()

    private val _liveprogEnabledFlow = MutableStateFlow(JamesDspPreferences.isLiveprogEnabled())
    val liveprogEnabledFlow: StateFlow<Boolean> = _liveprogEnabledFlow.asStateFlow()

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Registers the [JamesDspAudioProcessor] instance owned by [AudioProcessorManager] and
     * immediately re-applies every persisted [JamesDspPreferences] value to it, so saved
     * settings survive a pipeline rebuild (format change, service restart) without the
     * user touching a control.
     */
    fun attachProcessor(jamesDspProcessor: JamesDspAudioProcessor) {
        processor = jamesDspProcessor
        jamesDspProcessor.enabled = JamesDspPreferences.isMasterEnabled()

        jamesDspProcessor.setLimiter(JamesDspPreferences.getLimiterThreshold(), JamesDspPreferences.getLimiterRelease())
        jamesDspProcessor.setPostGain(JamesDspPreferences.getPostGain())

        JamesDspPreferences.getEqBandsRaw()?.let { raw ->
            jamesDspProcessor.setGraphicEq(JamesDspPreferences.isEqEnabled(), raw)
        }
        jamesDspProcessor.setBassBoost(JamesDspPreferences.isBassBoostEnabled(), JamesDspPreferences.getBassBoostMaxGain())
        jamesDspProcessor.setReverb(JamesDspPreferences.isReverbEnabled(), JamesDspPreferences.getReverbPreset())
        JamesDspPreferences.getCompanderBandsRaw()?.let { raw ->
            jamesDspProcessor.setCompander(
                    JamesDspPreferences.isCompanderEnabled(),
                    JamesDspPreferences.getCompanderTimeConstant(),
                    JamesDspPreferences.getCompanderGranularity(),
                    JamesDspPreferences.getCompanderTfResolution(),
                    parseDoubleBands(raw)
            )
        }
        jamesDspProcessor.setCrossfeed(
                JamesDspPreferences.isCrossfeedEnabled(),
                JamesDspPreferences.getCrossfeedMode(),
                JamesDspPreferences.getCrossfeedCustomFcut(),
                JamesDspPreferences.getCrossfeedCustomFeed()
        )
        jamesDspProcessor.setStereoEnhancement(JamesDspPreferences.isStereoEnhancementEnabled(), JamesDspPreferences.getStereoEnhancementLevel())
        jamesDspProcessor.setVacuumTube(JamesDspPreferences.isVacuumTubeEnabled(), JamesDspPreferences.getVacuumTubeLevel())

        syncFlowsFromPreferences()
    }

    /** Call from the player service's teardown so the manager does not hold a stale reference. */
    fun detachProcessor() {
        processor = null
    }

    private fun syncFlowsFromPreferences() {
        _masterEnabledFlow.value = JamesDspPreferences.isMasterEnabled()
        _eqEnabledFlow.value = JamesDspPreferences.isEqEnabled()
        _bassBoostEnabledFlow.value = JamesDspPreferences.isBassBoostEnabled()
        _reverbEnabledFlow.value = JamesDspPreferences.isReverbEnabled()
        _companderEnabledFlow.value = JamesDspPreferences.isCompanderEnabled()
        _convolverEnabledFlow.value = JamesDspPreferences.isConvolverEnabled()
        _crossfeedEnabledFlow.value = JamesDspPreferences.isCrossfeedEnabled()
        _stereoEnhancementEnabledFlow.value = JamesDspPreferences.isStereoEnhancementEnabled()
        _vacuumTubeEnabledFlow.value = JamesDspPreferences.isVacuumTubeEnabled()
        _liveprogEnabledFlow.value = JamesDspPreferences.isLiveprogEnabled()
    }

    // -------------------------------------------------------------------------
    // Master
    // -------------------------------------------------------------------------

    fun setMasterEnabled(enabled: Boolean) {
        JamesDspPreferences.setMasterEnabled(enabled)
        processor?.enabled = enabled
        _masterEnabledFlow.value = enabled
    }

    fun isMasterEnabled(): Boolean = _masterEnabledFlow.value

    // -------------------------------------------------------------------------
    // Limiter / post gain
    // -------------------------------------------------------------------------

    fun setLimiter(threshold: Float, release: Float) {
        JamesDspPreferences.setLimiter(threshold, release)
        processor?.setLimiter(threshold, release)
    }

    fun setPostGain(gain: Float) {
        JamesDspPreferences.setPostGain(gain)
        processor?.setPostGain(gain)
    }

    // -------------------------------------------------------------------------
    // Equalizer
    // -------------------------------------------------------------------------

    /**
     * Applies a graphic EQ string (the native engine's own "freq:gain;freq:gain;..." form)
     * built by the UI layer, persists it, and updates [eqEnabledFlow].
     */
    fun setGraphicEq(enabled: Boolean, filterType: Int, interpolationMode: Int, bandsRaw: String) {
        JamesDspPreferences.setEqEnabled(enabled)
        JamesDspPreferences.setEqFilterType(filterType)
        JamesDspPreferences.setEqInterpolationMode(interpolationMode)
        JamesDspPreferences.setEqBandsRaw(bandsRaw)
        processor?.setGraphicEq(enabled, bandsRaw)
        _eqEnabledFlow.value = enabled
    }

    fun isEqEnabled(): Boolean = _eqEnabledFlow.value

    // -------------------------------------------------------------------------
    // Bass boost
    // -------------------------------------------------------------------------

    fun setBassBoost(enabled: Boolean, maxGain: Float) {
        JamesDspPreferences.setBassBoost(enabled, maxGain)
        processor?.setBassBoost(enabled, maxGain)
        _bassBoostEnabledFlow.value = enabled
    }

    // -------------------------------------------------------------------------
    // Reverb
    // -------------------------------------------------------------------------

    fun setReverb(enabled: Boolean, preset: Int) {
        JamesDspPreferences.setReverb(enabled, preset)
        processor?.setReverb(enabled, preset)
        _reverbEnabledFlow.value = enabled
    }

    // -------------------------------------------------------------------------
    // Compressor (compander)
    // -------------------------------------------------------------------------

    fun setCompander(enabled: Boolean, timeConstant: Float, granularity: Int, tfResolution: Int, bands: DoubleArray) {
        val raw = bands.joinToString(";")
        JamesDspPreferences.setCompander(enabled, timeConstant, granularity, tfResolution, raw)
        processor?.setCompander(enabled, timeConstant, granularity, tfResolution, bands)
        _companderEnabledFlow.value = enabled
    }

    // -------------------------------------------------------------------------
    // Convolver
    // -------------------------------------------------------------------------

    /**
     * Applies a decoded impulse response to the processor and persists the source URI so
     * the UI can show which file is loaded. Decoding the IR file itself (WAV/FLAC/MP3 →
     * [FloatArray]) is a UI-layer concern — see the convolver effect screen.
     */
    fun setConvolver(enabled: Boolean, irUri: String?, impulseResponse: FloatArray, irChannels: Int, irFrames: Int) {
        JamesDspPreferences.setConvolver(enabled, irUri)
        processor?.setConvolver(enabled, impulseResponse, irChannels, irFrames)
        _convolverEnabledFlow.value = enabled
    }

    // -------------------------------------------------------------------------
    // Crossfeed / stereo enhancement / vacuum tube
    // -------------------------------------------------------------------------

    fun setCrossfeed(enabled: Boolean, mode: Int, customFcut: Int, customFeed: Int) {
        JamesDspPreferences.setCrossfeed(enabled, mode, customFcut, customFeed)
        processor?.setCrossfeed(enabled, mode, customFcut, customFeed)
        _crossfeedEnabledFlow.value = enabled
    }

    fun setStereoEnhancement(enabled: Boolean, level: Float) {
        JamesDspPreferences.setStereoEnhancement(enabled, level)
        processor?.setStereoEnhancement(enabled, level)
        _stereoEnhancementEnabledFlow.value = enabled
    }

    fun setVacuumTube(enabled: Boolean, level: Float) {
        JamesDspPreferences.setVacuumTube(enabled, level)
        processor?.setVacuumTube(enabled, level)
        _vacuumTubeEnabledFlow.value = enabled
    }

    // -------------------------------------------------------------------------
    // Liveprog (EEL scripting)
    // -------------------------------------------------------------------------

    fun setLiveprog(enabled: Boolean, id: String, scriptUri: String?, scriptContent: String) {
        JamesDspPreferences.setLiveprog(enabled, id, scriptUri)
        processor?.setLiveprog(enabled, id, scriptContent)
        _liveprogEnabledFlow.value = enabled
    }

    private fun parseDoubleBands(raw: String): DoubleArray =
        raw.split(";").mapNotNull { it.toDoubleOrNull() }.toDoubleArray()
}

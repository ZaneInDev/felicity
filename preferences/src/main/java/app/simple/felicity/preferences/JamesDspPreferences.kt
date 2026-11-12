package app.simple.felicity.preferences

import androidx.core.content.edit
import app.simple.felicity.manager.SharedPreferences

/**
 * Persists every control exposed by the bundled JamesDSP native engine
 * (see the `:jamesdsp` module and [app.simple.felicity.engine.processors.JamesDspAudioProcessor]).
 *
 * This is a separate, additional global-equalizer-style effects stage from Felicity's own
 * built-in EQ/bass/treble/width/balance/saturation ([EqualizerPreferences]) — the two do not
 * share storage and can be used independently or together.
 *
 * @author Hamza417
 */
object JamesDspPreferences {

    /** Master enable for the entire JamesDSP processing stage. Default false (off by default). */
    const val MASTER_ENABLED = "jamesdsp_master_enabled"

    // -------------------------------------------------------------------------
    // Limiter / post gain
    // -------------------------------------------------------------------------

    /** Limiter threshold in dBFS, typically a small negative value just under 0. */
    const val LIMITER_THRESHOLD = "jamesdsp_limiter_threshold"

    /** Limiter release time in milliseconds. */
    const val LIMITER_RELEASE = "jamesdsp_limiter_release"

    /** Final output trim applied after every effect, in dB. */
    const val POST_GAIN = "jamesdsp_post_gain"

    // -------------------------------------------------------------------------
    // Equalizer (graphic string form, matching the native engine's own format)
    // -------------------------------------------------------------------------

    /** Boolean flag for the multi-band / graphic equalizer. */
    const val EQ_ENABLED = "jamesdsp_eq_enabled"

    /** Filter type index passed to `setMultiEqualizer` (engine-defined enum). */
    const val EQ_FILTER_TYPE = "jamesdsp_eq_filter_type"

    /** Interpolation mode index passed to `setMultiEqualizer` (engine-defined enum). */
    const val EQ_INTERPOLATION_MODE = "jamesdsp_eq_interpolation_mode"

    /**
     * Serialized graphic EQ string in the native engine's own "freq:gain;freq:gain;..." form,
     * passed directly to `setGraphicEq`/`setMultiEqualizer`. Kept as an opaque string here
     * (rather than parsed arrays) since the UI layer owns the exact serialization format.
     */
    const val EQ_BANDS_RAW = "jamesdsp_eq_bands_raw"

    // -------------------------------------------------------------------------
    // Bass boost
    // -------------------------------------------------------------------------

    const val BASS_BOOST_ENABLED = "jamesdsp_bass_boost_enabled"
    const val BASS_BOOST_MAX_GAIN = "jamesdsp_bass_boost_max_gain"

    // -------------------------------------------------------------------------
    // Reverb
    // -------------------------------------------------------------------------

    const val REVERB_ENABLED = "jamesdsp_reverb_enabled"

    /** Reverb preset index, as defined by the native engine's reverb module. */
    const val REVERB_PRESET = "jamesdsp_reverb_preset"

    // -------------------------------------------------------------------------
    // Compressor (compander)
    // -------------------------------------------------------------------------

    const val COMPANDER_ENABLED = "jamesdsp_compander_enabled"
    const val COMPANDER_TIME_CONSTANT = "jamesdsp_compander_time_constant"
    const val COMPANDER_GRANULARITY = "jamesdsp_compander_granularity"
    const val COMPANDER_TF_RESOLUTION = "jamesdsp_compander_tf_resolution"

    /** Serialized compander band gains, same "value;value;..." form the engine expects. */
    const val COMPANDER_BANDS_RAW = "jamesdsp_compander_bands_raw"

    // -------------------------------------------------------------------------
    // Convolver (impulse response loader)
    // -------------------------------------------------------------------------

    const val CONVOLVER_ENABLED = "jamesdsp_convolver_enabled"

    /** Content URI (as a string) of the last loaded impulse-response file, if any. */
    const val CONVOLVER_IR_URI = "jamesdsp_convolver_ir_uri"

    // -------------------------------------------------------------------------
    // Crossfeed / stereo enhancement / vacuum tube
    // -------------------------------------------------------------------------

    const val CROSSFEED_ENABLED = "jamesdsp_crossfeed_enabled"
    const val CROSSFEED_MODE = "jamesdsp_crossfeed_mode"
    const val CROSSFEED_CUSTOM_FCUT = "jamesdsp_crossfeed_custom_fcut"
    const val CROSSFEED_CUSTOM_FEED = "jamesdsp_crossfeed_custom_feed"

    const val STEREO_ENHANCEMENT_ENABLED = "jamesdsp_stereo_enhancement_enabled"
    const val STEREO_ENHANCEMENT_LEVEL = "jamesdsp_stereo_enhancement_level"

    const val VACUUM_TUBE_ENABLED = "jamesdsp_vacuum_tube_enabled"
    const val VACUUM_TUBE_LEVEL = "jamesdsp_vacuum_tube_level"

    // -------------------------------------------------------------------------
    // Liveprog (EEL scripting)
    // -------------------------------------------------------------------------

    const val LIVEPROG_ENABLED = "jamesdsp_liveprog_enabled"
    const val LIVEPROG_ID = "jamesdsp_liveprog_id"

    /** Content URI (as a string) of the last loaded liveprog script file, if any. */
    const val LIVEPROG_SCRIPT_URI = "jamesdsp_liveprog_script_uri"

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    fun setMasterEnabled(enabled: Boolean) {
        SharedPreferences.getSharedPreferences().edit { putBoolean(MASTER_ENABLED, enabled) }
    }

    fun isMasterEnabled(): Boolean = SharedPreferences.getSharedPreferences().getBoolean(MASTER_ENABLED, false)

    fun setLimiter(threshold: Float, release: Float) {
        SharedPreferences.getSharedPreferences().edit {
            putFloat(LIMITER_THRESHOLD, threshold)
            putFloat(LIMITER_RELEASE, release)
        }
    }

    fun getLimiterThreshold(): Float = SharedPreferences.getSharedPreferences().getFloat(LIMITER_THRESHOLD, -0.1f)
    fun getLimiterRelease(): Float = SharedPreferences.getSharedPreferences().getFloat(LIMITER_RELEASE, 60f)

    fun setPostGain(gain: Float) {
        SharedPreferences.getSharedPreferences().edit { putFloat(POST_GAIN, gain) }
    }

    fun getPostGain(): Float = SharedPreferences.getSharedPreferences().getFloat(POST_GAIN, 0f)

    fun setEqEnabled(enabled: Boolean) {
        SharedPreferences.getSharedPreferences().edit { putBoolean(EQ_ENABLED, enabled) }
    }

    fun isEqEnabled(): Boolean = SharedPreferences.getSharedPreferences().getBoolean(EQ_ENABLED, false)

    fun setEqFilterType(type: Int) {
        SharedPreferences.getSharedPreferences().edit { putInt(EQ_FILTER_TYPE, type) }
    }

    fun getEqFilterType(): Int = SharedPreferences.getSharedPreferences().getInt(EQ_FILTER_TYPE, 0)

    fun setEqInterpolationMode(mode: Int) {
        SharedPreferences.getSharedPreferences().edit { putInt(EQ_INTERPOLATION_MODE, mode) }
    }

    fun getEqInterpolationMode(): Int = SharedPreferences.getSharedPreferences().getInt(EQ_INTERPOLATION_MODE, 1)

    fun setEqBandsRaw(raw: String?) {
        SharedPreferences.getSharedPreferences().edit { putString(EQ_BANDS_RAW, raw) }
    }

    fun getEqBandsRaw(): String? = SharedPreferences.getSharedPreferences().getString(EQ_BANDS_RAW, null)

    fun setBassBoost(enabled: Boolean, maxGain: Float) {
        SharedPreferences.getSharedPreferences().edit {
            putBoolean(BASS_BOOST_ENABLED, enabled)
            putFloat(BASS_BOOST_MAX_GAIN, maxGain)
        }
    }

    fun isBassBoostEnabled(): Boolean = SharedPreferences.getSharedPreferences().getBoolean(BASS_BOOST_ENABLED, false)
    fun getBassBoostMaxGain(): Float = SharedPreferences.getSharedPreferences().getFloat(BASS_BOOST_MAX_GAIN, 0f)

    fun setReverb(enabled: Boolean, preset: Int) {
        SharedPreferences.getSharedPreferences().edit {
            putBoolean(REVERB_ENABLED, enabled)
            putInt(REVERB_PRESET, preset)
        }
    }

    fun isReverbEnabled(): Boolean = SharedPreferences.getSharedPreferences().getBoolean(REVERB_ENABLED, false)
    fun getReverbPreset(): Int = SharedPreferences.getSharedPreferences().getInt(REVERB_PRESET, 0)

    fun setCompander(enabled: Boolean, timeConstant: Float, granularity: Int, tfResolution: Int, bandsRaw: String?) {
        SharedPreferences.getSharedPreferences().edit {
            putBoolean(COMPANDER_ENABLED, enabled)
            putFloat(COMPANDER_TIME_CONSTANT, timeConstant)
            putInt(COMPANDER_GRANULARITY, granularity)
            putInt(COMPANDER_TF_RESOLUTION, tfResolution)
            putString(COMPANDER_BANDS_RAW, bandsRaw)
        }
    }

    fun isCompanderEnabled(): Boolean = SharedPreferences.getSharedPreferences().getBoolean(COMPANDER_ENABLED, false)
    fun getCompanderTimeConstant(): Float = SharedPreferences.getSharedPreferences().getFloat(COMPANDER_TIME_CONSTANT, 0f)
    fun getCompanderGranularity(): Int = SharedPreferences.getSharedPreferences().getInt(COMPANDER_GRANULARITY, 0)
    fun getCompanderTfResolution(): Int = SharedPreferences.getSharedPreferences().getInt(COMPANDER_TF_RESOLUTION, 0)
    fun getCompanderBandsRaw(): String? = SharedPreferences.getSharedPreferences().getString(COMPANDER_BANDS_RAW, null)

    fun setConvolver(enabled: Boolean, irUri: String?) {
        SharedPreferences.getSharedPreferences().edit {
            putBoolean(CONVOLVER_ENABLED, enabled)
            putString(CONVOLVER_IR_URI, irUri)
        }
    }

    fun isConvolverEnabled(): Boolean = SharedPreferences.getSharedPreferences().getBoolean(CONVOLVER_ENABLED, false)
    fun getConvolverIrUri(): String? = SharedPreferences.getSharedPreferences().getString(CONVOLVER_IR_URI, null)

    fun setCrossfeed(enabled: Boolean, mode: Int, customFcut: Int, customFeed: Int) {
        SharedPreferences.getSharedPreferences().edit {
            putBoolean(CROSSFEED_ENABLED, enabled)
            putInt(CROSSFEED_MODE, mode)
            putInt(CROSSFEED_CUSTOM_FCUT, customFcut)
            putInt(CROSSFEED_CUSTOM_FEED, customFeed)
        }
    }

    fun isCrossfeedEnabled(): Boolean = SharedPreferences.getSharedPreferences().getBoolean(CROSSFEED_ENABLED, false)
    fun getCrossfeedMode(): Int = SharedPreferences.getSharedPreferences().getInt(CROSSFEED_MODE, 0)
    fun getCrossfeedCustomFcut(): Int = SharedPreferences.getSharedPreferences().getInt(CROSSFEED_CUSTOM_FCUT, 700)
    fun getCrossfeedCustomFeed(): Int = SharedPreferences.getSharedPreferences().getInt(CROSSFEED_CUSTOM_FEED, 60)

    fun setStereoEnhancement(enabled: Boolean, level: Float) {
        SharedPreferences.getSharedPreferences().edit {
            putBoolean(STEREO_ENHANCEMENT_ENABLED, enabled)
            putFloat(STEREO_ENHANCEMENT_LEVEL, level)
        }
    }

    fun isStereoEnhancementEnabled(): Boolean = SharedPreferences.getSharedPreferences().getBoolean(STEREO_ENHANCEMENT_ENABLED, false)
    fun getStereoEnhancementLevel(): Float = SharedPreferences.getSharedPreferences().getFloat(STEREO_ENHANCEMENT_LEVEL, 0f)

    fun setVacuumTube(enabled: Boolean, level: Float) {
        SharedPreferences.getSharedPreferences().edit {
            putBoolean(VACUUM_TUBE_ENABLED, enabled)
            putFloat(VACUUM_TUBE_LEVEL, level)
        }
    }

    fun isVacuumTubeEnabled(): Boolean = SharedPreferences.getSharedPreferences().getBoolean(VACUUM_TUBE_ENABLED, false)
    fun getVacuumTubeLevel(): Float = SharedPreferences.getSharedPreferences().getFloat(VACUUM_TUBE_LEVEL, 0f)

    fun setLiveprog(enabled: Boolean, id: String, scriptUri: String?) {
        SharedPreferences.getSharedPreferences().edit {
            putBoolean(LIVEPROG_ENABLED, enabled)
            putString(LIVEPROG_ID, id)
            putString(LIVEPROG_SCRIPT_URI, scriptUri)
        }
    }

    fun isLiveprogEnabled(): Boolean = SharedPreferences.getSharedPreferences().getBoolean(LIVEPROG_ENABLED, false)
    fun getLiveprogId(): String = SharedPreferences.getSharedPreferences().getString(LIVEPROG_ID, "") ?: ""
    fun getLiveprogScriptUri(): String? = SharedPreferences.getSharedPreferences().getString(LIVEPROG_SCRIPT_URI, null)
}

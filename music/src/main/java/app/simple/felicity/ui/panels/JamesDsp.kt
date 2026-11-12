package app.simple.felicity.ui.panels

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import app.simple.felicity.databinding.FragmentJamesdspBinding
import app.simple.felicity.decorations.knobs.FelicityKnobListener
import app.simple.felicity.decorations.toggles.FelicityButtonGroup.Companion.Button
import app.simple.felicity.engine.managers.JamesDspManager
import app.simple.felicity.extensions.fragments.MediaFragment
import app.simple.felicity.preferences.JamesDspPreferences
import app.simple.felicity.R

/**
 * Fragment that presents the bundled JamesDSP effects engine (see the `:jamesdsp` module):
 * a master enable switch plus per-effect controls. This first pass covers Bass Boost,
 * Reverb, and Crossfeed as a template — Compander, Stereo Enhancement, Vacuum Tube,
 * the multi-band EQ curve editor, Convolver (impulse response import), and Liveprog
 * (EEL script editor) follow the same switch+control pattern established here.
 *
 * Every control writes through [JamesDspPreferences], which the player service's
 * [android.content.SharedPreferences.OnSharedPreferenceChangeListener] observes and
 * forwards to the live [app.simple.felicity.engine.processors.JamesDspAudioProcessor]
 * via [JamesDspManager] — the same one-way-data-flow pattern used by the main
 * [Equalizer] screen and [EqualizerManager].
 *
 * @author Hamza417
 */
class JamesDsp : MediaFragment() {

    private lateinit var binding: FragmentJamesdspBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentJamesdspBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireHiddenMiniPlayer()

        setupMasterSwitch()
        setupBassBoost()
        setupReverb()
        setupCrossfeed()
    }

    // -------------------------------------------------------------------------
    // Master enable
    // -------------------------------------------------------------------------

    private fun setupMasterSwitch() {
        binding.jamesdspMasterSwitch.isChecked = JamesDspPreferences.isEnabled()
        updateChildControlsEnabled(JamesDspPreferences.isEnabled())
        binding.jamesdspMasterSwitch.setOnCheckedChangeListener { _, isChecked ->
            JamesDspManager.setEnabled(isChecked)
            updateChildControlsEnabled(isChecked)
            Log.d(TAG, "JamesDSP master enable: $isChecked")
        }
    }

    /**
     * Dims and disables every per-effect control when the master switch is off, mirroring
     * how [Equalizer]'s own EQ switch gates its sliders — effects can still be individually
     * toggled, but they have no audible effect (and no reason to be fiddled with) while the
     * whole JamesDSP stage is bypassed.
     */
    private fun updateChildControlsEnabled(enabled: Boolean) {
        val alpha = if (enabled) 1f else 0.4f
        for (v in listOf(
                binding.bassBoostSwitch, binding.bassBoostKnob,
                binding.reverbSwitch, binding.reverbPresetGroup,
                binding.crossfeedSwitch, binding.crossfeedModeGroup
        )) {
            v.isEnabled = enabled
            v.alpha = alpha
        }
    }

    // -------------------------------------------------------------------------
    // Bass boost
    // -------------------------------------------------------------------------

    private fun setupBassBoost() {
        binding.bassBoostSwitch.isChecked = JamesDspPreferences.isBassBoostEnabled()
        binding.bassBoostSwitch.setOnCheckedChangeListener { _, isChecked ->
            JamesDspManager.setBassBoostEnabled(isChecked)
        }

        // Knob value 0-100 maps linearly to max gain 0-12 dB.
        binding.bassBoostKnob.setTickTexts("0dB", "12dB")
        binding.bassBoostKnob.divisionCount = 100
        binding.bassBoostKnob.setKnobPosition(
                maxGainToKnob(JamesDspPreferences.getBassBoostMaxGain()), animate = false)
        binding.bassBoostKnob.setListener(object : FelicityKnobListener {
            override fun onIncrement(value: Float) {}

            override fun onRotate(value: Float) {
                val db = knobToMaxGain(value)
                JamesDspManager.setBassBoostMaxGain(db)
            }

            override fun onLabel(value: Float): String {
                val db = knobToMaxGain(value)
                return "%.1f dB".format(db)
            }
        })
    }

    private fun maxGainToKnob(db: Float): Float = (db / 12f) * 100f
    private fun knobToMaxGain(knob: Float): Float = (knob / 100f) * 12f

    // -------------------------------------------------------------------------
    // Reverb
    // -------------------------------------------------------------------------

    private fun setupReverb() {
        binding.reverbSwitch.isChecked = JamesDspPreferences.isReverbEnabled()
        binding.reverbSwitch.setOnCheckedChangeListener { _, isChecked ->
            JamesDspManager.setReverbEnabled(isChecked)
        }

        binding.reverbPresetGroup.setButtons(
                listOf(
                        Button(textResId = R.string.jamesdsp_reverb_small_room),
                        Button(textResId = R.string.jamesdsp_reverb_medium_room),
                        Button(textResId = R.string.jamesdsp_reverb_large_room),
                        Button(textResId = R.string.jamesdsp_reverb_hall)
                )
        )
        binding.reverbPresetGroup.setSelectedIndex(
                JamesDspPreferences.getReverbPreset().coerceIn(0, 3), animate = false, notifyListener = false)
        binding.reverbPresetGroup.setOnButtonSelectedListener { index ->
            JamesDspManager.setReverbPreset(index)
        }
    }

    // -------------------------------------------------------------------------
    // Crossfeed
    // -------------------------------------------------------------------------

    private fun setupCrossfeed() {
        binding.crossfeedSwitch.isChecked = JamesDspPreferences.isCrossfeedEnabled()
        binding.crossfeedSwitch.setOnCheckedChangeListener { _, isChecked ->
            JamesDspManager.setCrossfeedEnabled(isChecked)
        }

        // Modes 1-3 are the engine's built-in Bauer stereophonic-to-binaural presets
        // of increasing strength; mode 0 (custom fcut/feed) has no UI yet — it will
        // get its own pair of knobs alongside the multi-band EQ editor.
        binding.crossfeedModeGroup.setButtons(
                listOf(
                        Button(textResId = R.string.jamesdsp_crossfeed_weak),
                        Button(textResId = R.string.jamesdsp_crossfeed_moderate),
                        Button(textResId = R.string.jamesdsp_crossfeed_strong)
                )
        )
        val savedMode = JamesDspPreferences.getCrossfeedMode().coerceIn(1, 3)
        binding.crossfeedModeGroup.setSelectedIndex(savedMode - 1, animate = false, notifyListener = false)
        binding.crossfeedModeGroup.setOnButtonSelectedListener { index ->
            JamesDspManager.setCrossfeedMode(index + 1)
        }
    }

    override val wantsMiniPlayerVisible: Boolean
        get() = false

    companion object {
        private const val TAG = "JamesDsp"

        fun newInstance(): JamesDsp {
            return JamesDsp()
        }

        const val TAG_FRAGMENT = "jamesdsp_fragment"
    }
}

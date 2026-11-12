package app.simple.felicity.engine.jamesdsp

/**
 * One EEL (liveprog) VM variable, as enumerated/manipulated by the native
 * engine's scripting runtime. Ported verbatim from RootlessJamesDSP's
 * `interop.structure.EelVmVariable` — the native wrapper's `EelVmVariable.cpp`
 * constructs instances of this exact class by name via JNI `FindClass`, so
 * the package path here must match what was renamed on the native side.
 */
data class EelVmVariable(var name: String, var value: String, var isString: Boolean)

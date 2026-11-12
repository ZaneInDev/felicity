cmake_minimum_required(VERSION 3.22.1)
project("felicity_jamesdsp")

# ---------------------------------------------------------------------------
# libjamesdsp — JamesDSPManager's legacy (pre-"extensions"-branch) native DSP
# engine, bundled directly from source (not as a submodule). Built fresh here
# via CMake since the upstream project only ships an Android.mk/ndk-build
# config. File list and compiler flags below are copied 1:1 from the
# authoritative jni/jamesdsp/Android.mk (84 compiled sources, jamesdsp.c
# excluded — it is the legacy Android AudioFlinger effect_handle_t HAL entry
# point, dead weight for our purposes since we talk to JamesDSPLib directly).
# ---------------------------------------------------------------------------

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -ffile-prefix-map=${CMAKE_SOURCE_DIR}=. -ffile-prefix-map=${CMAKE_BINARY_DIR}=./build")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -ffile-prefix-map=${CMAKE_SOURCE_DIR}=. -ffile-prefix-map=${CMAKE_BINARY_DIR}=./build")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -ffile-prefix-map=${ANDROID_NDK}=./ndk")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -ffile-prefix-map=${ANDROID_NDK}=./ndk")

# ---------------------------------------------------------------------------
# libjamesdsp — the DSP engine itself (C, no JNI symbols). Compiled as its
# own shared library, exactly mirroring the upstream Android.mk module name,
# so the wrapper below can link against it and nothing else needs to change
# if upstream engine updates are pulled in later.
# ---------------------------------------------------------------------------
add_library(jamesdsp SHARED
        jamesdsp/cpthread.c\
        jamesdsp/jdsp/generalDSP/spectralInterpolatorFloat.c\
        jamesdsp/jdsp/generalDSP/ArbFIRGen.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/codelet.c\
        jamesdsp/jdsp/generalDSP/digitalFilters.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/FFTConvolver.c\
        jamesdsp/jdsp/generalDSP/TwoStageFFTConvolver.c\
        jamesdsp/jdsp/generalDSP/interpolation.c\
        jamesdsp/jdsp/generalDSP/generalProg.c\
        jamesdsp/jdsp/Effects/vdc.c\
        jamesdsp/jdsp/Effects/vacuumTube.c\
        jamesdsp/jdsp/Effects/stereoEnhancement.c\
        jamesdsp/jdsp/Effects/reverb.c\
        jamesdsp/jdsp/Effects/liveprogWrapper.c\
        jamesdsp/jdsp/Effects/multimodalEQ.c\
        jamesdsp/jdsp/Effects/dynamic.c\
        jamesdsp/jdsp/Effects/dbb.c\
        jamesdsp/jdsp/Effects/convolver1D.c\
        jamesdsp/jdsp/Effects/crossfeed.c\
        jamesdsp/jdsp/Effects/bs2b.c\
        jamesdsp/jdsp/Effects/arbEqConv.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/libsamplerate/samplerate.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/libsamplerate/src_sinc.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/libsamplerate/src_linear.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/FilterDesign/generalFdesign.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/FilterDesign/cos_fib_paraunitary.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/FilterDesign/polyphaseFilterbank.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/FilterDesign/polyphaseASRC.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/FilterDesign/eqnerror.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/FilterDesign/firls.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/SolveLinearSystem/inv.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/SolveLinearSystem/pinv.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/SolveLinearSystem/mldivide.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/SolveLinearSystem/mrdivide.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/SolveLinearSystem/qr_fact.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/solvopt.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/cpoly.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/MersenneTwister.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/quadprog.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/atox.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/constant.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/cxaop.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/cxbasic.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/cxconstant.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/cxconvf.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/cxexp.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/cxhypb.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/cxidiv.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/cxpow.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/cxprcmp.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/cxtrig.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/hpaconf.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/prcxpr.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/print.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/prxpr.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/sfmod.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/shift.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/xadd.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/xchcof.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/xdiv.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/xevtch.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/xexp.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/xfmod.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/xfrac.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/xhypb.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/xivhypb.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/xivtrg.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/xlog.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/xmul.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/xneg.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/xprcmp.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/xpwr.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/xsigerr.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/xsqrt.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/xtodbl.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/xtoflt.c\
        jamesdsp/jdsp/Effects/eel2/numericSys/HPFloat/xtrig.c\
        jamesdsp/jdsp/Effects/eel2/s_str.c\
        jamesdsp/jdsp/Effects/eel2/fft.c\
        jamesdsp/jdsp/Effects/eel2/nseel-compiler.c\
        jamesdsp/jdsp/Effects/eel2/nseel-ram.c\
        jamesdsp/jdsp/Effects/eel2/y.tab.c\
        jamesdsp/jdsp/binaryBlobs.c\
        jamesdsp/jdsp/jdspController.c
        )

target_include_directories(jamesdsp PRIVATE
        jamesdsp
        jamesdsp/jdsp
        jamesdsp/jdsp/Effects
        jamesdsp/jdsp/Effects/eel2
        jamesdsp/jdsp/Effects/eel2/numericSys
        jamesdsp/jdsp/generalDSP)

find_library(log-lib-jdsp log)
target_link_libraries(jamesdsp ${log-lib-jdsp} m)

# Flags copied verbatim from Android.mk's arm64-v8a / armeabi-v7a block.
# JAMESDSP_REFERENCE_IMPL selects the portable (non-NEON-intrinsic) code
# paths inside the engine; -Ofast/NEON/vectorize match upstream's release
# tuning for this exact engine revision.
target_compile_options(jamesdsp PRIVATE
        -Wall -Wextra -ffunction-sections -fdata-sections -Ofast
        -ftree-vectorize -fvisibility=hidden -DJAMESDSP_REFERENCE_IMPL)
if(ANDROID_ABI STREQUAL "armeabi-v7a" OR ANDROID_ABI STREQUAL "arm64-v8a")
    target_compile_options(jamesdsp PRIVATE -mfpu=neon)
endif()
target_link_options(jamesdsp PRIVATE -Wl,--gc-sections,--exclude-libs,ALL)

# ---------------------------------------------------------------------------
# libjamesdsp-wrapper — JNI bridge, ported near-verbatim from
# RootlessJamesDSP's libjamesdsp-wrapper (the part of that project that is
# engine-agnostic glue, not the capture/root pipeline we are discarding).
# Talks to `jamesdsp` via jdsp_header.h's JamesDSPLib struct.
# ---------------------------------------------------------------------------
add_library(jamesdsp-wrapper SHARED
        wrapper/JamesDspWrapper.cpp
        wrapper/JArrayList.cpp
        wrapper/EelVmVariable.cpp
        wrapper/EELStdOutExtension.c)

target_include_directories(jamesdsp-wrapper PRIVATE
        wrapper
        jamesdsp
        jamesdsp/jdsp
        jamesdsp/jdsp/Effects
        jamesdsp/jdsp/Effects/eel2
        jamesdsp/jdsp/Effects/eel2/numericSys
        jamesdsp/jdsp/generalDSP)

find_library(log-lib-wrapper log)
target_link_libraries(jamesdsp-wrapper jamesdsp ${log-lib-wrapper})

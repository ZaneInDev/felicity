#pragma once

// Minimal logging shim.
//
// Upstream RootlessJamesDSP's <Log.h> (libcrashlytics-connector) wraps these
// macros around Firebase Crashlytics breadcrumb logging in addition to
// android/log.h. Felicity does not depend on Firebase for this module, so
// this header keeps the same macro surface the wrapper .cpp files expect
// (LOGD/LOGW/LOGE/LOGI) but drops the Crashlytics call entirely — plain
// logcat output only.

#include <android/log.h>

#ifndef LOG_TAG
#define LOG_TAG "FelicityJamesDsp"
#endif

#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

# Live Isolated PDF Parser Process-Death Test Plan

**Status:** Emulator verified; synthetic-only evidence captured.  
**Date:** 2026-07-22  
**Requirements:** P-07, P-14, P-15, P-17; Local-AI principles A-01, A-02, A-06.

## Purpose

ADR-017 requires evidence that an actual Android isolated parser-process death becomes
an atomic, retryable, content-free result in the ordinary Memora process. Existing
tests prove simulated Binder death; they do not prove that Android kills the live
isolated service, tears down its Binder, and releases the caller from an in-flight
request.

## Narrow test boundary

The instrumentation test will:

1. explicitly bind the existing non-exported `IsolatedPdfParserService`;
2. start one client-side parser request against a test-created pipe whose writer stays
   open, so the ordinary process has an in-flight request but no real file;
3. inspect Android's process list through the instrumentation shell only, requiring
   exactly one process with Memora's package name but a UID different from the ordinary
   app process;
4. invoke Android's `am crash <pid>` shell command for that exact isolated process;
5. assert a retryable, content-free client result, caller-side descriptor closure, and
   connection unavailability; and
6. close its test pipe, executor, and service connection in cleanup.

No code path is allowed to use a URI, SAF grant, file path, Room database, original
PDF, UI, WorkManager, AI, network, telemetry, or a production-only test hook.

## Why this mechanism

Android documents that `UiAutomation.executeShellCommand` executes a command as if it
were run through `adb shell`, and Android's activity-manager shell implementation
accepts a process ID for `am crash`. The test derives the PID only after a live bind
and refuses to issue any crash command unless the process list contains exactly one
candidate that is distinct from the ordinary app UID. This preserves the production
service contract: no AIDL diagnostic method, debug Binder action, intent extra, or
production kill switch is added.

References reviewed:

- [UiAutomation API reference](https://developer.android.com/reference/android/app/UiAutomation)
- [Android ActivityManager shell command source](https://android.googlesource.com/platform/frameworks/base/%2B/f996aa26326d/services/core/java/com/android/server/am/ActivityManagerShellCommand.java)

## Acceptance criteria

- The test must prove it has a live client-side request before inducing death.
- The test must crash only a uniquely identified isolated-UID process, never the
  ordinary Memora process or a package-level target.
- A service death must yield `FAILURE`, `retryable = true`, and no page count.
- The supplied ordinary-process descriptor must be closed.
- The connection must become `RETRYABLE_UNAVAILABLE`.
- Any inability to identify a unique isolated process is a safe test failure; no shell
  crash command may run in that case.
- The test is synthetic-only evidence and does not enable real-source parsing.

## Manual verification procedure

1. Start the Medium Phone emulator with normal connectivity restored; this test has no
   network dependency.
2. In Android Studio, run `LiveIsolatedPdfParserProcessDeathIntegrationTest`.
3. Confirm **1 test passed**. A transient service crash in the emulator is expected;
   it must not show a Memora app crash or require a production setting.
4. If Android reports that the isolated process cannot be uniquely identified, stop.
   Send the result rather than loosening the PID-selection rule.

## Explicit non-claims

This test does not prove user-PDF safety, source access, persistence, page-text result
transport, memory/battery/thermal limits, semantic understanding, recovery UI, or
reconnection scheduling. Those remain distinct ADR-017/product gates.

## Verification result

On 2026-07-23, the user ran
`LiveIsolatedPdfParserProcessDeathIntegrationTest` on the Medium Phone emulator:
**1 test passed**. The test bound the existing private isolated service, retained a
test-only pipe so an ordinary-process request stayed in flight, located exactly one
package-matching process with an isolated UID, and induced `am crash <pid>` for that
PID only. The ordinary process received a retryable, content-free failure, closed its
descriptor, and reported the connection unavailable.

The test did not open or create a user PDF, use a URI/SAF grant/file path, change the
production Binder interface or service behavior, add a debug kill switch, persist
data, invoke AI, make a network request, or emit source content. It confirms the
narrow live process-death recovery gate only.

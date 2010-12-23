@echo off
if "%1"=="" echo usage: cirrus ^<server^|console^> &goto:eof
set _JVMARGS=
if /i %1 == debugjs set _JVMARGS=%_JVMARGS% -Ddebugjs &shift
if /i %1 == debugjvm set _JVMARGS=%_JVMARGS% -agentlib:jdwp=server=y,suspend=n,transport=dt_socket,address=2718 &shift
if /i %1 == suspendjvm set _JVMARGS=%_JVMARGS% -agentlib:jdwp=server=y,suspend=y,transport=dt_socket,address=2718 &shift

java %_JVMARGS% -jar lib/runtime/js.jar script/%1.js

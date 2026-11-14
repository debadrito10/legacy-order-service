@echo off
java -Xmx256m -Xlog:gc*:file=gc.log:time -jar target\legacy-order-service-2.0.0.jar

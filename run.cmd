@echo off
java -Xmx256m -Xlog:gc*:file=gc.log:time -jar target\legacy-order-service-1.0.0.jar

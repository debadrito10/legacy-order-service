@echo off
java -Xmx256m -XX:+UseConcMarkSweepGC -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:gc.log -jar target\legacy-order-service-1.0.0.jar

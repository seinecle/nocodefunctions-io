#!/bin/bash
cd /home/waouh/nocodeapp-web/api

# Kill existing process and wait for it to terminate completely
pkill -f "import-api" || true
sleep 2  # Give JVM time to perform graceful shutdown

# Ensure process is really terminated (in case graceful shutdown wasn't enough)
while pgrep -f "import-api" > /dev/null; do
    echo "Waiting for previous process to terminate..."
    sleep 2
done

rm -f nohup.out
nohup java -Dname=import-api --enable-preview -jar NocodeIOWebServices-1.0.jar net.clementlevallois.nocodeimportwebservices.APIController > /home/waouh/nocodeapp-web/io/nohup.out 2>&1 &
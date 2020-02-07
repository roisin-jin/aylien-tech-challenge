#!/bin/bash
echo "Starting mysql"
cd mysql
docker-compose up -d

echo "String python app"
cd ../app
python3 app.py --port 8080 --monitor 8081 & P1=$!

echo "Staring akka-http"
cd ../akka-http-paint-factory
sbt clean compile
sbt run
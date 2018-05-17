#!/bin/bash

set -e

echo "Starting mysql services..."
docker-compose up -d mysql
echo "Waiting for mysql ready, sleep for 30s"
sleep 30

# Start Superset
echo "Starting Superset..."
docker-compose up -d superset
echo "Waiting for Superset ready, sleep for 30s"
sleep 30

# Inititalize Demo
docker-compose exec superset superset-init

echo "Navigate to http://localhost:8088"
echo -n "Press RETURN to bring down demo"
read down
docker-compose down -v

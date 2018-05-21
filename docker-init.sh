#!/bin/bash

set -e

# Inititalize Demo
docker-compose exec superset superset-init

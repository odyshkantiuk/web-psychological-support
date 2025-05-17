#!/bin/bash

echo "Stopping containers..."
docker-compose down

echo "Container status:"
docker-compose ps

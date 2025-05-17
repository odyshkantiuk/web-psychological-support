#!/bin/bash

echo "Building and starting containers..."
docker-compose up -d

echo "Container status:"
docker-compose ps

echo "Application is running at http://localhost:8080"

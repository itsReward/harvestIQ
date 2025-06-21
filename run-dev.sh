#!/bin/bash

# run-dev.sh - Script to run the application in development mode

echo "🚀 Starting HarvestIQ in Development Mode..."
echo "📊 This will enable debug logging and CORS for localhost:3000"
echo ""

# Create logs directory if it doesn't exist
mkdir -p logs

# Run with development profile
export SPRING_PROFILES_ACTIVE=dev

echo "🔧 Active Profile: dev"
echo "🌐 CORS enabled for: http://localhost:3000"
echo "📝 Debug logging enabled"
echo "🔍 SQL logging enabled"
echo ""

./gradlew bootRun --args='--spring.profiles.active=dev'
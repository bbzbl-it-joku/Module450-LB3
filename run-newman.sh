#!/bin/bash

NEWMAN_DIR="reports/newman"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Logging functions
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR:${NC} $1"
}

warning() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] WARNING:${NC} $1"
}

# Cleanup function
cleanup() {
    log "Cleaning up..."
    if [ -f "$NEWMAN_DIR/spring.pid" ]; then
        log "Stopping Spring Boot application..."
        kill $(cat $NEWMAN_DIR/spring.pid)
        rm $NEWMAN_DIR/spring.pid
    fi
    exit 0
}

# Check for clean command
if [ "$1" = "clean" ]; then
    log "Cleaning up reports directory..."
    rm -rf $NEWMAN_DIR
    exit 0
fi

# Validate iteration count if provided
if [ ! -z "$1" ] && ! [[ "$1" =~ ^[0-9]+$ ]]; then
    error "Iteration count must be a number"
    exit 1
fi

# Trap for Ctrl+C
trap cleanup SIGINT SIGTERM

# Create directories
REPORT_DIR="$NEWMAN_DIR/$(date +'%Y-%m-%d_%H-%M-%S')"
mkdir -p "$REPORT_DIR/html" # Explicitly create html directory

# Start Spring Boot
log "Starting Spring Boot application..."
mvn spring-boot:run >$NEWMAN_DIR/spring.log 2>&1 &
echo $! >$NEWMAN_DIR/spring.pid

# Wait for Spring Boot to start
log "Waiting for Spring Boot to start..."
while ! curl -s http://localhost:8080/api/persons >/dev/null; do
    sleep 1
done
log "Spring Boot is running!"

# Run Newman tests
log "Starting Newman tests..."
newman run collection.json \
    --reporters cli,htmlextra \
    --reporter-htmlextra-export "$REPORT_DIR/html/report.html" \
    --reporter-htmlextra-title "API Tests - ${1:-1} Iterations" \
    --reporter-htmlextra-testPaging true \
    --reporter-htmlextra-showOnlyFails false \
    --reporter-htmlextra-logs true \
    --iteration-count ${1:-1} \
    --bail # Stop on first error

NEWMAN_EXIT_CODE=$?

if [ $NEWMAN_EXIT_CODE -eq 0 ]; then
    log "Newman tests completed successfully"
else
    error "Newman tests failed with exit code $NEWMAN_EXIT_CODE"
fi

# Stop Spring Boot
log "Stopping Spring Boot application..."
kill $(cat $NEWMAN_DIR/spring.pid)
rm $NEWMAN_DIR/spring.pid

# Generate test summary
echo ""
log "Test Summary:"
echo "----------------------------------------"
echo "HTML Report: $REPORT_DIR/html/report.html"
echo "Spring Boot Log: $NEWMAN_DIR/spring.log"
echo "----------------------------------------"

# Check if HTML report was created
if [ ! -f "$REPORT_DIR/html/report.html" ]; then
    warning "HTML report was not created. Please check newman error output."
fi

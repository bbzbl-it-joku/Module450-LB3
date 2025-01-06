#!/bin/bash

# Farben für Output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Logging Funktion
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR:${NC} $1"
}

warning() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] WARNING:${NC} $1"
}

# Cleanup Funktion
cleanup() {
    log "Cleaning up..."
    if [ -f "spring.pid" ]; then
        log "Stopping Spring Boot application..."
        kill $(cat spring.pid)
        rm spring.pid
    fi
    exit 0
}

# Trap für Ctrl+C
trap cleanup SIGINT SIGTERM

# Verzeichnisse erstellen
REPORT_DIR="test-reports/$(date +'%Y-%m-%d_%H-%M-%S')"
mkdir -p $REPORT_DIR

# Spring Boot starten
log "Starting Spring Boot application..."
mvn spring-boot:run > spring.log 2>&1 & echo $! > spring.pid

# Warten bis Spring Boot gestartet ist
log "Waiting for Spring Boot to start..."
while ! curl -s http://localhost:8080/api/persons > /dev/null
do
    sleep 1
done
log "Spring Boot is running!"

# JMeter Test ausführen
log "Starting JMeter test..."
jmeter -n -t person-test.jmx \
    -l "$REPORT_DIR/results.jtl" \
    -e -o "$REPORT_DIR/html" \
    -j "$REPORT_DIR/jmeter.log"

if [ $? -eq 0 ]; then
    log "JMeter test completed successfully"
else
    error "JMeter test failed"
fi

# Report Pfad anzeigen
log "Test report is available at: $REPORT_DIR/html/index.html"

# Spring Boot stoppen
log "Stopping Spring Boot application..."
kill $(cat spring.pid)
rm spring.pid

# Zusammenfassung anzeigen
echo ""
log "Test Summary:"
echo "----------------------------------------"
echo "Report Location: $REPORT_DIR/html/index.html"
echo "Results File: $REPORT_DIR/results.jtl"
echo "JMeter Log: $REPORT_DIR/jmeter.log"
echo "Spring Boot Log: spring.log"
echo "----------------------------------------"

# Öffne den Report im Standard-Browser (funktioniert auf den meisten Unix-Systemen)
if command -v xdg-open > /dev/null; then
    xdg-open "$REPORT_DIR/html/index.html" 2>&1 /dev/null
elif command -v open > /dev/null; then
    open "$REPORT_DIR/html/index.html" 2>&1 /dev/null
else
    warning "Could not open report automatically. Please open manually at: $REPORT_DIR/html/index.html"
fi
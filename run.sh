#!/bin/bash
# Compile and run Pharmacy Expiry Shelf Check
set -e
cd "$(dirname "$0")"
echo "Compiling..."
javac -encoding UTF-8 Medicine.java SampleData.java StockManager.java PharmacyApp.java
echo "Launching..."
java PharmacyApp

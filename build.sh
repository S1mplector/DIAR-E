#!/bin/bash

echo "Building DIAR-E..."
mvn clean install -DskipTests

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Build successful!"
    echo ""
    echo "To run DIAR-E:"
    echo "  cd src/bootstrap/diar-bootstrap"
    echo "  mvn exec:java"
    echo ""
    echo "Or use the run.sh script:"
    echo "  ./run.sh"
else
    echo ""
    echo "❌ Build failed. Please check the errors above."
    exit 1
fi

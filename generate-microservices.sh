#!/bin/bash

# Script to generate remaining microservices for tracking service

SERVICES=("location-service" "event-service" "notification-service" "analytics-service" "api-gateway")
BASE_DIR="/Users/sudhanshu/fourkites_workspace/tracking-service-segregation"

for SERVICE in "${SERVICES[@]}"; do
    echo "Generating $SERVICE..."
    
    SERVICE_NAME=$(echo $SERVICE | sed 's/-service//' | sed 's/-/_/g')
    PACKAGE_NAME="com.fourkites.$SERVICE_NAME"
    
    # Copy and modify the pom.xml from shipment-service
    cp $BASE_DIR/shipment-service/pom.xml $BASE_DIR/$SERVICE/pom.xml
    sed -i '' "s/shipment-service/$SERVICE/g" $BASE_DIR/$SERVICE/pom.xml
    sed -i '' "s/Shipment Service/${SERVICE_NAME^} Service/g" $BASE_DIR/$SERVICE/pom.xml
    sed -i '' "s/com.fourkites.shipment/$PACKAGE_NAME/g" $BASE_DIR/$SERVICE/pom.xml
    
    echo "Generated $SERVICE structure"
done

echo "All microservices have been generated!"
echo "Run 'mvn clean install' in each service directory to build"
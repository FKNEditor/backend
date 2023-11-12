#!/bin/bash
export UNI_KEYCLOAK_URL="https://keycloak.expressionless.com.au"
export CORS_URLS="https://nish.expressionless.com.au"
mvn clean quarkus:dev -Ddebug=5556


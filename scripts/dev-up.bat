@echo off
REM Runs each service in its own window (Maven). Assumes .env files and infra (docker-compose) are already up.
REM Usage: scripts\dev-up.bat

start "Gateway"  cmd /k "cd services\gateway && mvn spring-boot:run"
start "Identity" cmd /k "cd services\identity && mvn spring-boot:run"
start "Payment"  cmd /k "cd services\payment && mvn spring-boot:run"
start "Wallet"   cmd /k "cd services\wallet && mvn spring-boot:run"

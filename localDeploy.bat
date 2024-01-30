@echo off
setlocal EnableDelayedExpansion

set prefix=exam-manager-

set spring_boot_service=%prefix%springbootapp
set graylog_service=%prefix%graylog
set mongo_service=%prefix%mongo
set elasticsearch_service=%prefix%elasticsearch
set postgresql_service=%prefix%postgresql

set "services_not_found=0"
set "services_processed=0"

CALL :check_and_start_container %spring_boot_service%
CALL :check_and_start_container %graylog_service%
CALL :check_and_start_container %mongo_service%
CALL :check_and_start_container %elasticsearch_service%
CALL :check_and_start_container %postgresql_service%

:check_and_start_container
echo Container is %~1
set "container_name=%~1"
set /a services_processed+=1
FOR /F "tokens=*" %%i IN ('docker ps -a -q --no-trunc --filter "name=!container_name!"') DO (
    SET "container_id=%%i"
    FOR /F "tokens=*" %%j IN ('docker ps -q --no-trunc --filter "id=%%i"') DO SET "running_container_id=%%j"
    IF "!running_container_id!"=="" (
        ECHO Starting !container_name!...
        docker start %%i
    ) ELSE (
        ECHO !container_name! is already running.
    )
    goto :container_checked
)
set /a services_not_found+=1

:container_checked
goto :passed

:passed
IF !services_processed! GTR 5 (
    IF !services_not_found! GTR 0 (
        ECHO One or more required containers do not exist. Starting all services with Docker Compose...
        docker-compose up -d
        
        TIMEOUT /T 60
        ECHO Configuring Graylog...
        curl -X POST "http://127.0.0.1:9000/api/system/inputs" -H "X-Requested-By: Local" -H "Content-Type: application/json" -H "Authorization: Basic YWRtaW46YWRtaW4=" -d "{"""title""": """UDP Input""", """type""": """org.graylog2.inputs.gelf.udp.GELFUDPInput""", """configuration""": {"""bind_address""": """0.0.0.0""", """port""": 12201, """recv_buffer_size""": 262144, """number_worker_threads""": 12, """override_source""": null, """decompress_size_limit""": 8388608}, """global""": true}"

    ) ELSE (
        ECHO All required containers are up and running. No further action is taken.
    )
)

:end
endlocal

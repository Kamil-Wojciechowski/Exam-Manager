@echo off
setlocal

:: Assuming the default project name is the directory name where docker-compose.yml resides
for /f "tokens=*" %%i in ('cd') do set project_name=%%~ni

set graylog=%project_name%_graylog_1
set mongo=%project_name%_mongo_1
set elasticsearch=%project_name%_elasticsearch_1

call :check_and_start_container %graylog%
call :check_and_start_container %mongo%
call :check_and_start_container %elasticsearch%

:: Check if all containers are running before proceeding
docker ps | findstr /C:"%graylog%" >nul && (
    docker ps | findstr /C:"%mongo%" >nul && (
        docker ps | findstr /C:"%elasticsearch%" >nul && (
            echo All required containers are running.
            call :configure_graylog
        )
    )
) || (
    echo One or more required containers are not running. Starting Docker Compose...
    docker-compose up -d
    timeout /t 60
    call :configure_graylog
)

goto :eof

:check_and_start_container
docker ps -a -q --no-trunc -f name=^/%1$ | findstr /C:"%1" >nul && (
    echo %1 is already running.
) || (
    echo Starting %1...
    docker start %1
)
goto :eof

:configure_graylog
echo Configuring Graylog...
curl --location "http://127.0.0.1:9000/api/system/inputs" ^
    --header "X-Requested-By: Local" ^
    --header "Content-Type: application/json" ^
    --header "Authorization: Basic YWRtaW46YWRtaW4=" ^
    --data "{""title"": ""UDP Input"",""type"": ""org.graylog2.inputs.gelf.udp.GELFUDPInput"",""configuration"": {""bind_address"": ""0.0.0.0"",""port"": 12201,""recv_buffer_size"": 262144,""number_worker_threads"": 12,""override_source"": null,""decompress_size_limit"": 8388608},""global"": true}"
goto :eof

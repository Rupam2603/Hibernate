@echo off
echo ========================================================
echo Starting Hibernate E-Commerce Web Application on Browser
echo ========================================================
.\mvnw.cmd compile exec:java -Dexec.mainClass="com.ecommerce.web.WebServer"

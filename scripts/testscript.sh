#!/bin/bash

# For testing purposes
# Launches three servers, a middleware and two clients, all running on localhost.

if [ $# -eq 1 ]; then
	FILE=$1
fi

xterm -e ./serverscript.sh 6060 &
xterm -e ./serverscript.sh 6161 &
xterm -e ./serverscript.sh 6262 &
read -p "When all servers are ready, press enter to launch middleware"
xterm -e "./middlewarescript.sh localhost 6060 localhost 6161 localhost 6262 6363" &
read -p "When middleware is ready, press enter to launch clients"
echo "Press ctrl-C to end the application"
if [ -z FILE ]; then
	xterm -e "./clientscript.sh localhost 6363" &
	xterm -e "./clientscript.sh localhost 6363"
else
	# Only one client with file as input
	xterm -e "./clientscript.sh localhost 6363 $FILE; bash"
fi
exit 0

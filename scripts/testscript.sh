#!/bin/bash

# For testing purposes
# Launches three servers, a middleware and two clients, all running on localhost.

if [ $# -eq 1 ]; then
	FILE=$1
	echo "file is $FILE"
fi

rm -f ../resources/*/*

xterm -e "./serverscript.sh 6060; bash" &
xterm -e "./serverscript.sh 6161; bash" &
xterm -e "./serverscript.sh 6262; bash" &
read -p "When all servers are ready, press enter to launch middleware"
xterm -e "./middlewarescript.sh localhost 6060 localhost 6161 localhost 6262 6363; bash" &
read -p "When middleware is ready, press enter to launch clients"
echo "Press ctrl-C to end the application"
if [ $# -eq 1 ]; then
	# Only one client with file as input
	xterm -e "./clientscript.sh localhost 6363 $FILE; bash"
else
	xterm -e "./clientscript.sh localhost 6363; bash" &
	xterm -e "./clientscript.sh localhost 6363; bash"
fi
exit 0

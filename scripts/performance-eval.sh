#!/bin/bash

# For testing purposes
# Launches three servers, a middleware and two clients, all running on localhost.

cd ..

if [ $# -eq 1 ];then
	ITER=$1
else
	echo "Usage: ./performance-eval.sh <iterations>"
	exit -1
fi

xterm -e ./serverscript.sh 6060 &
xterm -e ./serverscript.sh 6161 &
xterm -e ./serverscript.sh 6262 &
read -p "When all servers are ready, press enter to launch middleware"
xterm -e "./middlewarescript.sh localhost 6060 localhost 6161 localhost 6262 6363" &
read -p "When middleware is ready, press enter to launch clients"
echo "Press ctrl-C to end the application"
xterm -e "./testclient.sh localhost 6363 $ITER"
exit 0

#!/bin/bash

SRC=${PWD}

cd ..

VAR=${PWD}

export CLASSPATH=$VAR/server/:$VAR/middleware/

#Setting values for server name and server port

if [ $# -eq 2 ];then 
	SERVERNAME=$1
	PORT=$2
elif [ $# -eq 3 ];then 
	SERVERNAME=$1
	PORT=$2
	FILE=$3
else
	echo "Usage: ./clientscript.sh <middleware-hostname> <middleware-port>"
	exit -1
fi

echo "Server is $SERVERNAME"
echo "Port is $PORT"
echo "Starting client..."
cd ./client/
echo "grant codeBase \"file:$VAR/client\" { permission java.security.AllPermission; };" > client.policy
javac client/*.java
if [ $# -eq 3 ];then
	java -Djava.security.policy=client.policy -classpath .:../server:../middleware client.Client $SERVERNAME $PORT $SRC/$FILE
else
	java -Djava.security.policy=client.policy -classpath .:../server:../middleware client.Client $SERVERNAME $PORT
fi

#!/bin/bash

#Script that will do all necessary work to start a Resource Manager server
#	- Sets CLASSPATH
#	- Compiles scripts
#	- Bring user to right directory
#
#Requires:
#	1 - Port number of the server
#		> RMIRegistry will be set using this number

cd ..

VAR=${PWD}

export CLASSPATH=$VAR/server/:$VAR/middleware

if [ $# -eq 7 ];then
	
	echo "Server 1 is $1"
	SERVER1NAME=$1

	echo "Server 1 port is $2"
	SERVER1PORT=$2

	echo "Server 2 is $3"
	SERVER2NAME=$3

	echo "Server 2 port is $4"
	SERVER2PORT=$4

	echo "Server 3 is $5"
	SERVER3NAME=$5

	echo "Server 3 port is $6"
	SERVER3PORT=$6

	echo "RMI port is $7"
	PORT=$7
else
	echo "Usage: ./middlewarescript.sh <server1-hostname> <server1-port> <server2-hostname> <server2-port> <server3-hostname> <server3-port> <middleware-listenport>"
	exit -1
fi

echo "Starting RMI Registry..."
rmiregistry $PORT &

echo "Starting middleware..."
cd ./middleware/
echo "grant codeBase \"file:$VAR/middleware\" { permission java.security.AllPermission; }; grant { permission java.security.AllPermission; };" > middleware.policy
javac */*.java
java -Djava.security.policy=middleware.policy -classpath .:../server middleware.Middleware $SERVER1NAME $SERVER1PORT $SERVER2NAME $SERVER2PORT $SERVER3NAME $SERVER3PORT $PORT

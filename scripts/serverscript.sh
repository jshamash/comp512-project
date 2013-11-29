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

#Setting values for server name and server port
if [ $# -eq 1 ];then
	PORT=$1
else
	echo "Usage: ./serverscript.sh <listen-port>"
	exit -1
fi
echo "port is $PORT"

cd ./server/

echo "Starting RMI Registry"

rmiregistry $PORT &

echo "Starting server..."
javac */*.java 
echo "grant codeBase \"file:$VAR/server\" { permission java.security.AllPermission; };" > server.policy
java -Djava.security.policy=server.policy -Djava.rmi.server.codebase=file:$VAR/server/ ResImpl.ResourceManagerImpl $PORT


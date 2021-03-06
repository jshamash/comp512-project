#!/bin/bash

cd ..

VAR=${PWD}

export CLASSPATH=$VAR/server/:$VAR/middleware/

#Setting values for server name and server port
if [ $# -eq 3 ];then
	SERVERNAME=$1
	PORT=$2
	ITER=$3
else
	echo "Usage: ./clientscript.sh <middleware-hostname> <middleware-port> <iterations>"
	exit -1
fi

echo "Server is $SERVERNAME"
echo "Port is $PORT"
echo "$ITER iterations"
echo "Starting client..."
cd ./test
echo "grant codeBase \"file:$VAR/test\" { permission java.security.AllPermission; };" > ./test/client.policy
javac test/*.java
java -Djava.security.policy=test/client.policy -classpath .:../server:../middleware test.PerformanceEvaluationOneClient $SERVERNAME $PORT $ITER
cat results.txt
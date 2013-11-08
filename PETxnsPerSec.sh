#!/bin/bash

VAR=${PWD}

export CLASSPATH=$VAR/server/:$VAR/middleware/

#Setting values for server name and server port
if [ $# -eq 5 ];then
	SERVERNAME=$1
	PORT=$2
	CLIENTS=$3
	TXNS=$4
	SECONDS=$5
else
	echo "Usage: ./PETxnsPerSec <middleware-hostname> <middleware-port> <num-clients> <txns-per-sec> <seconds>"
	exit -1
fi

echo "Server is $SERVERNAME"
echo "Port is $PORT"
echo "$CLIENTS clients"
echo "$TXNS transactions per sec"
echo "Test will run for $SECONDS seconds"

TXNSPERCLIENT=$(printf "%.0f" $(bc -l <<< "($TXNS / $CLIENTS)"))
echo "Each client should perform about $TXNSPERCLIENT transactions per second."

cd ./test
echo "grant codeBase \"file:$VAR/test\" { permission java.security.AllPermission; };" > ./test/client.policy
javac test/*.java

COUNTER=0
while [ $COUNTER -lt $CLIENTS ]; do
	echo "Starting client $counter"
	xterm -e "java -Djava.security.policy=test/client.policy -classpath .:../server:../middleware test.DistributedResponseTimeEvaluation $SERVERNAME $PORT $TXNSPERCLIENT $SECONDS $COUNTER" & 
	let COUNTER=COUNTER+1
done

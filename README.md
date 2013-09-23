comp512-project
===============

Example setup:

PREPARING CODE
cd comp512/comp512-project
git pull origin master
javac server/*/*.java middleware/Middleware.java client/client.java


SERVER (lab2-1, lab2-2, lab2-3)
NOTE: in this case each RM is run on a different machine.
rmiregistry 6961 &
rmiregistry 6962 &
rmiregistry 6963 &
export CLASSPATH="/home/2010/jshama1/comp512/comp512-project/server/"
cd server
java -Djava.security.policy=server.policy -Djava.rmi.server.codebase=file:/home/2010/jshama1/comp512/comp512-project/server/ ResImpl.ResourceManagerImpl 6961 &
java -Djava.security.policy=server.policy -Djava.rmi.server.codebase=file:/home/2010/jshama1/comp512/comp512-project/server/ ResImpl.ResourceManagerImpl 6962 &
java -Djava.security.policy=server.policy -Djava.rmi.server.codebase=file:/home/2010/jshama1/comp512/comp512-project/server/ ResImpl.ResourceManagerImpl 6963 &

MIDDLEWARE (lab2-4)
cd middleware
rmiregistry 6969 &
export CLASSPATH="/home/2010/jshama1/comp512/comp512-project/server/"
java  -Djava.security.policy=middleware.policy -classpath .:../server Middleware lab2-1 6961 lab2-2 6962 lab2-3 6963 6969

CLIENT (teaching)
cd client
java -Djava.security.policy=client.policy -classpath .:../server client lab2-4 6969
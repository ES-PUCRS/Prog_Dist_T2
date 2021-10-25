#!/bin/bash


supernode_ip="192.168.100.5"
supernode_port="9050"

localport=9040





args="$supernode_ip:$supernode_port super $localport"


if [ ! -d "./_class" ]
then
	mkdir _class
fi

javac ./*.java -d "./_class"
if [ $? -eq 0 ]
then

	if [[ $1 -eq "" ]]
	then
		echo ARGS: $args
		java -cp ./_class P2PNode $args
	elif [[ $1 -eq "-1" ]]
	then
		xterm -T "RMISemaphore" -ls -e $semaphore_command &
		sleep 3
		xterm -T "RMIServer" -ls -e $server_command &
		sleep 3
		xterm -T "RMIClient" -ls -e $client_command &

	elif [[ $1 -eq "clear" ]]
	then
		rm -f ./_class
	fi
fi
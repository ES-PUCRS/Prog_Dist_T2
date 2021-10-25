#!/bin/bash

source config.sh

if [ ! -d "./_class" ]
then
	mkdir _class
fi

javac ./*.java -d "./_class"
if [ $? -eq 0 ]
then

	if [[ $1 -eq "" ]]
	then
		echo ARGS: $args_1
		$LOAD $args_1

	elif [[ $1 -eq "1" ]]
	then
		xterm -geometry $uppers_window  -T "$localtype_1 NODE 1> $localport_1" -ls -e $LOAD$args_1 &
		sleep 2
		xterm -T "$localtype_2 NODE 2" -ls -e $LOAD$args_2 &

	elif [[ $1 -eq "2" ]]
	then
		xterm -geometry $uppers_window  -T "$localtype_1 NODE 1> $localport_1" -ls -e $LOAD$args_1 &
		sleep 2

		xterm -T "$localtype_2 NODE 2> $localport_2" -ls -e $LOAD$args_2 &
		sleep 2
		
		xterm -geometry $lower_window -T "$localtype_3 NODE 3> $localport_3" -ls -e $LOAD$args_3 &

	elif [[ $1 -eq "clear" ]]
	then
		rm -f ./_class
	fi
fi
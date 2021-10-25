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

	elif [[ $1 -eq "0" ]]
	then
		echo ARGS: $args_1
		xterm -T "$localtype_1 NODE 1" -ls -e $LOAD$args_1 &

	elif [[ $1 -eq "1" ]]
	then
		xterm -T "$localtype_1 NODE 1" -ls -e $LOAD$args_1 &
		sleep 3
		xterm -T "$localtype_2 NODE 2" -ls -e $LOAD$args_2 &
		sleep 3
		xterm -T "$localtype_3 NODE 3" -ls -e $LOAD$args_3 &

	elif [[ $1 -eq "clear" ]]
	then
		rm -f ./_class
	fi
fi
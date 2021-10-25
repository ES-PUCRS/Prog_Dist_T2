#!/bin/bash

MAIN_CLASS=P2PNode
LOAD="java -cp ./_class $MAIN_CLASS "


supernode_ip_1="192.168.100.5"
supernode_port_1="9005"
localtype_1=super
localport_1=9004

args_1="$supernode_ip_1:$supernode_port_1 $localtype_1 $localport_1"





supernode_ip_2="192.168.100.5"
supernode_port_2="9005"
localtype_2=super
localport_2=9004

args_2="$MAIN_CLASS $supernode_ip_2:$supernode_port_2 $localtype_2 $localport_2"





supernode_ip="192.168.100.5"
supernode_port="9005"
localtype=super
localport=9004

args_3="$MAIN_CLASS $supernode_ip_3:$supernode_port_3 $localtype_3 $localport_3"
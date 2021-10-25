# !/bin/bash

MAIN_CLASS=P2PNode
LOAD="java -cp ./_class $MAIN_CLASS "
uppers_window=80x20+918+45
middle_window=80x20+918+354
lower_window=80x20+918+662


supernode_ip_1="192.168.100.4"
supernode_port_1="9014"
localtype_1=SUPER
localport_1=9004

args_1="$supernode_ip_1:$supernode_port_1 $localtype_1 $localport_1"





supernode_ip_2="192.168.100.4"
supernode_port_2="9004"
localtype_2=SUPER
localport_2=9014

args_2="$supernode_ip_2:$supernode_port_2 $localtype_2 $localport_2"





supernode_ip_3="192.168.100.4"
supernode_port_3="9004"
localtype_3=SUPER
localport_3=9024

args_3="$supernode_ip_3:$supernode_port_3 $localtype_3 $localport_3"


supernode_ip_4="192.168.100.4"
supernode_port_4="9014"
localtype_4=SUPER
localport_4=9034

args_4="$supernode_ip_4:$supernode_port_4 $localtype_4 $localport_4"
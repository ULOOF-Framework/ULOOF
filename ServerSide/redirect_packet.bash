#!/bin/bash

platform='unknown'
user=`whoami`
unamestr=`uname`
if [[ "$unamestr" == 'Linux' ]]; then
   platform='linux'
   nic='wlp3s0'
   adb="/home/alessio/Android/Sdk/platform-tools/adb"
elif [[ "$unamestr" == 'Darwin' ]]; then
   platform='mac'
   adb="/Users/$user/Library/Android/sdk/platform-tools/adb"
   nic='en1'
fi

nic='wlp3s0'
vm=`VBoxManage list vms|grep 'Google Nexus 5'|head| sed 's/^[^{]*{//'|sed 's/}//'`

# adb='/Users/youf3/Library/Android/sdk/platform-tools/adb'

#ipaddr=`ifconfig $nic| sed -En 's/127.0.0.1//;s/192.168.*.//;s/.*inet (addr:)?(([0-9]*\.){3}[0-9]*).*/\2/p'`
#vmipaddr=`VBoxManage guestproperty get $vm androvm_ip_management| sed 's/Value: //'`

echo $nic
echo $ipaddr
echo $vmipaddr

echo "$adb -s   -a forward tcp:8080 tcp:8080" 
$adb -s 192.168.57.101:5555 -a forward tcp:8080 tcp:8080

echo "redir --laddr=132.227.79.200   --lport=8080 --caddr=localhost --cport=8080"
redir --laddr=132.227.79.161   --lport=8080 --caddr=localhost --cport=8080

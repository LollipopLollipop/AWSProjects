#!/bin/bash
#sudo cp -R /var/lib/mysql/ /home/mysql_backup/
#sudo service mysql stop
#sudo mount --bind /home/mysql_backup/ /var/lib/mysql
#sudo service mysql start

export JAVA_HOME=/usr/lib/jvm/java-6-openjdk/jre/
export AWS_CLOUDWATCH_HOME=/home/ubuntu/CloudWatch-1.0.20.0/
export PATH=$AWS_CLOUDWATCH_HOME/bin:$PATH;
export AWS_CREDENTIAL_FILE=/home/ubuntu/CloudWatch-1.0.20.0/credential-file-path.template

python /home/ubuntu/calcTPS.py


#!/bin/bash
sudo cp -R /var/lib/mysql/ /home/mysql_backup/
sudo service mysql stop
sudo mount --bind /home/mysql_backup/ /var/lib/mysql
sudo service mysql start


#!/bin/bash
# Ensures the MariaDB runtime socket dir exists (tmpfs is wiped on pod restart), then runs the server.
mkdir -p /var/run/mysqld
chown -R mysql:mysql /var/run/mysqld
exec /usr/sbin/mariadbd --user=mysql

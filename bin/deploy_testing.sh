#!/bin/bash

HOST=10.1.1.66
USER=developer
ONE="superset one bin Makefile .git"
DEPS="../kylinpy"
DST=/opt/webapps/One

ssh $USER@$HOST "mkdir -p $DST"
rsync -avz $ONE $USER@$HOST:$DST --delete
rsync -avz $DEPS $USER@$HOST:$DST --delete

ssh $USER@$HOST "supervisorctl restart superset"

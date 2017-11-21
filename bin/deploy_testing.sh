#!/bin/bash

HOST=10.1.1.66
USER=developer
ONE="superset one bin Makefile"
DEPS="../kylinpy"
DST=/opt/webapps/One

ssh $USER@$HOST "mkdir -p $DST"
rsync -avz --exclude=*.pyc $ONE $USER@$HOST:$DST --delete
rsync -avz --exclude=*.pyc $DEPS $USER@$HOST:$DST --delete

ssh $USER@$HOST "supervisorctl restart superset"

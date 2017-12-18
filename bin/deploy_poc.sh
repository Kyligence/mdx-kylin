#!/bin/bash

HOST=azure
USER=developer
ONE="superset one bin Makefile"
DEPS="../kylinpy"
DST=/opt/webapps/One

ssh $USER@$HOST "mkdir -p $DST"
rsync -avz $ONE $USER@$HOST:$DST --delete
rsync -avz $DEPS $USER@$HOST:$DST --delete

ssh $USER@$HOST "sudo supervisorctl restart superset"

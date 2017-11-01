#!/bin/bash
#!/usr/bin/env VIRTUAL_ENV

ROOT_DIR=$1

DB_IMAGE=mysql:latest
DB_CONTAINER=superset-db
DB_USER=root
DB_PASSWORD=root
DB_DATADIR=$ROOT_DIR/mysql_data

if [[ $ROOT_DIR == "" ]]; then
    echo "Error: Root dir is empty"
    return 127
fi

export SUPERSET_CONFIG_PATH=$ROOT_DIR/one/config.py

if [[ $(uname) == 'Linux' ]]; then
    sudo apt -y install supervisor
fi

function db_client {
    if [[ "$(mysql_config --version 2> /dev/null)" == "" ]]; then
        if [[ $(uname) == 'Linux' ]]; then
            sudo apt -y install default-libmysqlclient-dev
        fi

        if [[ $(uname) == 'Darwin' ]]; then
            brew install mysql
        fi
    fi
}

function python_env {
    if [[ "$(virtualenvwrapper 2> /dev/null)" == "" ]]; then
        sudo pip install --upgrade pip
        sudo pip install virtualenvwrapper
    fi
    source $(which virtualenvwrapper.sh)
    mkvirtualenv superset
    workon superset || return 127

    echo $ROOT_DIR > $VIRTUAL_ENV/.project
    rm $VIRTUAL_ENV/bin/postactivate
    rm $VIRTUAL_ENV/bin/predeactivate
    ln -s $ROOT_DIR/bin/postactivate $VIRTUAL_ENV/bin/postactivate
    ln -s $ROOT_DIR/bin/predeactivate $VIRTUAL_ENV/bin/predeactivate

    pip install -e superset
    pip install mysqlclient ipython ipdb rope jedi flake8 importmagic autopep8 yapf

    if [[ "$(pip list --format=columns | grep -E 'kylinpy|sqlalchemy_kylin')" == "" ]]; then
        echo 'Error: Please install kylinpy and sqlalchemy_kylin'
        return 127
    fi
}

function docker_db {
    if [[ "$(docker images -q $DB_IMAGE 2> /dev/null)" == "" ]]; then
        docker pull $DB_IMAGE
    fi

    if [[ "$(docker ps -q -f name=$DB_CONTAINER)" == "" ]]; then
        if [ "$(docker ps -aq -f status=exited -f status=created -f name=$DB_CONTAINER)" ]; then
            docker rm $DB_CONTAINER
        fi

        mkdir -p $DB_DATADIR
        docker run --name $DB_CONTAINER -v $DB_DATADIR:/var/lib/mysql -e MYSQL_ROOT_PASSWORD=$DB_USER -d -p 3306:3306 $DB_IMAGE
    fi

    while [ "$(docker exec -it $DB_CONTAINER mysql -h 127.0.0.1 -u $DB_USER -p$DB_PASSWORD -e 'show databases' 2> /dev/null | grep 'ERROR')" ]; do
        echo "waiting for container ready..."
        sleep 1
    done
    docker exec -it $DB_CONTAINER mysql -h 127.0.0.1 -u $DB_USER -p$DB_PASSWORD -e "CREATE DATABASE IF NOT EXISTS superset CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
}

function init {
    fabmanager create-admin --app superset --username admin --password admin --firstname admin --lastname admin --email admin@fab.org
    superset db upgrade
    superset init
}

db_client
python_env
docker_db
init

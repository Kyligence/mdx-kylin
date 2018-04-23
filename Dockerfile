FROM debian:stretch

# Configure environment
ENV LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    SUPERSET_HOME=/usr/local/superset \
    PYTHONPATH=/etc/superset:/usr/local/superset:$PYTHONPATH

# install dependencies
RUN mkdir /etc/superset && \
    mkdir /usr/local/superset && \
    apt-get update && \
    apt-get install -y \
        build-essential \
        curl \
        default-libmysqlclient-dev \
        libffi-dev \
        libldap2-dev \
        libpq-dev \
        libsasl2-dev \
        libssl-dev \
        openjdk-8-jdk \
        python-dev \
        python-pip && \
    curl -sL https://deb.nodesource.com/setup_8.x | bash - && \
    apt-get update && \
    apt-get install -y nodejs && \
    apt-get clean && \
    rm -r /var/lib/apt/lists/* && \
    pip install --no-cache-dir \
        flask-cors==3.0.3 \
        flask-mail==0.9.1 \
        flask-oauth==0.12 \
        flask_oauthlib==0.9.3 \
        gevent==1.2.2 \
        impyla==0.14.0 \
        mysqlclient==1.3.7 \
        psycopg2==2.6.1 \
        pyathenajdbc==1.2.0 \
        pyhive==0.5.0 \
        pyldap==2.4.28 \
        redis==2.10.5 \
        sqlalchemy-redshift==0.5.0 \
        sqlalchemy-clickhouse==0.1.1.post3 \
        Werkzeug==0.12.1 \
        kylinpy==1.0.12

# Install superset
COPY bin /usr/local/bin
COPY superset /usr/local/superset/superset
COPY one /usr/local/superset/one
RUN pip install -e /usr/local/superset/superset && \
    cd /usr/local/superset/superset/superset/assets && \
    npm install && npm run build && rm -rf node_module

# Configure Filesystem
VOLUME /etc/superset
WORKDIR /usr/local/superset

# Deploy application
EXPOSE 8088
HEALTHCHECK CMD ["curl", "-f", "http://localhost:8088/health"]
ENTRYPOINT ["superset"]
CMD ["runserver"]

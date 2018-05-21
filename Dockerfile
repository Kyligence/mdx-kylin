FROM debian:stretch

# Configure environment
ENV LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    SUPERSET_HOME=/usr/local/superset \
    PYTHONPATH=/etc/superset:/usr/local/superset:$PYTHONPATH

# install os dependencies
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
    apt-get clean && \
    rm -r /var/lib/apt/lists/*

# install python dependencies
RUN pip install --no-cache-dir \
        bleach==2.1.2 \
        boto3==1.4.7 \
        celery==4.1.0 \
        colorama==0.3.9 \
        cryptography==1.9 \
        flask==0.12.2 \
        flask-appbuilder==1.10.0 \
        flask-caching==1.4.0 \
        flask-compress==1.4.0 \
        flask-migrate==2.1.1 \
        flask-script==2.0.6 \
        flask-testing==0.7.1 \
        flask-wtf==0.14.2 \
        flower==0.9.2 \
        future==0.16.0 \
        geopy==1.11.0 \
        gunicorn==19.8.0 \
        humanize==0.5.1 \
        idna==2.6 \
        markdown==2.6.11 \
        pandas==0.22.0 \
        parsedatetime==2.0.0 \
        pathlib2==2.3.0 \
        polyline==1.3.2 \
        pydruid==0.4.2 \
        pyhive==0.5.1 \
        python-dateutil==2.6.1 \
        python-geohash==0.8.5 \
        pyyaml==3.12 \
        requests==2.18.4 \
        simplejson==3.13.2 \
        six==1.11.0 \
        sqlalchemy==1.2.2 \
        sqlalchemy-utils==0.32.21 \
        sqlparse==0.2.4 \
        thrift==0.11.0 \
        thrift-sasl==0.3.0 \
        unicodecsv==0.14.1 \
        unidecode==1.0.22 \
        contextlib2==0.5.5 \

        mysqlclient==1.3.7 \
        psycopg2-binary==2.7.4 \
        kylinpy==1.1.1

# Install superset
COPY bin/superset-init /usr/local/bin
COPY superset /usr/local/superset/superset
COPY one /usr/local/superset/one
RUN pip install -e /usr/local/superset/superset

# Configure Filesystem
VOLUME /etc/superset
VOLUME /usr/local/superset/superset
WORKDIR /usr/local/superset

# Deploy application
EXPOSE 8088
HEALTHCHECK CMD ["curl", "-f", "http://localhost:8088/health"]
ENTRYPOINT ["superset"]
CMD ["runserver"]

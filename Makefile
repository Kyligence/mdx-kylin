dev: export SUPERSET_CONFIG_PATH=$(shell pwd)/bin/superset_config_dev.py
dev: export SUPERSET_HOME=$(shell pwd)
dev: export PYTHONPATH=$(shell pwd):PYTHONPATH
dev:
	superset runserver -d

migrate: export SUPERSET_CONFIG_PATH=$(shell pwd)/bin/superset_config_dev.py
migrate: export SUPERSET_HOME=$(shell pwd)
migrate: export PYTHONPATH=$(shell pwd):PYTHONPATH
migrate:
	superset db migrate

initdev: export SUPERSET_CONFIG_PATH=$(shell pwd)/bin/superset_config_dev.py
initdev: export SUPERSET_HOME=$(shell pwd)
initdev: export PYTHONPATH=$(shell pwd):PYTHONPATH
initdev:
	@echo "Starting mysql services..."
	@docker run --name superset-db -v `pwd`/mysql_data:/var/lib/mysql -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=superset -d -p 3306:3306 mysql
	@echo "Sleeping for 20s"
	@sleep 20
	./bin/superset-init

build:
	docker build -f Dockerfile -t kyligence/superset-mod:latest .

publish:
	docker push kyligence/superset-mod:latest

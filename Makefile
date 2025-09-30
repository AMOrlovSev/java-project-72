# Корневой Makefile
APP_DIR = app

%:
	$(MAKE) -C $(APP_DIR) $@

setup:
	chmod +x $(APP_DIR)/gradlew
	$(MAKE) -C $(APP_DIR) setup

.PHONY: setup
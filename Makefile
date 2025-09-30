# Корневой Makefile
APP_DIR = app

%:
	$(MAKE) -C $(APP_DIR) $@

fix-permissions:
	chmod +x $(APP_DIR)/gradlew
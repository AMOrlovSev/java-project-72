# Для Unix/Linux/MacOS
setup:
	chmod +x ./gradlew
	./gradlew clean build

run-dist:
	./build/install/app/bin/app

build: setup

.PHONY: build setup run-dist



# Для Windows
build-win:
	gradlew.bat clean build

install-win:
	gradlew.bat installDist

run-dist-win: install-win
	call build/install/app/bin/app.bat

# Универсальная цель, которая соберет и запустит
all-win: build-win install-win run-dist-win

.PHONY: build-win install-win run-dist-win all-win
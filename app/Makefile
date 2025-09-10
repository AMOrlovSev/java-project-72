# Для Unix/Linux/MacOS
run-dist:
	./build/install/app/bin/app

build:
	./gradlew clean build

.PHONY: build



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
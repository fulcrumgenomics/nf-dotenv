.PHONY: all
all: assemble

.PHONY: assemble
assemble:
	./gradlew assemble

.PHONY: test
test:
	./gradlew test --warning-mode all

.PHONY: clean
clean:
	rm -rf .nextflow*
	rm -rf work
	rm -rf build
	./gradlew clean

.PHONY: install
install:
	./gradlew install

.POHNY: release
release:
	./gradlew releasePlugin

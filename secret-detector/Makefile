.PHONY: release

export DOCKER_ORG := expediadotcom
export DOCKER_IMAGE_NAME := haystack-pipes-secret-detector

docker_build:
	docker build -t $(DOCKER_IMAGE_NAME) -f build/docker/Dockerfile .

release: docker_build
	../deployment/scripts/publish-to-docker-hub.sh

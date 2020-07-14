#!/bin/sh

export VERSION=$(git rev-parse --short "$GITHUB_SHA")
export IMAGE=chusj/clin-fhir-server:$VERSION

docker build -t $IMAGE .

docker push $IMAGE


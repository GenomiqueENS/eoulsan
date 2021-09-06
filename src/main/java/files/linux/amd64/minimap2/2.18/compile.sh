#!/bin/bash

# This file contains the Docker instruction used to build the Minimap2
# executable for Eoulsan

VERSION=2.18
DOCKER_IMAGE="minimap2-eoulsan:$VERSION"

cat > Dockerfile << EOF
############################################################
# Dockerfile to build BWA container images
# Based on Ubuntu
############################################################

# Set the base image to Ubuntu
FROM ubuntu:16.04

# File Author / Maintainer
MAINTAINER Laurent Jourdren <jourdren@biologie.ens.fr>

# Update the repository sources list
RUN apt-get update

# Install compiler and perl stuff
RUN apt-get install --yes build-essential zlib1g-dev git

# Get source code
WORKDIR /tmp
RUN git clone https://github.com/lh3/minimap2.git

# Compile
WORKDIR /tmp/minimap2
RUN git checkout v$VERSION
RUN make
RUN cp -p minimap2 /usr/local/bin

# Cleanup
RUN rm -rf /tmp/minimap2
RUN apt-get clean
RUN apt-get remove --yes --purge build-essential zlib1g-dev git
WORKDIR /root
EOF

#docker build --no-cache -t $DOCKER_IMAGE .
docker build -t $DOCKER_IMAGE .

if [[ $? -eq 0 ]]; then
  docker run --rm -v `pwd`:/root -u `id -u`:`id -g` $DOCKER_IMAGE bash -c 'cp /usr/local/bin/minimap2 /root'
  docker rmi -f $DOCKER_IMAGE
  rm Dockerfile
fi

#!/bin/bash

# This file contains the Docker instruction used to build the BWA static
# executable for Eoulsan

VERSION=0.7.15
DOCKER_IMAGE="bwa-eoulsan:$VERSION"

cat > Dockerfile << EOF
############################################################
# Dockerfile to build BWA container images
# Based on Ubuntu
############################################################

# Set the base image to Ubuntu
FROM ubuntu:12.04

# File Author / Maintainer
MAINTAINER Laurent Jourdren <jourdren@biologie.ens.fr>

# Update the repository sources list
RUN apt-get update

# Install compiler and perl stuff
RUN apt-get install --yes build-essential gcc-multilib apt-utils zlib1g-dev wget

# Get source code
WORKDIR /tmp
RUN wget -q --no-check-certificate https://downloads.sourceforge.net/project/bio-bwa/bwa-$VERSION.tar.bz2
RUN tar xjf bwa-$VERSION.tar.bz2
WORKDIR /tmp/bwa-$VERSION

# Patch Makefile
RUN sed -i 's/CFLAGS=\\t\\t-g -Wall -Wno-unused-function -O2/CFLAGS=-g -Wall -Wno-unused-function -O2 -static/' Makefile

# Compile
RUN make
RUN cp -p bwa /usr/local/bin

# Cleanup
RUN rm -rf /tmp/bwa-$VERSION
RUN apt-get clean
RUN apt-get remove --yes --purge build-essential gcc-multilib apt-utils zlib1g-dev wget
WORKDIR /root
EOF

#docker build --no-cache -t $DOCKER_IMAGE .
docker build -t $DOCKER_IMAGE .

if [[ $? -eq 0 ]]; then
  docker run --rm -v `pwd`:/root -u `id -u`:`id -g` $DOCKER_IMAGE bash -c 'cp /usr/local/bin/bwa /root'
  docker rmi -f $DOCKER_IMAGE
  rm Dockerfile
fi

#!/bin/bash

# This file contains the Docker instruction used to build the Gsnap static
# executable for Eoulsan

VERSION=2014-12-21
DOCKER_IMAGE="gsnap-eoulsan:$VERSION"

cat > Dockerfile << EOF
############################################################
# Dockerfile to build Gsnap container images
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
RUN wget -q http://research-pub.gene.com/gmap/src/gmap-gsnap-$VERSION.tar.gz
RUN tar xzf gmap-gsnap-$VERSION.tar.gz
WORKDIR /tmp/gmap-$VERSION

# Compile
RUN ./configure && make && make install

# Cleanup
RUN rm -rf /tmp/bwa-$VERSION
RUN apt-get clean
RUN apt-get remove --yes --purge build-essential gcc-multilib apt-utils zlib1g-dev wget
WORKDIR /root
EOF

#docker build --no-cache -t $DOCKER_IMAGE .
docker build -t $DOCKER_IMAGE .

if [[ $? -eq 0 ]]; then
  docker run --rm -v `pwd`:/root -u `id -u`:`id -g` $DOCKER_IMAGE bash -c 'cd /usr/local/bin && cp fa_coords  gmap  gmap_build  gmapindex  gmap_process  gsnap /root'
  docker rmi -f $DOCKER_IMAGE
  rm Dockerfile
fi

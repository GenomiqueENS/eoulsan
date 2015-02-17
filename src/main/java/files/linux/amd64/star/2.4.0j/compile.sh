#!/bin/bash

# This file contains the Docker instruction used to build the STAR executable 
# for Eoulsan

VERSION=2.4.0j
DOCKER_IMAGE="star:$VERSION"

cat > Dockerfile << EOF
############################################################
# Dockerfile to build star container images
# Based on Ubuntu
############################################################

# Set the base image to Ubuntu
FROM ubuntu:12.04

# File Author / Maintainer
MAINTAINER Sophie Lemoine <slemoine@biologie.ens.fr>

# Update the repository sources list
RUN apt-get update

# Install compiler and perl stuff
RUN apt-get install --yes build-essential gcc-multilib apt-utils zlib1g-dev vim git

# Install STAR
WORKDIR /tmp
RUN git clone https://github.com/alexdobin/STAR.git
WORKDIR /tmp/STAR/source
RUN git checkout STAR_$VERSION
RUN make clean STARstatic
RUN mv STARstatic /usr/local/bin
RUN make clean STARlong
RUN mv STAR /usr/local/bin/STARlong

# Cleanup
RUN rm -rf /tmp/STAR
RUN apt-get clean
RUN apt-get remove --yes --purge build-essential gcc-multilib apt-utils zlib1g-dev vim git
WORKDIR /root
EOF

docker build --no-cache -t $DOCKER_IMAGE .

if [[ $? -eq 0 ]]; then
  docker run --rm -v `pwd`:/root -u `id -u`:`id -g` $DOCKER_IMAGE cp /usr/local/bin/STARstatic /usr/local/bin/STARlong /root
  docker rmi -f $DOCKER_IMAGE
  rm Dockerfile
fi

#!/bin/bash

# This file contains the Docker instruction used to build the Bowtie 2 static
# executable for Eoulsan

VERSION=2.2.4
DOCKER_IMAGE="bowtie2-eoulsan:$VERSION"

cat > Dockerfile << EOF
############################################################
# Dockerfile to build Bowtie 2 container images
# Based on Ubuntu
############################################################

# Set the base image to Ubuntu
FROM ubuntu:12.04

# File Author / Maintainer
MAINTAINER Laurent Jourdren <jourdren@biologie.ens.fr>

# Update the repository sources list
RUN apt-get update

# Install compiler and perl stuff
RUN apt-get install --yes build-essential gcc-multilib apt-utils zlib1g-dev git

# Install STAR
WORKDIR /tmp
RUN git clone https://github.com/BenLangmead/bowtie2.git
WORKDIR /tmp/bowtie2
RUN git checkout v$VERSION

# Patch Makefile
RUN sed -i 's/ifneq (,\$(findstring 13,\$(shell uname -r)))/ifneq (,\$(findstring Darwin 13,\$(shell uname -sr)))/' Makefile
RUN sed -i 's/RELEASE_FLAGS  = -O3 -m64 \$(SSE_FLAG) -funroll-loops -g3/RELEASE_FLAGS  = -O3 -m64 \$(SSE_FLAG) -funroll-loops -g3 -static/' Makefile

# Compile
RUN make
RUN cp -p bowtie2 bowtie2-* /usr/local/bin

# Cleanup
RUN rm -rf /tmp/bowtie2
RUN apt-get clean
RUN apt-get remove --yes --purge build-essential gcc-multilib apt-utils zlib1g-dev vim git
WORKDIR /root
EOF

docker build --no-cache -t $DOCKER_IMAGE .

if [[ $? -eq 0 ]]; then
  docker run --rm -v `pwd`:/root -u `id -u`:`id -g` $DOCKER_IMAGE bash -c 'cp /usr/local/bin/bowtie2* /root'
  docker rmi -f $DOCKER_IMAGE
  rm Dockerfile
fi

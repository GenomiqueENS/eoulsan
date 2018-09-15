############################################################
# Dockerfile to build Eoulsan container images
# Based on Ubuntu
############################################################

# Set the base image to Ubuntu
FROM genomicpariscentre/deseq:latest

# File Author / Maintainer
MAINTAINER Laurent Jourdren <jourdren@biologie.ens.fr>

# Update the repository sources list
RUN apt-get update

# Install OpenJDK 7 JRE
RUN apt-get install --yes openjdk-7-jre-headless

# Download and install Eoulsan
ADD https://github.com/GenomicParisCentre/eoulsan/releases/download/v2.3/eoulsan-2.3.tar.gz /tmp/

# Install Eoulsan
RUN tar --directory /usr/local -xf /tmp/eoulsan-*.tar.gz

# Create links for eoulsan.sh to get eoulsan.sh in PATH
RUN ln -s /usr/local/eoulsan-*/eoulsan.sh /usr/local/bin/eoulsan.sh
RUN ln -s /usr/local/eoulsan-*/eoulsan.sh /usr/local/bin/eoulsan

# Create data repositories directories
RUN mkdir -p /data/genomes
RUN mkdir -p /data/genomes_descs
RUN mkdir -p /data/mappers_indexes
RUN mkdir -p /data/annotations
RUN mkdir -p /data/additional_annotations
RUN mkdir -p /data/plugins

# Cleanup
RUN rm -rf /tmp/eoulsan-*.tar.gz
RUN apt-get clean

# Default command to execute at startup of the container
CMD eoulsan.sh --version

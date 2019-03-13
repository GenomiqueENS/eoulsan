############################################################
# Dockerfile to build Eoulsan container images
# Based on Ubuntu
############################################################

# Set the base image to Ubuntu
FROM bioconductor/release_sequencing:3.1

# File Author / Maintainer
MAINTAINER Laurent Jourdren <jourdren@biologie.ens.fr>

# Install Eoulsan 
RUN cd /tmp && \
    wget --quiet https://github.com/GenomicParisCentre/eoulsan/releases/download/v2.3/eoulsan-2.3.tar.gz && \
    tar --directory /usr/local -xf /tmp/eoulsan-*.tar.gz && \
    ln -s /usr/local/eoulsan-*/eoulsan.sh /usr/local/bin/eoulsan.sh && \
    ln -s /usr/local/eoulsan-*/eoulsan.sh /usr/local/bin/eoulsan && \
    mkdir -p /data/genomes && \
    mkdir -p /data/genomes_descs && \
    mkdir -p /data/mappers_indexes && \
    mkdir -p /data/annotations && \
    mkdir -p /data/additional_annotations && \
    mkdir -p /data/plugins && \
    rm -rf /tmp/eoulsan-*.tar.gz && \
    apt-get clean

# Default command to execute at startup of the container
CMD eoulsan.sh --version

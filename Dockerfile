############################################################
# Dockerfile to build Eoulsan container images
# Based on Ubuntu
############################################################

# Set the base image to easycontrasts
FROM genomicpariscentre/easycontrasts:2.0

ARG BUILD_PACKAGES="wget"
ARG DEBIAN_FRONTEND=noninteractive

# Install Eoulsan 
RUN cd /tmp && \
    apt update && \
    apt install --yes $BUILD_PACKAGES locales openjdk-17-jre-headless && \
    locale-gen en_US.UTF-8 && \
    update-locale && \
    wget --quiet https://github.com/GenomiqueENS/eoulsan/releases/download/v2.7.1/eoulsan-2.7.1.tar.gz && \
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
    apt remove --purge --yes $BUILD_PACKAGES && \
    apt autoremove --purge --yes && \
    apt clean && \
    rm -rf /var/lib/apt/lists/*

# Default command to execute at startup of the container
CMD ["eoulsan.sh", "-version"]

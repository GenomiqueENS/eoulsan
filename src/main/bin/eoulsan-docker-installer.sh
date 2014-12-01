#!/bin/bash

#
# This script allow to create the eoulsan-docker.sh script with the executable rights.
# To install eoulsan-docker.sh, use the following command
# $ curl @@@WEBSITE@@@/eoulsan-docker-installer.sh | bash 
#

# Create the script
cat > eoulsan-docker.sh <<'EOF'
#!/bin/bash

#
# This script allow to launch Eoulsan with Docker with the right 
# environment (mount ressources directories in the container and
# set the envionment variables)
#

EOULSAN_VERSION=@@@VERSION@@@
DOCKER_SOCKET_FILE=/var/run/docker.sock

ENV_ARGS=
VOLUMES_ARGS=
EOULSAN_SETTINGS_ARGS=

# Set Eoulsan environment variables for the container
if [ -v EOULSAN_MEMORY ]; then
  ENV_ARGS="$ENV_ARGV -E EOULSAN_MEMORY=$EOULSAN_MEMORY"
fi
if [ -v EOULSAN_JVM_OPTS ]; then
  ENV_ARGS="$ENV_ARGV -E EOULSAN_JVM_OPTS=$EOULSAN_JVM_OPTS"
fi
if [ -v EOULSAN_PLUGINS ]; then
  VOLUMES_ARGS="$VOLUMES_ARGS -v $EOULSAN_PLUGINS=/data/plugins"
fi

# Handle .eoulsan configuration file
# Mount if necessary the ressource directories in the container
if [ -f $HOME/.eoulsan ]; then

  VOLUMES_ARGS="$VOLUMES_ARGS -v $HOME/.eoulsan:/.eoulsan"

  GENOME_STORAGE_PATH=`grep -e '^main\.genome\.storage\.path=' $HOME/.eoulsan  | tail -n 1 | cut -f 2 -d '='`
  GENOME_MAPPER_INDEX_STORAGE_PATH=`grep -e '^main\.genome\.mapper\.index\.storage\.path=' $HOME/.eoulsan  | tail -n 1 | cut -f 2 -d '='`
  GENOME_DESC_STORAGE_PATH=`grep -e '^main\.genome\.desc\.storage\.path=' $HOME/.eoulsan  | tail -n 1 | cut -f 2 -d '='`
  ANNOTATION_STORAGE_PATH=`grep -e '^main\.annotation\.storage\.path=' $HOME/.eoulsan  | tail -n 1 | cut -f 2 -d '='`
  ADDITIONAL_ANNOTATION_STORAGE_PATH=`grep -e '^main\.additional\.annotation\.storage\.path=' $HOME/.eoulsan  | tail -n 1 | cut -f 2 -d '='`

  if [ ! -z "$GENOME_STORAGE_PATH" ]; then
    VOLUMES_ARGS="$VOLUMES_ARGS -v $GENOME_STORAGE_PATH:/data/genomes"
    EOULSAN_SETTINGS_ARGS="$EOULSAN_SETTINGS_ARGS -s main.genome.storage.path=/data/genomes"
  fi

  if [ ! -z "$GENOME_DESC_STORAGE_PATH" ]; then
    VOLUMES_ARGS="$VOLUMES_ARGS -v $GENOME_DESC_STORAGE_PATH:/data/genomes"
    EOULSAN_SETTINGS_ARGS="$EOULSAN_SETTINGS_ARGS -s main.genome.desc.storage.path=/data/genomes_descs"
  fi

  if [ ! -z "$GENOME_MAPPER_INDEX_STORAGE_PATH" ]; then
    VOLUMES_ARGS="$VOLUMES_ARGS -v $GENOME_MAPPER_INDEX_STORAGE_PATH:/data/mappers_indexes"
    EOULSAN_SETTINGS_ARGS="$EOULSAN_SETTINGS_ARGS -s main.genome.mapper.index.storage.path=/data/mappers_indexes"
  fi

  if [ ! -z "$ANNOTATION_STORAGE_PATH" ]; then
    VOLUMES_ARGS="$VOLUMES_ARGS -v $ANNOTATION_STORAGE_PATH:/data/annotations"
    EOULSAN_SETTINGS_ARGS="$EOULSAN_SETTINGS_ARGS -s main.annotation.storage.path=/data/annotations"
  fi

  if [ ! -z "$ADDITIONAL_ANNOTATION_STORAGE_PATH" ]; then
    VOLUMES_ARGS="$VOLUMES_ARGS -v $ADDITIONAL_ANNOTATION_STORAGE_PATH:/data/additional_annotations"
    EOULSAN_SETTINGS_ARGS="$EOULSAN_SETTINGS_ARGS -s main.additional.annotation.storage.path=additional_annotations"
  fi

fi

# Launch Eoulsan
docker run -ti --rm \
           -u `id -u`:`id -g` \
           $ENV_ARGV \
           -v $DOCKER_SOCKET_FILE:/var/run/docker.sock \
           -v $HOME/.eoulsan:/.eoulsan \
           -v $PWD:/root \
           $VOLUMES_ARGS \
           -w /root \
           genomicpariscentre/eoulsan:$EOULSAN_VERSION \
           eoulsan.sh \
           $EOULSAN_SETTINGS_ARGS \
           "$@"
EOF

# Set executable eoulsan-docker.sh
chmod +x eoulsan-docker.sh

# Success message
echo " * eoulsan-docker.sh script has been created."

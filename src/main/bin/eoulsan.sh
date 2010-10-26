#!/bin/sh

#
# This script set the right classpath and start the application
#
# Author : Laurent Jourdren
#


# Get the path to this script
BASEDIR=`dirname $0`

# Set the Eoulsan libraries path
LIBDIR=$BASEDIR/lib

# Set the memory in MiB needed by Eoulsan (only Java part, not external tools)
# By Default 2048
MEMORY=2048

# Add here your plugins and dependencies
PLUGINS=

# Set the path to java
JAVA_CMD=java

LIBCP=
for lib in `ls $LIBDIR`
do
    LIBCP=$LIBCP:$LIBDIR/$lib
done


# Launch Eoulsan
$JAVA_CMD -server \
          -Xmx${MEMORY}m \
          -cp $LIBCP:$PLUGINS \
          fr.ens.transcriptome.eoulsan.Main $*

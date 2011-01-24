#!/bin/sh

#
# This script set the right classpath and start the application
#
# Author : Laurent Jourdren
#

# Function to create lib paths
make_paths() {

	local RESULT=
	for lib in `ls $1`
	do
		if [ -f $1/$lib ]; then
			RESULT=$RESULT:$1/$lib
		fi
	done

	echo $RESULT
}

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

COMMON_LIBS=$(make_paths $LIBDIR)
LOCAL_LIBS=$(make_paths $LIBDIR/local)
PLUGINS_LIBS=$(make_paths $EOULSAN_PLUGINS)

# Launch Eoulsan
$JAVA_CMD \
		-server \
		-Xmx${MEMORY}m \
		-cp $COMMON_LIBS:$LOCAL_LIBS:$PLUGINS:$PLUGINS_LIBS \
		-Deoulsan.hadoop.libs=$COMMON_LIBS:$PLUGINS:$PLUGINS_LIB \
		-Deoulsan.launch.mode=local \
		fr.ens.transcriptome.eoulsan.Main "$@"

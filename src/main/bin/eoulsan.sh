#!/usr/bin/env bash

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

# Additional JVM options
JVM_OPTS="-server"

# Add here your plugins and dependencies
PLUGINS=

# Parse options
OPTERR=0
while getopts “j:m:J:p:” OPTION
do
	case $OPTION in
		j)
			JAVA_HOME=$OPTARG
		;;
		m)
			MEMORY=$OPTARG
		;;
		J)
			JVM_OPTS=$OPTARG
		;;
		p)
			PLUGINS=$OPTARG
		;;
	esac
done

# Set the path to java
if [ -z "$JAVA_HOME" ] ; then
	JAVA_CMD="java"
else
	JAVA_CMD="$JAVA_HOME/bin/java"
fi

COMMON_LIBS=$(make_paths $LIBDIR)
LOCAL_LIBS=$(make_paths $LIBDIR/local)
PLUGINS_LIBS=$(make_paths $EOULSAN_PLUGINS)

# Launch Eoulsan
$JAVA_CMD \
		$JVM_OPTS \
		-Xmx${MEMORY}m \
		-cp $COMMON_LIBS:$LOCAL_LIBS:$PLUGINS:$PLUGINS_LIBS \
		-Deoulsan.hadoop.libs=$COMMON_LIBS:$PLUGINS:$PLUGINS_LIB \
		-Deoulsan.launch.mode=local \
		-Deoulsan.launch.script.path=$0 \
		fr.ens.transcriptome.eoulsan.Main "$@"

#!/bin/bash

RLUTILS="/opt/rlutils"
PROJECT="TestDeviceApp"

mkdir -p ${RLUTILS}/{bin,config/${PROJECT,,},docs,jars,logs}
if [ ! -f "dist/${PROJECT}.jar" ]; then
    echo "dist/${PROJECT}.jar does not exist"
    exit 1
fi

cp -a dist/${PROJECT}.jar ${RLUTILS}/jars/
cp -a src/resources/* ${RLUTILS}/config/${PROJECT,,}

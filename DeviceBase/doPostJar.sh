#!/bin/bash

RLUTILS="/opt/rlutils"
PROJECT="DeviceBase"

mkdir -p ${RLUTILS}/{bin,config,docs,jars,logs}
if [ ! -f "dist/${PROJECT}.jar" ]; then
    echo "dist/${PROJECT}.jar does not exist"
    exit 1
fi

cp -a dist/${PROJECT}.jar ${RLUTILS}/jars/

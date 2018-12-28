#!/bin/bash

RLUTILS="/opt/rlutils"
PROJECT="MessagingService"

mkdir -p ${RLUTILS}/{bin,config,docs,jars,logs}
if [ ! -f "dist/${PROJECT}.jar" ]; then
    echo "dist/${PROJECT}.jar does not exist"
    exit 1
fi

${RLUTILS}/bin/JarFixer
cp -a dist/${PROJECT}.jar ${RLUTILS}/jars/
cp -a dist/${PROJECT} ${RLUTILS}/bin/

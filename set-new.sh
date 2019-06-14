#!/bin/sh

AWS_PROFILE=pci dev env set --env production --keyval APIBUILDER_SERVICE_URIS=https://s3.amazonaws.com/io.flow.aws-s3-public/util/lib-apibuilder/specs.zip
AWS_PROFILE=pci dev env list --env production | grep APIB

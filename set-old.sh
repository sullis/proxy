#!/bin/sh

AWS_PROFILE=pci dev env set --env production --keyval APIBUILDER_SERVICE_URIS=https://s3.amazonaws.com/io.flow.aws-s3-public/util/api-proxy/latest/api.service.json:https://s3.amazonaws.com/io.flow.aws-s3-public/util/api-internal-proxy/latest/api-internal.service.json:https://s3.amazonaws.com/io.flow.aws-s3-public/util/api-partner-proxy/latest/api-partner.service.json:https://s3.amazonaws.com/io.flow.aws-s3-public/util/apibuilder/flow/shopify-external.latest.json

AWS_PROFILE=pci dev env set --env development --keyval APIBUILDER_SERVICE_URIS=https://s3.amazonaws.com/io.flow.aws-s3-public/util/api-proxy/latest/api.service.json:https://s3.amazonaws.com/io.flow.aws-s3-public/util/api-internal-proxy/latest/api-internal.service.json:https://s3.amazonaws.com/io.flow.aws-s3-public/util/api-partner-proxy/latest/api-partner.service.json:https://s3.amazonaws.com/io.flow.aws-s3-public/util/apibuilder/flow/shopify-external.latest.json

AWS_PROFILE=pci dev env set --env production --keyval PROXY_CONFIG_URIS=https://s3.amazonaws.com/io.flow.aws-s3-public/util/api-proxy/latest/production.config.yml:https://s3.amazonaws.com/io.flow.aws-s3-public/util/api-internal-proxy/latest/production.config.yml:https://s3.amazonaws.com/io.flow.aws-s3-public/util/api-partner-proxy/latest/production.config.yml

AWS_PROFILE=pci dev env set --env development --keyval PROXY_CONFIG_URIS=https://s3.amazonaws.com/io.flow.aws-s3-public/util/api-proxy/latest/production.config.yml:https://s3.amazonaws.com/io.flow.aws-s3-public/util/api-internal-proxy/latest/production.config.yml:https://s3.amazonaws.com/io.flow.aws-s3-public/util/api-partner-proxy/latest/production.config.yml

AWS_PROFILE=pci dev env list --env production | grep APIB


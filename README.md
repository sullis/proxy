[![Build Status](https://travis-ci.com/flowvault/proxy.svg?token=8bzVqzHy6JVEQr9mN9hx&branch=master)](https://travis-ci.com/flowvault/proxy)

# Proxy

API Proxy server that is hosted at https://api.flow.io

## Main features:

  - Resolves an incoming path to a backend application (e.g. /users =>  'user' application)
  - Handles private vs. public paths
  - Authorizes the API token if present via the Authorization header (basic auth)
  - If the path contains an :organization prefix, verifies that the user identified
    by the API token has access to the organization (i.e. a membership record exists
    between the user and organization). This also verifies that the organization
    prefix is valid.

## Future features:

  - Implement expansion by detecting 'expand' query parameters

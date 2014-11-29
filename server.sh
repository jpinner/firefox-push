#!/bin/bash

args="$@"

mvn compile &&
mvn exec:exec -Dport="$args"

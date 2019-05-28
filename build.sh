#!/bin/bash
make
([ $? -eq 0 ] && echo -e "$\033[0;32m ETB2 built successfully$\033[0;30m") || echo -e "$\033[0;31m ETB2 has failed to build$\033[0;30m"

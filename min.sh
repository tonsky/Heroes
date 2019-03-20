#!/usr/bin/env bash -ex

clj -A:fig -m figwheel.main -O advanced --build-once min
open docs/index.html
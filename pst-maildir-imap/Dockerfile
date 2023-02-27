FROM ubuntu:22.04

RUN apt-get update --yes && apt-get install --yes --no-install-recommends \
    pst-utils \
    python3

RUN useradd user
USER user

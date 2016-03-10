FROM fedora:22

RUN dnf -y install cmake cyrus-sasl-devel gcc libuuid-devel make openssl-devel python-devel swig

ENV PROTON_VERSION 0.12.0
ADD qpid-proton-$PROTON_VERSION.tar.gz /
RUN mkdir /qpid-proton-$PROTON_VERSION/build
WORKDIR /qpid-proton-$PROTON_VERSION/build

RUN cmake .. -DSYSINSTALL_BINDINGS=ON -DCMAKE_INSTALL_PREFIX=/usr -DSYSINSTALL_PYTHON=ON && make install

CMD ["bash"]

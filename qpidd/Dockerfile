FROM gordons/qpid-proton:0.12.0

RUN dnf -y install boost-devel gcc-c++ pkgconfig ruby

ENV QPID_CPP_VERSION master
ADD qpid-cpp-$QPID_CPP_VERSION.tar.gz /
RUN mkdir /qpid-cpp-$QPID_CPP_VERSION/build
WORKDIR /qpid-cpp-$QPID_CPP_VERSION/build
RUN cmake .. -DCMAKE_INSTALL_PREFIX=/usr && make install

COPY ./run_qpidd.sh ./get_data_dir.py /qpid-cpp-$QPID_CPP_VERSION/
EXPOSE 5672
CMD ["/qpid-cpp-$QPID_CPP_VERSION/run_qpidd.sh"]

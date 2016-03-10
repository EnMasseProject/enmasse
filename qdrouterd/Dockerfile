FROM gordons/qpid-proton:0.12.0
RUN dnf -y install gettext

ENV DISPATCH_VERSION master
ADD qpid-dispatch-$DISPATCH_VERSION.tar.gz /
RUN mkdir /qpid-dispatch-$DISPATCH_VERSION/build
WORKDIR /qpid-dispatch-$DISPATCH_VERSION/build
RUN cmake .. -DCMAKE_INSTALL_PREFIX=/usr && make install

EXPOSE 5672 55672
COPY ./run_qdr.sh ./qdrouterd.conf.template /etc/qpid-dispatch/
CMD ["/etc/qpid-dispatch/run_qdr.sh"]

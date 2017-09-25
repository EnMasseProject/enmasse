The certs for the tests were generated as follows:

First we create the private key and self-signed certificate for the CA:

  openssl genrsa -out ca-key.pem 2048

  openssl req -new -sha256 -key ca-key.pem -out ca-csr.pem

  openssl x509 -req -in ca-csr.pem -signkey ca-key.pem -out ca-cert.pem

Then we create private keys and certificates, signed by the CA this
time, for the client:

  openssl genrsa -out client-key.pem 2048

  openssl req -new -sha256 -key client-key.pem -out client-csr.pem

  openssl x509 -req -in client-csr.pem -CA ca-cert.pem -CAkey ca-key.pem -out client-cert.pem -CAcreateserial

 and the server:

  openssl genrsa -out server-key.pem 2048

  openssl req -new -sha256 -key server-key.pem -out server-csr.pem

  openssl x509 -req -in server-csr.pem -CA ca-cert.pem -CAkey ca-key.pem -out server-cert.pem -CAcreateserial

The certificate signing requests and the ca key are not used beyond the generation of the other artefacts.
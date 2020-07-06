/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package accesscontrolserver

import (
	"fmt"
	"github.com/enmasseproject/enmasse/pkg/accesscontrolserver/amqp"
	"log"
	"net"
	"sync"
)

type Server struct {
	listener net.Listener
	address  string
	quit     chan interface{}
	wg       *sync.WaitGroup
}

func NewServer(hostname string, port uint16) (*Server, error) {
	s := &Server{
		quit: make(chan interface{}),
		wg:   &sync.WaitGroup{},
	}

	s.address = fmt.Sprintf("%s:%d", hostname, port)
	listener, err := net.Listen("tcp", s.address)
	if err != nil {
		return nil, err
	}

	s.listener = listener
	s.wg.Add(1)
	go s.serve()
	return s, nil
}

func (s *Server) GetAddress() string {
	return s.listener.Addr().String()
}

func (s *Server) Stop() {
	close(s.quit)
	_ = s.listener.Close()
	s.wg.Wait()
}

func (s Server) serve() {
	defer s.wg.Done()

	for {
		conn, err := s.listener.Accept()
		if err != nil {
			select {
			case <-s.quit:
				return
			default:
				log.Fatalf("accept error : %v", err)
			}
		} else {
			s.wg.Add(1)
			go func() {
				s.handleConnection(conn)
				s.wg.Done()
			}()
		}
	}
}

func (s Server) handleConnection(conn net.Conn) {
	defer conn.Close()
	ic, error := amqp.NewIncoming(conn, amqp.WithSASLAnonymous())
	if error != nil {
		log.Printf("Failed to admit incoming connection: %v", error)
		return
	}

	// TODO calculate authorisation
	error = ic.Accept()
	if error != nil {
		log.Printf("Failed to accept incoming connection: %v", error)
	}

}

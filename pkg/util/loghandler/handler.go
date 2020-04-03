/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package loghandler

import (
	"github.com/go-logr/logr"
	"k8s.io/apimachinery/pkg/api/meta"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/client-go/util/workqueue"
	"sigs.k8s.io/controller-runtime/pkg/event"
	"sigs.k8s.io/controller-runtime/pkg/handler"
	"sigs.k8s.io/controller-runtime/pkg/runtime/inject"
)

// ensure we implement the necessary interfaces

var _ handler.EventHandler = &LogHandler{}
var _ inject.Scheme = &LogHandler{}
var _ inject.Mapper = &LogHandler{}

// implementation of handler.EventHandler

type LogHandler struct {
	handler handler.EventHandler
	logger  logr.InfoLogger
	name    string

	injectScheme inject.Scheme
	injectMapper inject.Mapper
}

func New(handler handler.EventHandler, logger logr.InfoLogger, name string) *LogHandler {

	injectScheme, _ := handler.(inject.Scheme)
	injectMapper, _ := handler.(inject.Mapper)

	return &LogHandler{
		handler: handler,
		logger:  logger,
		name:    name,

		injectScheme: injectScheme,
		injectMapper: injectMapper,
	}

}

func (l *LogHandler) Create(event event.CreateEvent, queue workqueue.RateLimitingInterface) {
	l.logger.Info("Created", "event", event, "handler", l.name)
	l.handler.Create(event, queue)
}

func (l *LogHandler) Update(event event.UpdateEvent, queue workqueue.RateLimitingInterface) {
	l.logger.Info("Update", "event", event, "handler", l.name)
	l.handler.Update(event, queue)
}

func (l *LogHandler) Delete(event event.DeleteEvent, queue workqueue.RateLimitingInterface) {
	l.logger.Info("Delete", "event", event, "handler", l.name)
	l.handler.Delete(event, queue)
}

func (l *LogHandler) Generic(event event.GenericEvent, queue workqueue.RateLimitingInterface) {
	l.logger.Info("Generic", "event", event, "handler", l.name)
	l.handler.Generic(event, queue)
}

// implementation of inject.Scheme

func (l *LogHandler) InjectScheme(scheme *runtime.Scheme) error {
	if l.injectScheme != nil {
		return l.injectScheme.InjectScheme(scheme)
	} else {
		return nil
	}
}

// implementation of inject.Mapper

func (l *LogHandler) InjectMapper(mapper meta.RESTMapper) error {
	if l.injectMapper != nil {
		return l.injectMapper.InjectMapper(mapper)
	} else {
		return nil
	}
}

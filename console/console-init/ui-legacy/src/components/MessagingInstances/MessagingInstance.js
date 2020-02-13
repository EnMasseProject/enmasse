

class MessagingInstance {
  constructor(name, namespace, component, type, timeCreated, isReady, phase, url) {
    this.name = name;
    this.namespace = namespace;
    this.component = 'AS';
    this.type = type;
    this.timeCreated = timeCreated;
    this.consoleUrl = url;
    this.isReady = isReady;
    this.phase = phase;
  }

}

export default MessagingInstance;

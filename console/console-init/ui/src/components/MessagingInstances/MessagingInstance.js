

class MessagingInstance {
  constructor(name, namespace, component, type, timeCreated, isReady, url) {
    this.name = name;
    this.namespace = namespace;
    this.component = 'AS';
    this.type = type;
    this.timeCreated = timeCreated;
    this.consoleUrl = url;
    this.isReady = isReady;
  }

}

export default MessagingInstance;

var rhea = require('rhea');

function TimeSeries(label, max_size) {
    this.yData = [label];
    this.xData = ['time'];
    this.max_size = max_size | 10;
    this.last = undefined;
}

TimeSeries.prototype.push = function (value) {
    if (this.xData.length > this.max_size) {
        this.xData.splice(1, 1);
        this.yData.splice(1, 1);
    }
    this.yData.push(value);
    this.xData.push(new Date().getTime());
}

TimeSeries.prototype.push_delta = function (value) {
    if (this.last === undefined) {
        this.push(value);
    } else {
        this.push(value - this.last);
    }
    this.last = value;
}

function AddressDefinition(a) {
    this.update(a);
    /*
    this.messages_in_series = new TimeSeries('messages-in');
    this.messages_out_series = new TimeSeries('messages-out');
    this.message_in_series_config = {
        chartId: 'ingress-' + this.address,
        tooltipType: 'default'
    };
    this.message_out_series_config = {
        chartId: 'egress-' + this.address,
        tooltipType: 'default'
    };
    */
    this.depth_series = new TimeSeries('messages-stored');
    this.depth_series_config = {
        chartId      : 'depth-' + this.address,
        tooltipType  : 'actual',
        title        : 'Messages Stored',
        layout       : 'compact',
        trendLabel   : 'Messages Stored',
        valueType    : 'actual',
        timeFrame    : 'Last 5 Minutes',
        units        : ''
    };
    this.update_depth_series();
    this.periodic_deltas = ['messages_in', 'messages_out'];
    for (var i = 0; i < this.periodic_deltas.length; i++) {
        var name = this.periodic_deltas[i];
        this['_last_' + name] = 0;
        Object.defineProperty(this, name + '_delta', { get: function () { return this[name] - this['_last_' + name]; } });
    }
}

AddressDefinition.prototype.update = function (a) {
    for (var k in a) {
        this[k] = a[k];
    }
    if (this.store_and_forward) {
        if (this.multicast) {
            this.pattern = "topic";
        } else {
            this.pattern = "queue";
        }
    } else {
        if (this.multicast) {
            this.pattern = "multicast";
        } else {
            this.pattern = "anycast";
        }
    }
}

AddressDefinition.prototype.update_depth_series = function () {
    if (this.store_and_forward && this.depth !== undefined) {
        this.depth_series.push(this.depth);
        return true;
    } else {
        return false;
    }
}

AddressDefinition.prototype.reset_periodic_deltas = function () {
    var changed = false;
    for (var i = 0; i < this.periodic_deltas.length; i++) {
        var name = this.periodic_deltas[i];
        if (this['_last_' + name] !== this[name]) {
            this['_last_' + name] = this[name];
            changed = true;
        }
    }
    return changed;
}

function AddressService() {
    this.addresses = [];
    this.flavors = [];
    this.connections = [];
    this.users = [];
    var ws = rhea.websocket_connect(WebSocket);
    this.connection = rhea.connect({"connection_details":ws("ws://" + location.hostname + ":56720", ["binary", "AMQPWSB10"]), "reconnect":true});
    this.connection.on('message', this.on_message.bind(this));
    this.sender = this.connection.open_sender();
    this.connection.open_receiver();
    setInterval(this.reset_periodic_deltas.bind(this), 5*60000);
    setInterval(this.update_depth_series.bind(this), 30000);
}

AddressService.prototype.update_depth_series = function () {
    var changed = false;
    for (var i = 0; i < this.addresses.length; i++) {
        if (this.addresses[i].update_depth_series()) {
            changed = true;
        }
    }
    if (changed && this.callback) this.callback();
};

AddressService.prototype.reset_periodic_deltas = function () {
    var changed = false;
    for (var i = 0; i < this.addresses.length; i++) {
        if (this.addresses[i].reset_periodic_deltas()) {
            changed = true;
        }
    }
    if (changed && this.callback) this.callback();
};

AddressService.prototype.update = function (a) {
    var i = 0;
    while (i < this.addresses.length && a.address !== this.addresses[i].address) {
        i++;
    }
    if (this.addresses[i] === undefined) {
        this.addresses[i] = new AddressDefinition(a);
    } else {
        this.addresses[i].update(a);
    }
}

AddressService.prototype.create_address = function (obj) {
    var added = { address: obj.address };
    if (obj.pattern === 'queue') {
        added.multicast = false;
        added.store_and_forward = true;
    } else if (obj.pattern === 'topic') {
        added.multicast = true;
        added.store_and_forward = true;
    } else if (obj.pattern === 'anycast') {
        added.multicast = false;
        added.store_and_forward = false;
    } else if (obj.pattern === 'multicast') {
        added.multicast = true;
        added.store_and_forward = false;
    }
    if (obj.flavor) added.flavor = obj.flavor;
    if (obj.transactional) added.group_id = 'transactional';
    else if (obj.pooled) added.group_id = 'pooled';
    this.sender.send({subject: 'create_address', body: added});
}

AddressService.prototype.delete_selected = function () {
    for (var i = 0; i < this.addresses.length;) {
        if (this.addresses[i].selected) {
            this.sender.send({subject: 'delete_address', body: this.addresses[i]});
            this.addresses.splice(i, 1);
        } else {
            i++;
        }
    }
}

AddressService.prototype.is_unique_name = function (name) {
    return !this.addresses.some(function (a) { return a.address === name; });
}

AddressService.prototype.create_user = function (obj) {
    console.log('creating user: ' + JSON.stringify(obj));
    this.sender.send({subject: 'create_user', body: obj});
}

AddressService.prototype.delete_selected_users = function () {
    for (var i = 0; i < this.users.length;) {
        if (this.users[i].selected) {
            this.sender.send({subject: 'delete_user', body: this.users[i].name});
            this.users.splice(i, 1);
        } else {
            i++;
        }
    }
}

AddressService.prototype.update_connection = function (c) {
    var i = 0;
    while (i < this.connections.length && c.id !== this.connections[i].id) {
        i++;
    }
    this.connections[i] = c;
}

AddressService.prototype.update_user = function (c) {
    var i = 0;
    while (i < this.users.length && c.name !== this.users[i].name) {
        i++;
    }
    this.users[i] = c;
}

AddressService.prototype.on_message = function (context) {
    if (context.message.subject === 'address') {
        this.update(context.message.body);
        if (this.callback) this.callback();
    } else if (context.message.subject === 'address_deleted') {
        var changed = false;
        for (var i = 0; i < this.addresses.length;) {
            if (this.addresses[i].address === context.message.body) {
                this.addresses.splice(i, 1);
                changed = true;
            } else {
                i++;
            }
        }
        if (changed && this.callback) this.callback();
    } else if (context.message.subject === 'flavors') {
        this.flavors = context.message.body;
        if (this.callback) this.callback();
    } else if (context.message.subject === 'connection') {
        this.update_connection(context.message.body);
        if (this.callback) this.callback();
    } else if (context.message.subject === 'connection_deleted') {
        var changed = false;
        for (var i = 0; i < this.connections.length;) {
            if (this.connections[i].id === context.message.body) {
                this.connections.splice(i, 1);
                changed = true;
            } else {
                i++;
            }
        }
        if (changed && this.callback) this.callback();
    } else if (context.message.subject === 'user') {
        console.log('got user: ' + JSON.stringify(context.message.body));
        this.update_user(context.message.body);
        if (this.callback) this.callback();
    } else if (context.message.subject === 'user_deleted') {
        var changed = false;
        for (var i = 0; i < this.users.length;) {
            if (this.users[i].id === context.message.body) {
                this.users.splice(i, 1);
                changed = true;
            } else {
                i++;
            }
        }
        if (changed && this.callback) this.callback();
    }
}

AddressService.prototype.on_update = function (callback) {
    this.callback = callback;
}

angular.module('address_service', []).factory('address_service', function() {
    return new AddressService();
});


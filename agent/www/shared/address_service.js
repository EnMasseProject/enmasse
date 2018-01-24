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

function WindowedDelta(window) {
    this.last = undefined;
    this.deltas = new Array(window);
    this.current = 0;
}

WindowedDelta.prototype.push = function (value) {
    this.deltas[this.current++] = value;
    if (this.current >= this.deltas.length) {
        this.current = this.current % this.deltas.length;
    }
};

WindowedDelta.prototype.update = function (value) {
    if (this.last === undefined || this.last > value) {
        this.push(value);
    } else {
        this.push(value - this.last);
    }
    this.last = value;
};

WindowedDelta.prototype.total = function (current) {
    var t = this.last === undefined ? current : current - this.last;
    for (var i = 0; i < this.deltas.length; i++) {
        if (this.deltas[i] != undefined) {
            t += this.deltas[i];
        }
    }
    return t;
};

function AddressDefinition(a) {
    this.update(a);
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
    this.periodic_deltas = {
        'messages_in': new WindowedDelta(10),
        'messages_out': new WindowedDelta(10)
    };
    for (var name in this.periodic_deltas) {
        this.define_periodic_delta(name);
    }
}

AddressDefinition.prototype.define_periodic_delta = function (name) {
    Object.defineProperty(this, name + '_delta', { get: function () { return this.periodic_deltas[name].total(this[name]); } });
}

AddressDefinition.prototype.update = function (a) {
    for (var k in a) {
        this[k] = a[k];
    }
}

AddressDefinition.prototype.update_depth_series = function () {
    if ((this.type === 'queue' || this.type === 'topic') && this.depth !== undefined) {
        this.depth_series.push(this.depth);
        return true;
    } else {
        return false;
    }
}

AddressDefinition.prototype.update_periodic_deltas = function () {
    for (var name in this.periodic_deltas) {
        this.periodic_deltas[name].update(this[name]);
    }
    return true;
}

function AddressService($http) {
    var self = this;  // 'this' is not available in the success funtion of $http.get
    this.admin_disabled = true;
    this.addresses = [];
    this.address_types = [];
    this.address_space_type = '';
    this.connections = [];
    this.users = [];
    var ws = rhea.websocket_connect(WebSocket);
    this.connection = rhea.connect({"connection_details":ws("wss://" + location.hostname + ":" + location.port + "/websocket", ["binary", "AMQPWSB10"]), "reconnect":true, rejectUnauthorized:true});
    this.connection.on('message', this.on_message.bind(this));
    this.sender = this.connection.open_sender();
    this.connection.open_receiver();
    setInterval(this.update_periodic_deltas.bind(this), 30000);
    setInterval(this.update_depth_series.bind(this), 30000);

    this.tooltip = {}
    $http.get('tooltips.json')
      .then(function (d) {
        self.tooltip = d.data;
      })
}

function by_name(name) {
    return function (o) {
        return o.name === name;
    }
}

AddressService.prototype.get_plan_display_name = function (type, plan) {
    var t = this.address_types.filter(by_name(type))
    if (t.length) {
        var p = t[0].plans.filter(by_name(plan));
        if (p.length) {
            return p[0].displayName;
        } else {
            console.log('found no plan called %s address of type %s', plan, type);
        }
    } else {
        console.log('found no address for type %s', type);
    }
    return plan;
};

AddressService.prototype.get_valid_plans = function (type) {
    var l = this.address_types.filter(function (f) { return f.name === type; })
    return l.length ? l[0].plans : [];
};

AddressService.prototype.get_valid_address_types = function () {
    return this.address_types;
};

AddressService.prototype.update_depth_series = function () {
    var changed = false;
    for (var i = 0; i < this.addresses.length; i++) {
        if (this.addresses[i].update_depth_series()) {
            changed = true;
        }
    }
    if (changed && this.callback) this.callback('update_depth_series');
};

AddressService.prototype.update_periodic_deltas = function () {
    for (var i = 0; i < this.addresses.length; i++) {
        this.addresses[i].update_periodic_deltas();
    }
    if (this.callback) this.callback('reset_periodic_deltas');
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
    this.sender.send({subject: 'create_address', body: obj});
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
    if (i >= this.connections.length)
      this.connections[i] = c
    else
    // don't replace existing connection items, just update them
      Object.assign(this.connections[i], c)
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
        if (this.callback) this.callback('address');
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
        if (changed && this.callback) this.callback('address:deleted');
    } else if (context.message.subject === 'address_types') {
        this.address_types = context.message.body;
        this.address_space_type = context.message.application_properties.address_space_type;
        this.admin_disabled = context.message.application_properties.disable_admin;
        if (this.callback) this.callback('address_types');
    } else if (context.message.subject === 'connection') {
        this.update_connection(context.message.body);
        if (this.callback) this.callback('connection');
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
        if (changed && this.callback) this.callback("connection:deleted");
    } else if (context.message.subject === 'user') {
        console.log('got user: ' + JSON.stringify(context.message.body));
        this.update_user(context.message.body);
        if (this.callback) this.callback("user");
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
        if (changed && this.callback) this.callback("user:deleted");
    }
}

AddressService.prototype.on_update = function (callback) {
    this.callback = callback;
}

angular.module('address_service', []).factory('address_service', function($http) {
    return new AddressService($http);
});


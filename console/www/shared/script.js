
function extended_tooltip (colour, rows) {
    var html = '<table class="c3-tooltip">';
    for (var i = 0; i < rows.length; i++) {
        if (i === 0) {
            html += '<tr><td><span style="background-color:' + colour + '"></span><strong>'
                + rows[i][0] + '</strong></td><td><strong>' + rows[i][1] + '</strong></td></tr>';
        } else {
            html += '<tr><td>' + rows[i][0] + '</td><td>' + rows[i][1] + '</td></tr>';
        }
    }
    html += '</table>';
    return html;
}

function get_tooltip_for_shard (address) {
    return function (datum, defaultTitleFormat, defaultValueFormat, colour) {
        var shard = address.shards[datum[0].index];
        var rows = [[shard.name, shard.messages]];
        if (!address.multicast) {
            return extended_tooltip(colour(datum[0]), rows.concat(['enqueued', 'acknowledged', 'expired', 'killed'].map(function (key) { return [key, shard[key]]; })));
        } else {
            return extended_tooltip(colour(datum[0]), rows);
        }
    };
}

function create_donut_chart(title, tooltip, size) {
    var chart = patternfly.c3ChartDefaults().getDefaultDonutConfig(title);
    chart.tooltip = {show: true};
    chart.tooltip.contents = tooltip || $().pfDonutTooltipContents;
    chart.size = size || { width: 250, height: 115 };
    return chart;
}

function get_donut_chart(address, property, title, tooltip, size) {
    var chart = address[property];
    if (chart === undefined) {
        chart = create_donut_chart(title, tooltip, size);
        address[property] = chart;
    }
    return chart;
}

function create_map(names, values) {
    var result = {};
    for (var i = 0; i < names.length; i++) {
        result[names[i]] = values[i];
    }
    return result;
}

function get_variations(basenames, suffixes) {
    return suffixes.map(function (suffix) {
        return basenames.map(function (basename) {
            return basename + suffix;
        });
    }).reduce(function (a, b) {
        return a.concat(b);
    });
}

var colours = get_variations(['blue', 'gold', 'red', 'green', 'purple', 'orange','lightBlue', 'lightGreen', 'cyan', 'black'], ['', '300', '100', '500', '200', '400']);

function get_colours(count) {
    if (count <= colours.length) {
        return colours.slice(0, count);
    } else {
        return colours.slice(0).concat(get_colours(count - colours.length));
    }
}

function get_colour_map(keys, palette) {
    return create_map(keys, palette ? get_colours(keys.length).map(function (c) { return palette[c]; }) : get_colours(keys.length));
}

function get_total(objects, property) {
    return objects.map(function (s) { return s[property]; }).reduce(function (a, b) { return a + b; });
}


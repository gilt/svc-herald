define([
    'underscore',
	'backbone'
], function (_, Backbone) {
    'use strict';

    var App = {
        // event pub sub for the app.
        Events: _.extend({}, Backbone.Events)
    };

    return App;
});

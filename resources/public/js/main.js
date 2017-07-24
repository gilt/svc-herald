'use strict';

require.config({
    shim: {
        underscore: {
            exports: '_'
        },
        backbone: {
            deps: [ 'underscore', 'jquery' ],
            exports: 'Backbone'
        },
        epoxy : {
          deps: [ 'backbone' ]
        },
        bootstrap: {
            deps: [ 'jquery' ]
        },
        bootstrapselect: {
          deps: [ 'bootstrap' ]
        },
        momenttimezone : {
          deps: [ 'moment' ]
        },
        datetimepicker: {
          deps: [ 'jquery', 'moment', 'bootstrap' ]
        }
    },
    paths: {
        jquery: [
            'https://ajax.googleapis.com/ajax/libs/jquery/3.1.1/jquery.min',
            'herald/webjars/jquery/3.1.1/jquery.min'
            ],
        underscore: [
            'https://cdnjs.cloudflare.com/ajax/libs/lodash.js/4.15.0/lodash.min',
            'herald/webjars/lodash/4.15.0/lodash.min'
            ],
        backbone: [
            'https://cdnjs.cloudflare.com/ajax/libs/backbone.js/1.3.3/backbone-min',
            'herald/webjars/backbonejs/1.3.2/backbone-min'
            ],
        epoxy: [
          'https://cdnjs.cloudflare.com/ajax/libs/backbone.epoxy/1.2/backbone.epoxy.min',
          'herald/webjars/backbone.epoxy/1.2/backbone.epoxy.min'
        ],
        text: 'herald/webjars/requirejs-text/2.0.15/text',
        bootstrap: [
            'https://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/3.3.7/js/bootstrap.min',
            'herald/webjars/bootstrap/3.3.7/js/bootstrap.min'
        ],
        bootstrapselect: [
          'https://cdnjs.cloudflare.com/ajax/libs/bootstrap-select/1.11.2/js/bootstrap-select.min',
          'herald/webjars/bootstrap-select/1.11.2/js/bootstrap-select.min'
        ],
        moment: [
          'https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.15.2/moment.min',
          'herald/webjars/momentjs/2.15.2/min/moment.min'
        ],
        momenttimezone: [
          'https://cdnjs.cloudflare.com/ajax/libs/moment-timezone/0.5.5/moment-timezone-with-data.min',
          'herald/webjars/moment-timezone/0.5.5/moment-timezone-with-data'
        ],
        datetimepicker: [
          'https://cdnjs.cloudflare.com/ajax/libs/bootstrap-datetimepicker/4.17.43/js/bootstrap-datetimepicker.min',
          'herald/webjars/eonasdan-bootstrap-datetimepicker/4.17.43/build/js/bootstrap-datetimepicker.min'
        ]
    }
});

require(
    ['backbone',
     'herald/js/app',
     'herald/js/router'
     ], function (Backbone, App, ApplicationRouter) {

    new ApplicationRouter();
	/*jshint nonew:false*/
	// Initialize routing and start Backbone.history()
	Backbone.history.start();
});

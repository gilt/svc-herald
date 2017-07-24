define([
  'jquery',
  'underscore',
  'backbone',
  'epoxy',
  'bootstrap',
  'bootstrapselect',
  'moment',
  'momenttimezone',
  'datetimepicker',
  'herald/js/app',
  'text!herald/templates/events.html'
], function($, _, Backbone, Epoxy, Bootstrap, BootstrapSelect, moment, momentTz,  DateTimePicker, App, eventsTpl) {
  'use strict';

  var dateFormat = 'DD/MM/YYYY HH:mm';
  
  var EventModel = Backbone.Model.extend({
    urlRoot: '/herald/events',
    defaults: {
      id: undefined,
      name: '',
      start: '',
      duration: 'PT1H',
      recurrence: 'daily',
      affected: ['web'],
      scale: 1,
      active: true
    },
    durations: {
      PT1M : '1 minute',
      PT5M : '5 minutes',
      PT10M : '10 minutes',
      PT30M : '30 minutes',
      PT1H : '1 hour',
      PT3H : '3 hours',
      PT6H : '6 hours',
      PT12H : '12 hours',
      PT24H : '1 day',
      PT72H : '3 days',
      PT168H : '1 week',
      PT336H : '2 weeks'
    },
    zones: {
      'America/New_York': 'New York',
      'Europe/Dublin': 'Dublin',
      'UTC': 'UTC'
    },
    humaniseStart: function() {
      return moment(this.get('start')).fromNow();
    },
    getStartZone: function() {
      if(this.get('start')) {
        var offset = moment.parseZone(this.get('start')).format('Z');
        return _.findKey(this.zones, function(label, z) {
          return moment.tz(z).format('Z') === offset;
        });
      }
    }
  });

  var EventsCollection = Backbone.Collection.extend({
    model: EventModel,
    url: '/herald/events'
  });

  var EventsCollectionView = Backbone.View.extend({
    el: '#content',
    template: _.template($(eventsTpl).filter('#events').html()),

    events: {
     'click #add-event-btn': 'addEvent',
     'click #refresh-events-btn': 'refreshEvents',
     'click .edit-event-btn': 'editEvent',
     'click .copy-event-btn': 'copyEvent',
     'click .remove-event-btn': 'removeEvent'
    },

    addEvent: function(event) {
      App.Events.trigger('events:add');
    },

    editEvent: function(event) {
      App.Events.trigger('events:edit', $(event.target).closest('td').data('target'));
    },

    copyEvent: function(event) {
      App.Events.trigger('events:copy', $(event.target).closest('td').data('target'));
    },

    removeEvent: function(event) {
      App.Events.trigger('events:remove', $(event.target).closest('td').data('target'));
    },

    refreshEvents: function(event) {
      App.Events.trigger("events:refresh");
    },

    render: function() {
      this.$el.html(this.template());
    }
  });

  var EventView = Backbone.Epoxy.View.extend({
    el: _.template($(eventsTpl).filter("#event").html()),
    bindings: "data-bind",
    events: {
      'click #event-submit': 'submit',
      'change #event-timezone': 'changeTimeZone'
    },

    computeds: {
      scale: {
        get: function() {
            return this.model.get('scale');
        },
        set: function(value) {
          this.model.set('scale', parseInt(value));
        }
      },
      start: {
        get: function() {
          return moment.parseZone(this.model.get('start')).format(dateFormat);
        },
        set: function(value) {
          this.setStartDate(value);
        }
      }
    },

    changeTimeZone: function() {
      this.setStartDate(moment.parseZone(this.model.get('start')).format(dateFormat));
    },

    setStartDate: function(value) {
      if (value) {
        var zone = $('#event-timezone', '#dialog').val();
        var offset = moment(value, dateFormat).tz(zone).format('Z');    
        this.model.set('start', (moment(value, dateFormat).format('YYYY-MM-DDTHH:mm:ss') + offset + '[' + zone + ']'), { silent: true });
      }
    },

    submit: function() {
      App.Events.trigger("events:submit", this.model);
    },

    render: function() {
      $('#dialog').html(this.el);
      $('#event-start-datetimepicker', '#dialog').datetimepicker({ format: dateFormat });
      $('.selectpicker', '#dialog').selectpicker({
        width: '100%',
        iconBase: 'fa',
        tickIcon: 'fa-check'
      });

      this.$el.modal('show');
    },

    close: function() {
      this.$el.modal('hide');
    }
  });

  return {
    EventModel: EventModel,
    EventsCollection: EventsCollection,
    EventView: EventView,
    EventsCollectionView: EventsCollectionView
  };
});

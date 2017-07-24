define([
  'backbone',
  'herald/js/app',
  'herald/js/modules/commons',
  'herald/js/modules/events'
], function(Backbone, App, Commons, Events) {
  'use strict';

  var ApplicationRouter = Backbone.Router.extend({

    routes: {
      "": "home",
      "events": "events"
    },
    initialize: function() {
      // commons
      this.headerView = new Commons.HeaderView();
      this.headerView.render();
      this.footerView = new Commons.FooterView();
      this.footerView.render();

      // bind events
      App.Events.on('events:refresh', this.events, this);
      App.Events.on('events:add', this.addEvent, this);
      App.Events.on('events:submit', this.submitEvent, this);
      App.Events.on('events:edit', this.editEvent, this);
      App.Events.on('events:copy', this.copyEvent, this);
      App.Events.on('events:remove', this.removeEvent, this);
    },
    home: function() {
      this.navigate('/events', true);
    },
    events: function() {
      var self = this;
      var events = new Events.EventsCollection();
      events.fetch().done(function() {
        self.eventsView = self.eventsView || new Events.EventsCollectionView();
        self.eventsView.collection = events;
        self.eventsView.render();
      });
    },
    copyEvent: function(eventId) {
      var self = this;
      var event = new Events.EventModel({id: eventId});
      event.fetch().done(function() {
        event.set( { id: undefined, name: event.get('name') + ' [COPY]' } );
        self.addEvent(event);
      });
    },
    addEvent: function(model) {
      this.eventView = new Events.EventView({ model: model ? model : new Events.EventModel() });
      this.eventView.render();
    },
    submitEvent: function(event) {
      var self = this;
      event.save(null, {
        success: function(model, response) {
          self.eventView.close();
          var alertView = new Commons.AlertView( { model: { message: "Event " + model.id + " successfully submitted", type: 'success' } });
          alertView.render();
          self.events();
        },
        error: function(model, response) {
          var alertView = new Commons.AlertView( { model:  { message: "Unable to sumbit event: " + response.statusText, type: 'danger' }, el: '#event-alert' } );
          alertView.render();
        }
      });
    },
    editEvent: function(eventId) {
      var self = this;
      var event = new Events.EventModel({id: eventId});
      event.fetch().done(function() {
        self.eventView = new Events.EventView({ model: event });
        self.eventView.render();
      });
    },
    removeEvent: function(eventId) {
      var self = this;
      var event = new Events.EventModel({id: eventId});
      var resp = confirm("Are you sure you want to remove event with ID: " + eventId);
      if (resp) {
        event.fetch().done(function() {
          event.destroy({
            success: function(model, response) {
              var alertView = new Commons.AlertView( { model: { message: "Event " + model.id + " successfully removed", type: 'success' } });
              alertView.render();
              self.events();
            },
            error: function(model, response) {
              var alertView = new Commons.AlertView( { model:  { message: "Unable to remove event: " + response.statusText, type: 'danger' } } );
              alertView.render();
            }
          });
        });
      }
    }
  });

  return ApplicationRouter;
});

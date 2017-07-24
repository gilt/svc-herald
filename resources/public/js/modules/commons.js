define([
  'jquery',
  'underscore',
  'backbone',
  'text!herald/templates/commons.html'
], function($, _, Backbone, commonsTpl) {
  
  HeaderView = Backbone.View.extend({
    el: "#header",
    template: _.template($(commonsTpl).filter('#header').html()),

    render: function() {
      this.$el.html(this.template);
    }
  });

  FooterView = Backbone.View.extend({
    el: "#footer",
    template: _.template($(commonsTpl).filter('#footer').html()),
    render: function() {
      this.$el.html(this.template);
    }
  });

  AlertView = Backbone.View.extend({
    el: '#alert',
    template: _.template($(commonsTpl).filter("#alert").html()),

    render: function() {
      this.$el.html(this.template(this.model));
    }
  });

  return {
    HeaderView: HeaderView,
    FooterView: FooterView,
    AlertView: AlertView
  }
});

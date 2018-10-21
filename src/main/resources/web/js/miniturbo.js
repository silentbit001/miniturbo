Vue.component('mt-resources', {
  template: `
	  <div class="row">
	  	<div class="col-md-2">{{resource}}</div>
	  	<div class="col-md-2"><input type="checkbox" data-toggle="toggle"></div>
	  	<div class="col-md-2"></div>
	  </div>
  `,
  props: ['resource']
})

var resourceApp = new Vue({
	el: "#resource-list",
	data: {
		resources: []
	}
});

$.ajax({
	url: "/api/resource",
	success: function(result) {
		_.forEach(JSON.parse(result), function(r) {resourceApp.resources.push(r)});
	}
});
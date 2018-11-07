var ToggleButtonGroup =  ReactBootstrap.ToggleButtonGroup;
var ToggleButton =  ReactBootstrap.ToggleButton;
var Row = ReactBootstrap.Row;
var Col = ReactBootstrap.Col;
var Badge = ReactBootstrap.Badge;

class ResourceItem extends React.Component {
	
	constructor(props) {
	    super(props);
	    this.handleChange = this.handleChange.bind(this);
	    this.state = {value: "off", retry: 0};
	    this.checkState(100, false);
	}
	
	checkState(delay, retry) {
		_.delay(() => {
			console.log('Hi! Im Elfo.');
			$.ajax({
				url: "/api/resource/" + this.props.resource + "/status",
				timeout: 5000,
				success: (response) => {
					var data = JSON.parse(response);
					console.log("Response: ", data, this.state);
					if (data.ready) {
						console.log("We are good!");
						if (this.state.value == "off") {
							this.turnOn();
						}
						this.setState({retry: 0, value: "on", image: data.image, ports: data.ports});
					} else if (this.state.retry >= 30) {
						this.turnOff();
					} else if (retry) {
						this.setState({value: this.state.value, retry: this.state.retry + 1});
						this.checkState(delay, retry);
					}
				},
				error: (data, response) => {
					console.log("Fail: ", data, response);
					this.setState({value: this.state.value, retry: this.state.retry + 1});
					this.checkState(delay, retry);
				}
			});
		}, delay);
	}
	
	turnOff() {
		this.setState({value: "off"});
		this.toggleResource(this.state.value);
	}
	
	turnOn() {
		this.setState({value: "on"});
		this.toggleResource(this.state.value);
	}
	
	handleChange(value, event) {
		console.log( "Toggle", this.props.resource, value);
		this.setState({ value });
		this.toggleResource(value);
		if (value == "on") {
			this.checkState(10000, true);
		} else {
			this.setState({image: null, ports: null});
		}
	}
	
	toggleResource(value) {
		var action = value == "on" ? "/start" : "/stop";
		$.ajax({type: "POST", url: "/api/resource/" + this.props.resource + action});
	}

	render() {
		
		var imageRender = this.state.image ? <Badge variant="primary">image: {this.state.image}</Badge> : null;
		
		var ports = this.state.ports;
		var portRender = ports ? Object.keys(ports).map((portI) => 
			<Badge key={portI} variant="success">{portI}:<a href={"//".concat(window.location.hostname).concat(":").concat(ports[portI])}>{ports[portI]}</a></Badge>) : null;
		
		return (
			<Row>
				<Col sm={1}></Col>
			 	<Col sm={2}>{this.props.resource}</Col>
			 	<Col sm={1}>
			 		<ToggleButtonGroup type="radio" name="options" value={this.state.value} defaultValue={this.state.value} onChange={this.handleChange}>
			 			<ToggleButton value={"on"}>on</ToggleButton>
			 			<ToggleButton value={"off"}>off</ToggleButton>
			 		</ToggleButtonGroup>
	    	    </Col>
	    	    <Col sm={1}>
	    	    	{imageRender}{portRender}
	    	    </Col>
	    	</Row>
		);
	}
	
}

class ResourceList extends React.Component{
	
	constructor(props) {
	    super(props);
	    this.state = {resources: []};
	    this.poll();
	}
	
	componentDidMount() {
	    this.timerID = setInterval(
	      () => this.poll(),
	      10000
	    );
	}
	
	componentWillUnmount() {
	    clearInterval(this.timerID);
	}
	
	poll() {
		
		$.ajax({
			url: "/api/resource",
			success: (response) => {
				this.setState({resources: _.sortBy(JSON.parse(response))});
			}
		});
	}
	
	render() {
		
		const resources = this.state.resources.map((resource) => 
			<ResourceItem key={resource} resource={resource} />
		);
		
		return (
				<div className="resource-list container-fluid">
					{resources}
				</div>
		);
	}
}

function MiniTurboApp() {
  return (
    <div>
      <ResourceList />
    </div>
  );
}

ReactDOM.render(<MiniTurboApp />, document.getElementById('root'));

var connection = new WebSocket('ws://' + window.location.host + '/status');
//When the connection is open, send some data to the server
connection.onopen = function () {
  connection.send('Ping'); // Send the message 'Ping' to the server
};

// Log errors
connection.onerror = function (error) {
  console.log('WebSocket Error ', error);
};

// Log messages from the server
connection.onmessage = function (e) {
  console.log('Server: ', e, e.data);
};

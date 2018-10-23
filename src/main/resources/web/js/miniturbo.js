var ToggleButtonGroup =  ReactBootstrap.ToggleButtonGroup;
var ToggleButton =  ReactBootstrap.ToggleButton;
var Row = ReactBootstrap.Row;
var Col = ReactBootstrap.Col;

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
				success: (response) => {
					var data = JSON.parse(response);
					console.log("Response: ", data, this.state);
					if (data.ready) {
						console.log("We are good!");
						if (this.state.value == "off") {
							this.turnOn();
						}
						this.state.retry = 0;
					} else if (this.state.retry >= 30) {
						this.turnOff();
					} else if (retry) {
						this.setState({value: this.state.value, retry: this.state.retry + 1});
						this.checkState(delay, retry);
					}
				},
				error: (response) => {
					console.log("Fail: ", response);
					this.setState({value: this.state.value, retry: this.state.retry + 1});
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
		}
	}
	
	toggleResource(value) {
		var action = value == "on" ? "/start" : "/stop";
		$.ajax({type: "POST", url: "/api/resource/" + this.props.resource + action});
	}

	render() {
		return (
			<Row>
				<Col sm={1}></Col>
			 	<Col sm={3}>{this.props.resource}</Col>
			 	<Col sm={1}>
			 		<ToggleButtonGroup type="radio" name="options" value={this.state.value} defaultValue={this.state.value} onChange={this.handleChange}>
			 			<ToggleButton value={"on"}>on</ToggleButton>
			 			<ToggleButton value={"off"}>off</ToggleButton>
			 		</ToggleButtonGroup>
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
	      5000
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
				<div className="resource-list">
					<h1>Resource List</h1>
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
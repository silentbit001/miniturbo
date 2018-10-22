var ToggleButtonGroup =  ReactBootstrap.ToggleButtonGroup;
var ToggleButton =  ReactBootstrap.ToggleButton;
var Row = ReactBootstrap.Row;
var Col = ReactBootstrap.Col;

class ResourceItem extends React.Component {
	
	constructor(props) {
	    super(props);
	    this.handleChange = this.handleChange.bind(this);
	    this.state = {value: "off"};
	    this.checkState();
	}
	
	checkState() {
		_.delay(() => {
			console.log('Hi! Im Elfo.');
			this.setState({value: "off"});
			$.ajax({
				url: "/api/resource/" + this.props.resource "/state",
				success: (response) => {
					data = JSON.parse(data);
					console.log(data);
				},
				error: () => {
					//this.toggleResource(this.state.value);	
				}
			});
		}, 3000);
	}
	
	handleChange(value, event) {
		console.log( "Toggle", this.props.resource, value);
		this.setState({ value });
		this.toggleResource(value);
		this.checkState();
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
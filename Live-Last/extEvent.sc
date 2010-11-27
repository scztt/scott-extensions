+ Event {
	<< {
		| event |
		^this.composeEvents(event);
	}
	
	<+ {
		| event |
		this.putAll(event);
	}
}


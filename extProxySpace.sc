+ProxySpace {
	*get {
		| s, name, clock |
		var existing = all[name];
		existing.isNil.if({ 
			existing = this.new(s, name, clock) 
		},{
			if( existing.server != s, {
				"Specified Server does not match server of existing ProxySpace.".warn;
			})
		});
		^existing;
	}
}


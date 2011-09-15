ProxyEnvironment : Environment {
	var pspace;

	*new { arg s; 
		^super.new.init(s);
	}
	*make { arg s; 
		^super.make.init(s);
	}
	*use { arg s; 
		^super.use.init(s);
	}
	
	init { arg s;
		pspace = ProxySpace(s);	
	}
	
	at { arg key, value;
		(super.at(key).isNil).if({
			^pspace.at( key );
		})
	}
	
	put { arg key, value;
		"Putting ".post;
		(value.class.findMethod(\prepareForProxySynthDef).isNil).if({
			"non NodeProxy object".postln;
			^super.put( key, value );
		}, {
			"NodeProxy object".postln;
			^pspace.put( key, value );
		});
	}		
}
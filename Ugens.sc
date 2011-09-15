Recurse : UGen {
	*new {
		| sig, func, count=1 |
		var out;
		out = sig;
		count.do({
			out = func.value( out );
		});
		^out
	}	
}

Vary : UGen {
	*ar {
		| min, max, freq=1 |
		var width = (max-min)/2;
		if( freq.isArray, {
			^Vary.ar( min, max, Vary.ar( *freq ))
		},{
			^LFDNoise3.ar( freq, width, min + width )
		})
		
	}

	*kr {
		| min, max, freq=1 |
		var width = (max-min)/2;
		if( freq.isArray, {
			^Vary.kr( min, max, Vary.kr( *freq ))
		},{
			^LFDNoise3.kr( freq, width, min + width )
		})
		
	}
}
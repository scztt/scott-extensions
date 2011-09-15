DebugTest { 
	testMethod 
	{ 
		| a,b,c |
		var d, e, f; 
		var x=10;
		this.postln;
		d = d ?? {
			e = this.size;
			f = e * 2;
			f = a ? x;
			f;
		};
		
		if( this.isKindOf("Array"), {
			d = this.size;
			e = this;
			f = this.collect( _ );
		},{
			//d = this.size;
			e = this;
			f = this.collect( _ );
		});
		^d;
	} 
	
	noner { |prepArgs, ugenGraphFunc, rates, prependArgs|
		^SynthDef.wrap({ |lag,amp=1|
			amp.lag(lag) * SynthDef.wrap( ugenGraphFunc, rates, prependArgs )
		}, prependArgs: prepArgs);
	}
	
	test {
		this.this
	}
	
	+= {
		"increment".postln;
	}
}
	
+Object {
	getCurrentLine {
		_GetCurrentLine
	}
}
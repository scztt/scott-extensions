
Pnlive : FilterPattern {
	var <repeats=1, <>repeatsLeft=1;
	*new { arg pattern, repeats=inf;
		^super.new(pattern).init(repeats);
	}
	
	init {
		| r |
		repeats = r;
		repeatsLeft = r;
	}
	
	storeArgs { ^[pattern,repeats] }

	embedInStream {
		| in |
		var str;
		str = pattern.asStream;
		while ({ (repeatsLeft = repeatsLeft-1)>=0 } , {
			in = pattern.embedInStream( in );
		});
		repeatsLeft = repeats;
		^in;
	}
	
	repeats_ {
		| r |
		(repeats==inf).if({
			repeatsLeft = r;
		},{
			repeatsLeft = repeatsLeft + (r - repeats);
			("new repeatsLeft = " + repeatsLeft).postln;
		});
		repeats = r;
		r.postln;
	}	
}



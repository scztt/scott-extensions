+SoundFile {
	extractMarkers {
		var cueRe, markerList, cueStrings;
		cueRe = "Cue ID :\\s*(\\d).*Pos :\\s*(\\d*).*";
		cueStrings = this.readHeaderAsString.findRegexp(cueRe);
		if( cueStrings.size%3 == 0, {
			^cueStrings.clump(3).collect({
				| cue |
				cue[2][1].asInteger;
			})
		},{
			"Unexpected result searching for cue markers".warn;
			this.readHeaderAsString.postln;
		});
		^[]
	}
}
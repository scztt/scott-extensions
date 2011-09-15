HardCompress : UGen {
	*ar {
		| sig, speed=1, minAmp=0.01, amp=1.0 |
		var inAmp, ampThresh;
		inAmp = Amplitude.ar( sig, 0.0001, speed ).max( minAmp );
		^sig/inAmp;
	}
}

HardCompress2 : UGen {
	*ar {
		| sig, speed=1, minAmp=0.01, amp=1.0 |
		var inAmp, ampThresh;
		inAmp = Slew.ar(sig.abs, 999999, speed).max( minAmp );
		^sig/inAmp;
	}
}

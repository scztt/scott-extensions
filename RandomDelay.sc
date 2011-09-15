
RandomDelayN : UGen {
	
	*ar {
		arg in, freq=1, totalMaxDelay=1.0, minDelay=0.0, maxDelay=1.0, feedback=0.0;
		var sig, delayAmt, trig;
		delayAmt = TRand.ar( minDelay, maxDelay, LFPulse.ar( freq, 0, 0.1, 1, -0.5 ) );
		sig = in + (feedback * DelayC.ar( in, totalMaxDelay, delayAmt ) );
		LocalOut.ar( sig );
		^sig;
	}
}

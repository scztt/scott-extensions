// Weedwacker classes


// rate: 1..128
// width: -1..1
// phase: 0..1

LFO : UGen {
	*kr {
		arg rate=64, bpm=120, width=0, gate=0, phase=0, type=0, mul=1, add=0;
		var freq, amp, tri, pulse;
		
		freq = ((480/bpm) * rate).reciprocal;
		amp = 1;
		width = (width*0.5)+0.5;
		
		tri = VarSaw.kr( freq, phase, width );
		pulse = LFPulse.kr( freq, phase, width );
		^(Select.kr( type, [pulse, tri] )*mul)+add ;
	}
	
	*ar {
		arg rate=64, bpm=120, width=0, gate=0, phase=0, type=0, mul=1, add=0;
		var freq, amp, tri, pulse;
		
		freq = ((480/bpm) * rate).reciprocal;
		amp = 1;
		width = (width*0.5)+0.5;
		
		tri = VarSaw.ar( freq, phase, width );
		pulse = LFPulse.ar( freq, phase, width );
		^(Select.ar( type, [pulse, tri] )*mul)+add ;
	}
}

EG : UGen {
// Skipped retrigger
	*kr {
		| gate, vel, bpmPower, bpp, a, d, s, r, tune, track, n |
		var newGate, newA, newD, newS, newR, modBpp;
		modBpp = (3312/bpp) ;

		newGate = ((gate*vel)+gate) * 
			(1 + (tune*0.007874*track));
	
		newA = (modBpp * (a.dbamp-1)).ampdb;
		newD = (modBpp * (d.ampdb-1)).ampdb;
		newS = s*s;
		newR = (modBpp * (r.dbamp-1)).ampdb;
		
		^EnvGen.kr( Env.adsr(a, d, s, r, 1), newGate, newGate );
	}
}


EG2 : UGen {
	*kr {
		| bpm, rate, width, gate, repeat, cut, x, shift |
		var freq, amp, sync, phase, lfo, hold, router,
			cutOut, xOut, sftOut;
		
		freq = ((60/bpm)*rate).reciprocal;
		
		lfo = VarSaw.kr( freq, (width+1)*0.5, (width*0.5)+0.5, 0.5, 0.5);
		hold = repeat + Trig.kr( gate, (60/bpm)*rate );
		
		router = Gate.kr( lfo, hold );

		cutOut = cut * router.pow(3);
		xOut = x * 0.016666667 * router.pow(3);
		sftOut = shift * 0.5 * router.pow(2);
		^[cutOut, xOut, sftOut];
	}
}

WeedPitch : UGen {
	*kr {
		| pitch, gate, bpm, vibrato, vibAmt, vibMod, vibRate, vibSync, pitchBend, tune, fine, oct |
		var tuneOut, p, vib;
		
		vib = WeedVib.kr( bpm, vibrato, vibAmt, vibMod, vibRate, vibSync );
		tuneOut = pitch + pitchBend + tune + fine;
		p = tuneOut + (oct*12) + vib;
		^[tuneOut, p];
	}
}

WeedVib : UGen {
	*kr {
		| bpm, vib, amt, mod, rate, sync |
		var f, a, snc;
		f = (0.05 + rate) * (bpm/128) * 0.133333337;
		a = amt.pow(2) + (vib*mod*0.033);
		^SinOsc.ar( f, 1, a );
	}
}


WeedOsc : UGen {
	*ar {
		| a, 
			max, cmax, maxMod,
		  	min, cmin, minMod,
		  	res, cres, resMod,
		  	up, cup, upMod,
		  	down, cdown, downMod,
		  	p,
		  	shift, cshift, fineShift, sftShift,
		  	drive, cdrive, driveMod 
		  	noise, shift2 |
		var maxOut, minOut, resOut, upOut, downOut, shiftOut, outOut, driveOut,
			noiseSig, pulseSig, mirrorSig, peakEqSig,mirrorFeedback;

		#maxOut, minOut, resOut, upOut, downOut, shiftOut, outOut, driveOut = 
			WeedControl.kr( 
				max, cmax, maxMod,
		 	 	min, cmin, minMod,
		  		res, cres, resMod,
		  		up, cup, upMod,
		  		down, cdown, downMod,
		  		
		  		p,
		  		shift, cshift, fineShift, sftShift,
		   		drive, cdrive, driveMod );
	
		noiseSig = WhiteNoise.ar(  noise.pow(2)*0.5 );
		mirrorFeedback = LocalIn.ar(1);
		pulseSig = WeedPulse.ar( p.midicps+noiseSig, mirrorFeedback, upOut, downOut, a*outOut );
		
		// find correst cres input to match reaktor?
		peakEqSig = MidEQ.ar( pulseSig, (shiftOut+shift2).midicps, 1.01-resOut, 70 );
		mirrorSig = Mirror.ar( peakEqSig, minOut.min(0), maxOut );
		//SharedOut.ar( 1, outOut );
		LocalOut.ar( mirrorSig );
		^peakEqSig;
		//^SinOsc.ar( p.midicps + noiseSig );
	}
}

WeedControl : UGen {
	*kr {
		| max, cmax, maxMod,
		  min, cmin, minMod,
		  res, cres, resMod,
		  up, cup, upMod,
		  down, cdown, downMod,
		  p,
		  shift, cshift, fineShift, sftShift,
		   drive, cdrive, driveMod |
		
		^[ 
			cmax + (max + maxMod + 0.0055),
			cmin + (min + minMod+ 0.0055),
			((res * resMod * 0.00166667) + cres).min(0.999),
			((up * upMod * 0.3)+cup.pow(2)).max(1),
			((down * downMod * 0.3)+cdown.pow(2)).min(0),
			( (shift*cshift) + sftShift + (1+fineShift) + p ),
			( ( downMod*0.004*drive ) + (cdrive.pow(3)) ).max(-0.051),
			cdrive.pow(3)
		]
	}
}

WeedPulse : UGen {
	*ar {
		| freq, width, up, down, mul=1, add=0 |
		^Slew.ar( LFPulse.ar( freq, 0, (width*0.5)+0.5, mul, add ), up*freq, down*freq );
	}

	*kr {
		| freq, width, up, down, mul=1, add=0 |
		^Slew.kr( LFPulse.kr( freq, 0, (width*0.5)+0.5, mul, add ), up*freq, down*freq );
	}
}
 

Mirror : UGen {
	*ar {
		| sig, min=(-1), max=1 |
		var width, middle; 
		width = (0.5*(max-min));
		middle = min + width;
		^(sig-middle).fold2( width ) + middle
	}
	
	*kr {
		| sig, min=(-1), max=1 |
		var width, middle; 
		width = (0.5*(max-min));
		middle = min + width;
		^(sig-middle).fold2( width ) + middle
	}	
}



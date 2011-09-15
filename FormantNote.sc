FormantNote {
	classvar <selectedList;
	
	var tabletSize=600.0;
	var x, y, size;
	var amp, fund, formFreq, width;
	var synth, delay, draw;
	

	/* SynthDef
	SynthDef( \formantNote, {
		| out=0, amp=0.3, freq=640, formFreq=770, width=400, delay=1 |
		var sig;
		amp = Lag3.kr( amp, delay );
		freq = Lag3.kr( freq, delay );
		formFreq = Lag3.kr( formFreq, delay );
		width = Lag3.kr( width, delay );
		
		sig = Formant.ar( freq, formFreq, width, amp );
		
		Out.ar( out, sig );
	}).send(s).store;
	*/

	*initClass {
		selectedList = Array.new;
	}
	
	*new {
		| amp, fund, formFreq, width, delay=1, out=0  |
		^super.newCopyArgs.init( amp, fund, formFreq, width, delay, out );
	}
	
	init {
		| argAmp, argFund, argFormFreq, argWidth, argDelay, out  |
		amp = argAmp; fund = argFund;
		formFreq = argFormFreq; width = argWidth;
		delay = argDelay;
				
		synth = Synth.tail( Server.default, \formantNote, [out, amp, fund, formFreq, width, delay] );
	}
	
	draw {
		var x, y, w, h;
		Color.red.alpha_(amp+0.1).set;
		w = 12;
		h = (width/14000.0)*tabletSize;
		x = ((width/8000.0)*tabletSize) - (w/2);
		y = ( (1-(formFreq/14000.0))*tabletSize ) - (h/2);
		
		Pen.fillRect( Rect( x, y, w, h ) );
	}
	
	set {
		| x, y, pres |
		[x,y,pres].postln;
		amp = pres ? amp;
		formFreq = (1-(y/600.0)) * 14000.0;
		width = (x/600.0) * 8000.0;
		synth.set( \amp, amp, \formFreq, formFreq, \width, width );
//		[amp, formFreq, width].postln;
	}

	click { 
		| inx, iny |
		var w, h, x, y;
		w = 12;
		h = (width/14000.0)*tabletSize;
		x = ((width/8000.0)*tabletSize) - (w/2);
		y = ( (1-(formFreq/14000.0))*tabletSize ) - (h/2);
		Rect( x, y, w, h ).containsPoint( Point(inx, iny ) ).if({
			this.class.selectedList.add( this ); "clicked".postln; });
	}
	
	*setAll {
		|x, y, pres |
		if( selectedList.size != 0,  
			{ selectedList.do({ | fn | fn.set( x, y, pres ) }) } )
	}
		
	*unclickAll {
		selectedList = Array.new;
	}

}
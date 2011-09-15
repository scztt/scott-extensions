WeedwackerFactory {
	var server, <presets;
	var <group, <groupBus, <outSynth, <positionSynth;
	
	var <controlSpecs, <controls;
	var <>currentPresetName;
	var <>currentPresetDict;
	var <>currentPreset;
	var <playingNotes, <currentlyPlaying=false, <>queuedNotes,  releaseDelay=0;
	var popup, outSynthDef=\quadOut, outputPopUp;
	
	// adhoc
	var <>noteArray=1;
	var <>inUgen;
	
	*new { 
		|server|
		^super.new.init(server);
	}
	
	init {
		| s |
		server = server ? Server.default;		
		this.loadPresets;
		this.setPreset( "TooSweet" );
		this.synthDef.store.send(server);
		this.stereoOutSynthDef.store.send(server);
		this.quadOutSynthDef.store.send(server);
		controlSpecs = Dictionary.new;
		this.makeControlSpecs;
		controls = Dictionary.new;
		playingNotes = Dictionary.new;
		queuedNotes = [];
		group = Group.new;
		groupBus = Bus.audio( server, 3 );
		outSynth = Synth.new( outSynthDef, [ \out, 0, \in, groupBus.index ], group, \addAfter )
	}
	
	loadPresets {
		var f;
		f = File.new( "/Users/Scott/Library/Application Support/SuperCollider/presets/WeedPresets.scd", "r" );
		presets = f.readAllString.interpret;
		f.close;
		^presets
	}
	
	savePresets {
		var f;
		// Write current preset
		presets[ currentPresetName ] = currentPresetDict;
		// Save to file
		f = File.new( "/Users/fsc/scwork/presets/WeedPresets.sc", "w" );
		f.write( presets.asCompileString );
		f.close;
		"Presets saved...".postln;
	}
	
	newPreset {
		| name |
		presets[ name ] = currentPresetDict;
		popup.items_( presets.keys.asArray );
		popup.value_( presets.keys.asArray.indexOfEqual( name ) );
	}
	
	allPresets {
		^this.presets.keys
	}
	
	setOutputSynth {
		| def |
		if( (def==\quadOut) || (def==\stereoOut) , {
			outSynthDef = def;
			outSynth.free;
			outSynth = Synth.new( outSynthDef, [ \out, 0, \in, groupBus.index ], group, \addAfter );
		},{
			"Nope. \quadOut or \stereoOut".postln
		})
	}
	
	setPreset {
		| preset |
		if( presets.includesKey( preset ), {
			currentPresetName = preset;
			currentPresetDict = presets[ preset ];
			if( controls.isNil.not, {
				currentPresetDict.keysValuesDo({
					| key, value |
					controls[key].value_( value );
				})
			})
			^currentPreset = currentPresetDict.asKeyValuePairs;
		},{ ^nil })
		
	}
	
	deletePreset {
		var num, keys;
		keys = presets.keys.asArray;
		num = keys.indexOfEqual( currentPresetName );
		num = (num-1).max(0);
		if( num.isNil, {num=0});
		presets.removeAt( currentPresetName );
		this.setPreset( presets.keys.asArray[num] ); 
		popup.items_( presets.keys.asArray );
		popup.value_( num );
	}
	
	randomPreset {
		var r = presets.values.size.rand;
		^this.setPreset( presets.keys.asArray[r].postln )
	}
	
	synth {
		| preset, options, target, addAction |
		var settings;
		^Synth.newPaused( \weedwacker, currentPreset ++ [\posIn, positionSynth.bus.index] )
	}
	
	playNote {
		| notePitch=60, velocity=64, amp=0.3 |
		var synth;
		notePitch = notePitch.round(0.05);
		if( playingNotes[notePitch].isNil, {
			synth = Synth.new( \weedwacker, 
				currentPreset ++ [\notePitch, notePitch, \velocity, velocity, \out, groupBus.index ],
				group, \addToTail, \gate, 1 );
			playingNotes[notePitch] = synth;
		})
		^synth
	}

	
	playNotes {
		| notes, vel, amp=1.0 |
		notes.do({
			|notePitch|
			this.playNote( notePitch, vel, amp );
		})
	}

	stopNote {
		| notePitch |
		if( notePitch.size>0, {
			notePitch.do({
				| p |
				p = p.round(0.05);
				server.sendBundle( releaseDelay.max( 0.03 ), playingNotes[p].setMsg( \gate, 0 ); );
				playingNotes.removeAt(p);	
			})
		}, {
			notePitch = notePitch.round(0.05);
			server.sendBundle( releaseDelay.max( 0.03 ), playingNotes[notePitch].setMsg( \gate, 0 ); );
			playingNotes.removeAt(notePitch);	
		})
	}
	
	freeNote {
		| notePitch |
		if( notePitch.size>0, {
			notePitch.do({
				| p |
				p = p.round(0.05);

				playingNotes[p].free;
				playingNotes.removeAt(p);	
			})
		}, {
			notePitch = notePitch.round(0.05);
			playingNotes[notePitch].free;
			playingNotes.removeAt(notePitch);	
		})
	}
	
	stopAll {
		server.listSendMsg( group.setMsg(  \endIn, releaseDelay.max(0.03) ) );
		playingNotes = Dictionary.new;
	}
	
	freeAll {
		group.freeAll;
		outSynth.free;
		outSynth = Synth.new( outSynthDef, [ \out, 0, \in, groupBus.index ], group, \addAfter );
	}
	
	startMIDI {
		MIDIIn.connect;
		MIDIIn.noteOn = { 
			|port, chan, note, vel| 
		}; 
		MIDIIn.noteOff = { 
			|port, chan, note, vel| 
		}; 
	}
	
	makeRoutingSelector {
		| view, name |
		var value, control, font;
		value = currentPresetDict[name];
		font = Font( "Helvetica", 9 );
		
		control = SCPopUpMenu( view, Rect( 0, 0, 60, 15 ) )
			.items_( ["lfo3", "lfo2", "lfo1", "env gen"] )
			.action_({
				| menu |
				currentPresetDict[name] = menu.value;
				currentPreset = currentPresetDict.asKeyValuePairs;
				group.set( name, menu.value );
			})
			.value_(value)
			.font_( Font("Helvetica",9) );
		SCStaticText( view, Rect( 0, 0, ("xxxxx->" ++ name).bounds(font).width+2, 15 ) )
			.string_( "-> " ++ name  )
			.font_( font );
		controls[ name ] = control;
		^control
	}
	
	makeControl {
		| view, name, style=\knob |
		var control, spec, value;
		spec = controlSpecs[name].asSpec;
		value = currentPresetDict[name];
		
		if( style==\knob, {
			control = EZKnob( view, 40@72, name.asString, spec, {
				| slider |
				currentPresetDict[name] = slider.value;
				currentPreset = currentPresetDict.asKeyValuePairs;
				group.set( name, slider.value );
			}, 	initVal:value, 
				labelWidth: 40,
				numberWidth: 40,
				back: Color.clear);
			control.knobView.color = 
				[ Color.grey.alpha_(0.85), Color.green.alpha_(0.25), 
					Color.black.alpha_(0.3), Color.black.alpha_(0.7) ];
		},{
			// do slider
		});
				
		controls[ name ] =  control;
		^control
	}
	
	makeOutControl {
		| view, name, style=\knob |
		var control, spec, value;
		spec = controlSpecs[name].asSpec;
		value = spec.default;
		
		if( style==\knob, {
			control = EZKnob( view, 40@72, name.asString, spec, {
				| slider |
				currentPresetDict[name] = slider.value;
				currentPreset = currentPresetDict.asKeyValuePairs;
				outSynth.set( name, slider.value );
			}, 	initVal:value, 
				labelWidth: 40,
				numberWidth: 40,
				back: Color.clear);
			control.knobView.color = 
				[ Color.grey.alpha_(0.85), Color.green.alpha_(0.25), 
					Color.black.alpha_(0.3), Color.black.alpha_(0.7) ];
		},{
			// do slider
		});
				
		controls[ name ] =  control;
		^control
	}
	
	gui {
		var w, v, names, search, newButton, modal, tablet, tabletCursor;
		var cv, curView, font, searchedPresetList, r, keycodes, downkey, tabletColor;
		
		font = Font( "Helvetica", 9 );
		
		w = SCWindow( "Weedwacker", Rect(100, 345, 400, 655));
		//key note listener
		keycodes = [ 122, 115, 120, 100, 99, 118, 103, 98, 104, 
			110, 106, 109, 44, 108, 46, 59, 127, 113, 50, 119, 
			51, 101, 114, 53, 116, 54, 121, 55, 117, 105, 57, 111, 48, 112 ];

		w.view.keyDownAction_({
			| a,b,c,d |
			downkey = keycodes.indexOf( d );
			if( downkey.notNil, {
				if( currentlyPlaying, {
					this.playNote(60+downkey)
				},{
					queuedNotes = queuedNotes.add( 60+downkey );
				})
			})
		});
		w.view.keyUpAction_({
			| a,b,c,d |
			downkey = keycodes.indexOf( d );
			if( downkey.notNil, {
				if( currentlyPlaying, {
					this.stopNote( 60+downkey )
				},{
					queuedNotes.remove( 60+downkey )
				})
			})
		});
		
		w.view.background_( Color(0.80392156862745, 0.75294117647059, 0.69019607843137, 1.0) );
		w.view.decorator = FlowLayout( w.view.bounds );
		w.onClose_({
			if( modal.notNil, { modal.close; modal=nil });
			r.stop;
		});
					
		searchedPresetList = presets.keys.asArray;			
		search = SCTextView( w, Rect( 0,0, 120, 20 ) )
			.keyUpAction_({
				| view |
				searchedPresetList = [];
				presets.keys.asArray.do({
					| key |
					if( view.string == "", {
						searchedPresetList = presets.keys.asArray;
					},{
						if( key.asString.containsi( view.string ), {
							searchedPresetList = searchedPresetList.add( key );
						})
					})
				});
				if( searchedPresetList.size == 1, {
					this.setPreset( searchedPresetList[0] );
					popup.value_(0);
				});
				popup.items_( searchedPresetList )
			 });
			
		popup = SCPopUpMenu( w, Rect( 0, 0, 120, 20 ) )
			.items_( searchedPresetList )
			.value_( presets.keys.asArray.indexOfEqual( currentPresetName ) )
			.action_({
				| menu |
				this.setPreset( searchedPresetList[ menu.value ] )
			});
		// Preset Buttons
		newButton = SCButton( w, Rect( 0,0, "new".bounds(font).width+6, 16) )
			.states_( [["new", Color.black, Color.blue.alpha_(0.2)]] )
			.font_(font)
			.action_({
				var text, okButton;
				if( modal.isNil, {
					modal = SCWindow( "new preset name...", Rect( 0, 0, 120, 50).moveToPoint( w.bounds.leftBottom + (newButton.bounds.leftTop*(1@(-1))) ) , border:false )
						.alwaysOnTop_( true );
					text = SCTextView( modal, Rect(5,5,100,14) )
							.string_("")
							.keyUpAction_({
								|a,b,c|
								if( b.ascii == 13, {
									okButton.doAction;
								})
							})
							.selectedString_("")
							.focus( true );
					okButton = SCButton( modal, Rect( 5, 25, 50, 14 ) )
						.action_({ this.newPreset( text.string.asString ); modal.close; modal=nil })
						.states_([[ "Ok", Color.black, Color.clear]])
						.font_( Font("Helvetica",9));
					SCButton( modal, Rect( 60, 25, 50, 14 ) )
						.action_({ modal.close; modal=nil })
						.states_([[ "Cancel", Color.black, Color.clear]])
						.font_( Font("Helvetica",9));
					modal.front;
				})
			});
		SCButton( w, Rect( 0,0, "delete".bounds(font).width+6, 16) )
			.states_( [["delete", Color.black, Color.blue.lighten(0.3).alpha_(0.2)]] )
			.font_(font)
			.action_({ this.deletePreset });
		SCButton( w, Rect( 0,0, "save".bounds(font).width+6, 16) )
			.states_( [["save", Color.black, Color.blue.lighten(0.3).alpha_(0.2)]] )
			.font_(font)
			.action_({ this.savePresets });
		SCButton( w, Rect( 0,0, "kill all notes".bounds(font).width+6, 16) )
			.states_( [["kill all notes", Color.black, Color.red.lighten(0.3).alpha_(0.2)]] )
			.font_(font)
			.action_({ this.freeAll });

		w.view.decorator.nextLine;

		// Position controller
		tabletColor = Color( 0.5, 0.5, 0.6).alpha_( 0.4 );
		tablet = SCTabletView( w, Rect( 0, 0, 300, 300 ) )
			.background_( tabletColor )
			.mouseDownAction_({
				|view, x,y, pres, ta, tb|
				currentlyPlaying=true;
				this.playNotes( queuedNotes );
				queuedNotes = [];
				group.set( \posX, x/tablet.bounds.width*2-1, \posY, y/tablet.bounds.height*2-1, 
					\vel, pres**4*128, \tiltA, ta, \tiltB, tb );
				tabletCursor.bounds_( tabletCursor.bounds.moveTo( 
					tablet.bounds.left+x, tablet.bounds.top+y ) );
			})
			.action_({
				|view, x,y, pres, ta, tb|
				group.set( \posX, x/tablet.bounds.width*2-1, \posY, y/tablet.bounds.height*2-1, 
					\vel, pres**4*128, \tiltA, ta, \tiltB, tb );
				tabletCursor.bounds_( tabletCursor.bounds.moveTo( 
					tablet.bounds.left+x, tablet.bounds.top+y ) );
			})
			.mouseUpAction_({
				| view |
				currentlyPlaying=false;
				
				//this.stopAll;
			});

		tabletCursor = SCStaticText( w, Rect( 0, 0, 8, 12))
			.bounds_( Rect( tablet.bounds.left+40, tablet.bounds.top+4, 8, 8) )
			.string_("X")
			.font_( Font("Helvetica", 6) );

		w.view.decorator.nextLine;			
		SCStaticText( w, Rect( 0,0,"release delay:".bounds(font).width, 24 ) )
			.string_("release delay:")
			.font_(font);
		this.draggableNumberBox( w, { |view| releaseDelay = view.value }, [0, 30].asSpec, 0 );
					
		//  Tabbed settings	
		v = TabbedView( w, Rect( 10,100,390, 270 ),
			[ "main", "vibrato", "oscillator", "env1", "env2", "lfo1", "lfo2", "lfo3", "veloFilter", "osc routing" ])
			.font_(Font("Helvetica", 9))
			.labelPadding_( 4 )
			.tabHeight_( 14 )	;

		
		// main
		v.views[0].flow({
			|w|
			this.makeControl( w, \amp );
			this.makeControl( w, \glide );
			this.makeControl( w, \fing );
			this.makeControl( w, \power );
			this.makeControl( w, \oct );
			this.makeControl( w, \fine );
				w.startRow;
			this.makeOutControl( w, \delayAmp );
			this.makeOutControl( w, \delayTime );
			this.makeOutControl( w, \delayFeedback );
			this.makeOutControl( w, \delayFilt );
			this.makeOutControl( w, \delayDist );
			this.makeOutControl( w, \rotate );
		});
		
		// vibrato
		v.views[1].flow({
			|w|
			this.makeControl( w, \vibRate );
			this.makeControl( w, \vibAmt );
			this.makeControl( w, \vibMod );
			this.makeControl( w, \vibSync );
		});
		
		// osc
		v.views[2].flow({
			|w|
			this.makeControl( w, \oscNoise );
				w.startRow;
			this.makeControl( w, \cdrive );
			this.makeControl( w, \creson );
			this.makeControl( w, \cup );
			this.makeControl( w, \cdown );
			this.makeControl( w, \cmin );
			this.makeControl( w, \cmax );
			this.makeControl( w, \cshift );
				w.startRow;
			this.makeControl( w, \driveMod );
			this.makeControl( w, \resonMod );
			this.makeControl( w, \upMod );
			this.makeControl( w, \downMod );
			this.makeControl( w, \minMod );
			this.makeControl( w, \maxMod );
			this.makeControl( w, \shiftMod );
			this.makeControl( w, \shiftFine );
		});

		// env1
		v.views[3].flow({
			|w|
			this.makeControl( w, \attack );
			this.makeControl( w, \decay );
			this.makeControl( w, \sustain );
			this.makeControl( w, \release );
				w.startRow;
			this.makeControl( w, \egTrack );
			this.makeControl( w, \egVel );
			this.makeControl( w, \egN );
		});

		// env2
		v.views[4].flow({
			|w|
			this.makeControl( w, \eg2Width );
			this.makeControl( w, \eg2Rate );
			this.makeControl( w, \eg2X );
			this.makeControl( w, \eg2Shift );
			this.makeControl( w, \eg2Cut );
			this.makeControl( w, \eg2Rep );
		});
		
		// lfo1
		v.views[5].flow({
			|w|
			this.makeControl( w, \lfo1Wave );
			this.makeControl( w, \lfo1Width );
			this.makeControl( w, \lfo1Phase );
			this.makeControl( w, \lfo1Rate );
			this.makeControl( w, \lfo1Sync );
		});
		v.views[6].flow({
			|w|
			this.makeControl( w, \lfo2Wave );
			this.makeControl( w, \lfo2Width );
			this.makeControl( w, \lfo2Phase );
			this.makeControl( w, \lfo2Rate );
			this.makeControl( w, \lfo2Sync );
		});
		v.views[7].flow({
			|w|
			this.makeControl( w, \lfo3Wave );
			this.makeControl( w, \lfo3Width );
			this.makeControl( w, \lfo3Phase );
			this.makeControl( w, \lfo3Rate );
			this.makeControl( w, \lfo3Sync );
		});
		
		// veloc
		v.views[8].flow({
			|w|
			this.makeControl( w, \vfCutoff );
			this.makeControl( w, \vfCutoffVel );
			this.makeControl( w, \vfReson );
			this.makeControl( w, \vfResonVel );
			this.makeControl( w, \vfRnd );
				w.startRow;
			this.makeControl( w, \vfDrive );
			this.makeControl( w, \vfUp );
			this.makeControl( w, \vfDown );
			this.makeControl( w, \vfX );
		});

		// routing
		v.views[9].flow({
			|w|
			this.makeRoutingSelector( w, \mVib );
				w.startRow;
			this.makeRoutingSelector( w, \mDrv );
				w.startRow;
			this.makeRoutingSelector( w, \mRes );
				w.startRow;
			this.makeRoutingSelector( w, \mUp );
				w.startRow;
			this.makeRoutingSelector( w, \mDown );
				w.startRow;
			this.makeRoutingSelector( w, \mMin );
				w.startRow;
			this.makeRoutingSelector( w, \mMax );
				w.startRow;
			this.makeRoutingSelector( w, \mSft );
		});
		
		^w.front
	}
	
	draggableNumberBox {
		| view, action, spec, init |
		var numberView, dragFrom;
		
		init = init ? spec.default;
		^numberView = SCNumberBox( view, Rect( 0, 0, 30, 24 ) )
			.font_( Font( "Helvetica", 9 ) )
			.stringColor_( Color.blue.lighten(0.35).alpha_(0.9) )
			.value_( init )
			.action_({ 
				|v|
				v.value_( spec.constrain(v.value) );
				action.value( v ) 
			})
			.mouseDownAction_({
				| view, inx, iny |
				dragFrom = inx;
			})
			.mouseMoveAction_({ 
				| view, inx, iny |
				view.value = spec.map( spec.unmap( view.value ) + ((inx-dragFrom)/500) );
				dragFrom = inx;
				action.value(view);
			})
	}
	
	// Methods to build the synth
	
	lfo {
		arg rate=64, bpm=120, width=0, gate=0, phase=0, type=0, mul=1, add=0;
		var freq, amp, tri, pulse;
		
		freq = ((480/bpm) * rate).reciprocal;
		type = (type*2).round;
		width = (width*0.5)+0.5;
		
		tri = VarSaw.kr( freq, phase, width );
		pulse = LFPulse.kr( freq, phase, width, 2, 0 );
	 	^((Select.kr( type, [tri, pulse] )*mul)+add)
	}
	
	eg {
			| gate, vel, bpmPower, bpp, bpm, a, d, s, r, tune, track, n |
			var newGate, regate, newA, newD, newS, newR, modBpp, env;
			modBpp = (3312/bpp) ;
	
			newGate = (( (gate-0.5)*vel*(gate>0))+((gate>0)*0.5)) * 
				(1 + (tune*0.007874*track));
				
			regate = gate * LFPulse.ar(  (bpmPower*0.1666666*(bpm/60)*n*2), 0.03, 0.99 ); 
		
			newA = 10.pow(    (modBpp * (a.dbamp-1)).ampdb / 20 )/1000;
			newD = 10.pow(    (modBpp * (d.dbamp-1)).ampdb / 20 )/1000;
			newS = s*s;
			newR = 10.pow(    (modBpp * (r.dbamp-1)).ampdb / 20 )/1000;
			
//			^EnvGen.kr( Env.adsr(newA, newD, newS, newR, 1, -2), regate>0, newGate )
			env = EnvGen.kr( 
				Env.new( [0, 1, newS, 0.0001, 0],
						[newA, newD, newR, 0.2], -2), regate>0, newGate, doneAction:2 );
			^env
	}
	
	eg2 {
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
	
	weedPitch {
		| pitch, gate, bpm, vibrato, vibAmt, vibMod, vibRate, vibSync, pitchBend, tune, fine, oct |
		var tuneOut, p, vib;
		
		vib = this.weedVib( bpm, vibrato, vibAmt, vibMod, vibRate, vibSync );
		tuneOut = pitch + pitchBend + tune + fine;
		p = tuneOut + (oct*12) + vib;
		^[tuneOut, p]
	}
	
	weedVib {
		| bpm, vib, amt, mod, rate, sync |
		var f, a, snc;
		f = (0.05 + rate) * (bpm/128) * 0.133333337;
		a = amt.pow(2) + (vib*mod*0.033);
		^SinOsc.ar( f, 0, a.max(0) )
	}
	
	weedOsc {
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
			this.weedControl( 
				max, cmax, maxMod,
		 	 	min, cmin, minMod,
		  		res, cres, resMod,
		  		up, cup, upMod,
		  		down, cdown, downMod,
		  		p,
		  		shift, cshift, fineShift, sftShift,
		   		drive, cdrive, driveMod );
	
		noiseSig = WhiteNoise.ar(  noise.pow(2)*0.5 );
		mirrorFeedback =  LocalIn.ar(1);

		pulseSig = this.mirror( if( inUgen.isNil, {
				this.weedPulse( (p.midicps+noiseSig)*noteArray, mirrorFeedback, upOut, downOut, a*outOut )}, { inUgen }),
			 -0.9, 0.9 );

		// find correst cres input to match reaktor?
		peakEqSig = MidEQ.ar( pulseSig, (shiftOut+shift2).midicps, 1.0-resOut, 60 );
		mirrorSig = this.mirror( peakEqSig, minOut.min(0), maxOut );
		
		LocalOut.ar( mirrorSig );
		^[peakEqSig, driveOut]
	}

	weedControl {
		| max, cmax, maxMod,
		  min, cmin, minMod,
		  res, cres, resMod,
		  up, cup, upMod,
		  down, cdown, downMod,
		  p,
		  shift, cshift, fineShift, sftShift,
		   drive, cdrive, driveMod |
		
		^[ 
			cmax + (max * maxMod * 0.0055),
			cmin + (min * minMod * 0.0055),
			((res * resMod * 0.00166667) + cres).min(0.999),
			((up * upMod * 0.3)+cup.pow(2)).max(1),
			((down * downMod * 0.3)+cdown.pow(2)).max(0),
			( (shift*cshift) + sftShift + (1+fineShift) + p ),
			( ( downMod*0.004*drive ) + (cdrive.pow(3)) ).max(-0.051),
			cdrive.pow(3)
		]
	}
	
	weedPulse {
		| freq, width, up, down, mul=1, add=0 |
		^Slew.ar( LFPulse.ar( freq, 0, (width*0.5)+0.5, 2, -1), up*2*freq, down*2*freq ) * mul
	}
	
	mirror {
		| sig, min=(-1), max=1 |
		var width, middle; 
		width = (0.5*(max-min));
		middle = min + width;
		^(sig-middle).fold2( width ) + middle
	}
	
	weedVeloFilt {
		| in, drv, cm, xm, 
		gate, 
		vfCutoff, vfCutoffVel, vfReson, vfResonVel, vfRnd, vfDrive, vfUp, vfDown, vfX |
		var fadeAmt, hpf, lpf, driven, freq, res,
			ctr, rand;
		
		driven = in * (
			( ((drv*0.6).neg)+1 ) * (vfDrive.pow(3)) );
			
		ctr = Slew.kr( gate, vfUp.pow(4), vfDown.pow(4) );
		
		rand = LFNoise2.kr( vfRnd, vfRnd*6 );
		
		fadeAmt = Lag3.kr( (vfX + xm), 0.002 );
	
		freq = (vfCutoff+(vfCutoffVel*ctr)+cm+rand).max(20).min(19000);
		res = (    ( (vfResonVel/60) * ctr ) + vfReson     ).min(0.98).max(0.01);
	
		hpf = RHPF.ar( driven, freq.midicps, res );
		lpf = RLPF.ar( driven, freq.midicps, res );
		
		^LeakDC.ar( (lpf*fadeAmt) + (hpf*(1-fadeAmt)) )
	}
	
	synthDef {
		^SynthDef( \weedwacker, {
			| 
				out=0, vel=64, amp=0.4, gate=1, bpm=120, notePitch=64, pitchbend=0,
				glide=0.5, fing=0, power=0, oct=0, tune=0, fine=0.0,
				vibRate=65, vibAmt=0.1, vibMod=0, vibSync=0,
				oscNoise=0, cdrive=0.55, driveMod=0, 
					creson=0.9, resonMod=0,
					cup=1, upMod=0,
					cdown=2, downMod=0,
					cmin=(-0.3), minMod=0,
					cmax=0.3, maxMod=0,
					cshift=0, shiftMod=0, shiftFine=0.0, 
				drive=0,
				attack=32, decay=32, sustain=0.5, release=32,
					egTrack=(-0.7), egVel=0.5, egN=0,
				eg2Width=0.01, eg2Rate=1, eg2X=0, eg2Shift=0, eg2Cut=0, eg2Rep=0,
				lfo1Wave=1, lfo1Width=0.5, lfo1Phase=0.0, lfo1Rate=65, lfo1Sync=1, 
				lfo2Wave=1, lfo2Width=0.5, lfo2Phase=0.0, lfo2Rate=65, lfo2Sync=1,		lfo3Wave=1, lfo3Width=0.5, lfo3Phase=0.0, lfo3Rate=65, lfo3Sync=1,
				vfCutoff=64, vfCutoffVel=0, vfReson=0.49, vfResonVel=0, vfRnd=3,
				vfDrive=0.75, vfUp=1.9, vfDown=1.9, vfX=0.5,
				mVib=1, mDrv=1, mRes=1, mUp=1, mDown=1, mMin=1, mMax=1, mSft=1,
				posX=0, posY=0, tiltA=0, tiltB=0, endIn=0
			|
			var 	bpp, bpmPower,
				lfo1, lfo2, lfo3, eg,
				eg2sftOut, eg2cutOut, eg2xOut,
				min, max, sft, drv, up, down, res, vib,
				pitchTune, pitchP,
				osc, w,x,y, p;
		
			bpmPower = power+3;
			bpp = (bpmPower*(2.ampdb)).dbamp*bpm;
			
			pitchTune=LocalIn.kr(1);
			gate = gate*(vel/128.0);
			gate = gate*(endIn<=0 + Trig1.kr( endIn, endIn ));
			
			tiltA = Lag3.kr( tiltA, 0.04 );
			tiltB = Lag3.kr( tiltB, 0.04 );
		
			// LFO1
			lfo1 = this.lfo( lfo1Rate, bpp, lfo1Width, gate, (tiltA*tiltB+lfo1Phase), lfo1Wave,1,1 );
			// LFO2
			lfo2 = this.lfo( lfo2Rate, bpp, lfo2Width, gate, (tiltA*tiltB+lfo2Phase), lfo2Wave,1,1 );
			// LFO2
			lfo3 = this.lfo( lfo3Rate, bpp, lfo3Width, gate, (tiltA*tiltB+lfo3Phase), lfo3Wave,1,1 );
			
			// Eg
			eg = this.eg( gate, egVel, bpmPower, bpp, bpm, attack, decay, sustain, release, pitchTune, egTrack, egN );
				
			// Matrix
			vib = Select.kr( mVib, [lfo3, lfo2, lfo1, eg] );
			drv = Select.kr( mDrv, [lfo3, lfo2, lfo1, eg] );
			res = Select.kr( mRes, [lfo3, lfo2, lfo1, eg] );
			up = Select.kr( mUp, [lfo3, lfo2, lfo1, eg] );
			down = Select.kr( mDown, [lfo3, lfo2, lfo1, eg] );
			min = Select.kr( mMin, [lfo3, lfo2, lfo1, eg] );
			max = Select.kr( mMax, [lfo3, lfo2, lfo1, eg] );	
			sft = Select.kr( mSft, [lfo3, lfo2, lfo1, eg] );
			
			//Eg2
			# eg2cutOut, eg2xOut, eg2sftOut = 
				this.eg2( bpp, eg2Rate, eg2Width, gate, eg2Rep, eg2Cut, eg2X, eg2Shift );
			
			//Pitch
			# pitchTune, pitchP =
				this.weedPitch( notePitch, gate, bpm, vib, vibAmt, vibMod, vibRate, vibSync, pitchbend, tune, fine, oct );
			
			// OSC
			#osc, drive = this.weedOsc( eg, 
				max, cmax, maxMod,
				min, cmin, minMod,
				res, creson, resonMod,
				up, cup, upMod,
				down, cdown, downMod, 
				pitchP,
				sft, cshift, shiftFine, shiftMod,
				drv, cdrive, driveMod,
				oscNoise, eg2sftOut
				);
			
			osc = this.weedVeloFilt( osc, drive, eg2cutOut, eg2xOut,  
				gate, 
				vfCutoff, vfCutoffVel, vfReson, vfResonVel, vfRnd, vfDrive, vfUp, vfDown, vfX );
				
			LocalOut.kr( pitchTune );
			osc = LeakDC.ar( Compander.ar( osc, osc, thresh: 0.95, slopeBelow: 1, slopeAbove: 0.1, clampTime: 0.01, relaxTime: 0.01 ) );
				
			posX = Lag3.kr( Gate.kr( posX, gate ), 0.4, 0.4 );
			posY = Lag3.kr( Gate.kr( posY, gate ), 0.4, 0.4 );				p = Point( posX.neg, posY ).asPolar;
			# w,x,y = PanB2.ar( osc, p.theta/pi+0.5, 1 - (p.rho.pow(2)/10) );			//Out.ar( out, B2UHJ.ar( w,x,y)*amp );
			Out.ar( out, [w,x,y]*amp );
		})
	}
	
	stereoOutSynthDef {
		^SynthDef( \stereoOut, {
			| out=0, in, delayAmp=0.2, delayTime=0.5, delayFeedback=3, delayFilt=0, delayDist=0.0, 
				rotate=0 |
			var inSig, del, w, x, y;
			delayDist = delayDist+0.1;
			del = DelayC.ar( LocalIn.ar( 3 ).min(1).max(-1), 5, delayTime );
			del = LeakDC.ar(
				(delayDist*LPF.ar( del, 100+((1-delayFilt)*18000), 0.5**(1/(delayFeedback/delayTime)) ) ).tanh / delayDist );
			inSig = In.ar( in, 3 );
			#w, x, y = del;
			# x, y = Rotate2.ar( x, y, rotate );
			LocalOut.ar( [w,x,y] + inSig);
			#w, x, y = ([w,x,y]*delayAmp) + inSig;
			Out.ar( out, B2UHJ.ar(w,x,y) );
		})
	}

	quadOutSynthDef {
		^SynthDef( \quadOut, {
			| out=0, in, delayAmp=0.2, delayTime=0.5, delayFeedback=3, delayFilt=0, delayDist=0.0, 
				rotate=0 |
			var inSig, del, w, x, y;
			delayDist = delayDist+0.1;
			del = DelayC.ar( LocalIn.ar( 3 ).min(1).max(-1), 5, delayTime );
			del = LeakDC.ar(
				(delayDist*LPF.ar( del, 100+((1-delayFilt)*18000), 0.5**(1/(delayFeedback/delayTime)) ) ).tanh / delayDist );
			inSig = In.ar( in, 3 );
			#w, x, y = del;
			# x, y = Rotate2.ar( x, y, rotate );
			LocalOut.ar( [w,x,y] + inSig);
			#w, x, y = ([w,x,y]*delayAmp) + inSig;
			Out.ar( out, DecodeB2.ar(4, w, x, y, 0.5) );
		})
	}
	
	makeControlSpecs {
		[ 
			\delayAmp ->	[ -60.dbamp, 0.dbamp, 'exp', 0, 0.4],
			\delayTime -> [ 0.001, 5, 'exp', 0, 0.4 ],
			\delayFeedback -> [ 0, 10, 'lin', 0, 3 ],
			\delayFilt -> [0, 1, 'lin', 0, 0 ],
			\delayDist ->	[ 0, 35, 'lin', 0, 0 ],
			\rotate -> [-0.4, 0.4, 'lin', 0, 0 ],
			\amp ->		[-60.dbamp,12.dbamp,'exp',0, 0.4],

			\glide -> 	[0,1],
			\fing -> 		[0,1],
			\power -> 	[-3,3,'lin',1],
			\oct -> 		[-5, 5],
			\fine -> 		[0,1],

			\vibRate -> 	[1,128],
			\vibAmt -> 	[0,2],
			\vibMod -> 	[-60,60],
			\vibSync ->	[0,2],

			\oscNoise ->	[0,50],

			\cdrive ->	[0.1,1],
			\driveMod ->	[-60,60],

			\creson -> 	[0.9, 0.999],
			\resonMod -> 	[-60,60],

			\cup -> 		[1,4],
			\upMod -> 	[-60,60],

			\cdown -> 	[1,4],
			\downMod -> 	[-60,60],

			\cmin -> 		[-0.3, 0],
			\minMod -> 	[-60,60],

			\cmax -> 		[0,0.3],
			\maxMod -> 	[-60,60],

			\cshift -> 	[-36,60],
			\shiftMod -> 	[-60,60],
			\shiftFine -> [0,1],

			\attack -> 	[0,63.5],
			\decay -> 	[0,63.5],
			\sustain -> 	[0,1],
			\release -> 	[0,63.5],

			\egTrack -> 	[0, -1.4],
			\egVel -> 	[0,1],
			\egN -> 		[0,4,'lin',0.25],

			\eg2Width -> 	[-1,1],
			\eg2Rate -> 	[1,128],
			\eg2X -> 		[-60,60],
			\eg2Shift -> 	[-60,60],
			\eg2Cut -> 	[-60,60],
			\eg2Rep -> 	[0,1],

			\lfo1Wave -> 	[0,2],
			\lfo1Width -> [-1,1],
			\lfo1Phase -> [0,1],
			\lfo1Rate -> 	[1,128],
			\lfo1Sync -> 	[0,1],

			\lfo2Wave -> 	[0,2],
			\lfo2Width -> [-1,1],
			\lfo2Phase -> [0,1],
			\lfo2Rate -> 	[0,128],
			\lfo2Sync -> 	[0,1],

			\lfo3Wave -> 	[0,2],
			\lfo3Width -> [-1,1],
			\lfo3Phase -> [0,1],
			\lfo3Rate -> 	[1,128],
			\lfo3Sync -> 	[0,1],

			\vfCutoff -> 	[0,127],
			\vfCutoffVel -> [-64, 64],
			\vfReson -> 	[0,0.98],
			\vfResonVel -> [-64, 64],
			\vfRnd -> 	[0,6],
			\vfDrive -> 	[0,1.5],
			\vfUp -> 		[3.5, 0.4],
			\vfDown -> 	[3.5, 0.4],
			\vfX -> 		[0,1],

			\mVib -> 		[0,4],
			\mDrv -> 		[0,4],
			\mRes -> 		[0,4],
			\mUp -> 		[0,4],
			\mDown -> 	[0,4],
			\mMin -> 		[0,4],
			\mMax -> 		[0,4],
			\mSft -> 		[0,4]
		].do({ | assoc | controlSpecs[assoc.key] = assoc.value })
	}
}
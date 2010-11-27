AudioProxyGui : ProxyGui {
	var play, volSlider, volText;
	
	var changingVolume=false;
	var lastPlay, lastPower, lastVol;
	
	*new {
		| node, nodename, parent, controller |
		^super.new( node, nodename, parent, controller ).init;
	}
	
	init {
		origin = origin ? (0@0);
		pBounds = Rect(0,0, 400, 22).moveTo( origin.x, origin.y );
		this.initColors;
		defaultFont = Font("LucidaSans", 9);
		boldFont = Font("LucidaSans-Demi", 9);
		
		this.makeBackground;
		this.makeTitle;
		this.makePower;
		this.makePlay;
		this.makeVolume;
		//this.makeScope;
		this.makeEditButton;
		background.bounds_( background.bounds.width_(xPos) );
		this.refresh;
	}
	
	bounds {
		^background.bounds
	}
	
	moveTo {
		| x,y |
		background.bounds_( background.bounds.moveTo( x, y ) );
	}
	
	isPowered {
		^node.group.notNil.if({
			node.group.isRunning
		},{
			false
		})
	}
	
	isPlaying {
		^(node.monitor.notNil and: {node.monitor.group.notNil})
	}
	
	makePower {
			power = 
				SCUserView( background, Rect( xPos, 0, pBounds.height, pBounds.height )
					.insetBy( 1, 1 ) );
			power.relativeOrigin_(true)
				.canFocus_(false)
				.drawFunc_({
					lightBorder.set;
					Pen.strokeRect( Rect.new( 0, 0, power.bounds.width, power.bounds.height  ));
					if( this.isPowered, {powerOn.setFill}, {powerOff.setFill } );
					Pen.fillRect( power.bounds.moveTo(0,0).insetBy(2.5,2.5) );
				})
				.mouseDownAction_({ |view| 
					this.isPowered.if({
						node.free;
						// @todo: grey out some items
					},{
						node.wakeUp;
					});
					//view.refresh;
				})
				.mouseUpAction_({ |v| { v.refresh }.defer( node.fadeTime+0.25 ) });

				
		xPos = xPos + power.bounds.width + space; 
	}
	
	makePlay {
		play = 
			SCUserView.new(background,Rect(xPos, 0, pBounds.height, pBounds.height)
				.insetBy(1,1))
			.relativeOrigin_(true)
			.canFocus_(false)
			.mouseDownAction_({
				|view|
				if( this.isPlaying, { node.stop }, { node.play });
			})
			.mouseUpAction_({ |v| 
				{v.refresh; power.refresh; }.defer(node.fadeTime+0.05) })
			.drawFunc_({
				|view|
				var origin;
				origin = play.bounds.origin;
				lightBorder.set;
				Pen.strokeRect( Rect(0,0, play.bounds.width, play.bounds.height) );
				Pen.push;
					Pen.translate( 6, 10 );
					if( this.isPlaying, {
							Pen.strokeColor_( powerOn );
							Pen.addArc( (0@0), 8, 7*pi/4, 2*pi/4 );
							Pen.stroke;
							Pen.addArc( (0@0), 5, 7*pi/4, 2*pi/4 );
							Pen.stroke;
						
					},{
						Pen.line(  3@0, 8@0  );
						Pen.stroke;
					});
					Pen.addArc( (0@0), 1, 7*pi/4, 2*pi/4 );
					Pen.stroke;
				Pen.pop;
			});
		xPos = xPos + play.bounds.width + space + 2; 
	}
	
	makeVolume {
		volSlider = 
			SCSlider( background, Rect( xPos, 10, 60, pBounds.height-11 ) )
				.canFocus_(false)
				.thumbSize_( pBounds.height-13 )
				.action_({
					|view|
					node.server.sendBundle( nil, [15, node.group.nodeID, "amp", view.value ]);
					volText.string_( "volume: " ++ view.value.round(0.01).asString[0..5] );
					changingVolume = true;
				})
				.mouseUpAction_({
					|view|
					// On mouseUp, when dragging is finished, set the amp using NodeProxy interface, 
					//  so nodemap and whatnot get set correctly.
					node.set( \amp, view.value );
					changingVolume = false;
				});
		volText = SCStaticText( background, Rect( xPos, -2, 60, pBounds.height-10 ) )
			.string_( "volume: " )
			.stringColor_( lightGrey )
			.font_( Font( "LucidaSans", 9 ) );

		xPos = xPos + volSlider.bounds.width + space + 3; 	
	}
		
	makeScope {
		var buffer, synth, scope;
		buffer = Buffer.alloc( Server.default, 512, node.bus.numChannels );
		synth = SynthDef("stethoscope", { arg in, switch, bufnum;
				var z;
				z = In.ar(in, node.bus.numChannels); 
				ScopeOut.ar(z, bufnum);
			}).play(RootNode(Server.default), [\bufnum, buffer.bufnum, \in, node.index], \addToTail);
		synth.isPlaying = true;
		NodeWatcher.register(synth);
		
		SCScope( background, Rect(xPos, 1, pBounds.height-2, pBounds.height-2) )
			.background_( Color.white )
			.waveColors_( [ Color.black ] );

		xPos = xPos + pBounds.height + space; 	
	}
	
	refresh {
		var plState, pwState, volState;
		
		//"np.refresh".postln;

		plState = this.isPlaying;
		if( (plState == lastPlay).not, {
			lastPlay = plState;
			play.refresh;
		});
	
		pwState = this.isPowered;
		if( (pwState == lastPower).not, {
			lastPower = pwState;
			play.refresh;
		});
		
		volState = node.getValueOf( \amp ) ? 1;
		if( (volState == lastVol).not, {
			lastVol = volState;
			if( volState.class == NodeProxy, {
				volSlider.background_( lightGrey );
				volSlider.enabled_(false);
				volText.string_( "volume: *".postln );
			},{
				volSlider.background_( Color.clear );
				volSlider.enabled_(true);
				volSlider.value_( lastVol );
				volText.string_( "volume: " ++ lastVol.round(0.01).asString[0..5] );
			})
		})
	}
}

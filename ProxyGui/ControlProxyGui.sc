ControlProxyGui : ProxyGui {
	var textDisplay;
	var xPos = 0, space=3, pBounds;
	var lightGrey, darkGrey, lightBorder, transGrey, powerOn, powerOff;	
	var changingVolume=false;
	var lastPower, lastObj;
	
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
		this.makeTextDisplay;
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
		^node.group.isRunning
	}
	
	source {
		^node.source
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
	
	makeScope {
		var buffer, synth, scope;
		buffer = Buffer.alloc( Server.default, 512, node.bus.numChannels );
		synth = SynthDef("stethoscope", { arg in, switch, bufnum;
				var z;
				z = In.ar(in, node.bus.numChannels); 
				ScopeOut.ar(z, bufnum);
			}).play(RootNode(Server.default), [\bufnum, buffer.bufnum, \in, node.index, \switch, 0], \addToTail);
		synth.isPlaying = true;
		NodeWatcher.register(synth);
		
		SCScope( background, Rect(xPos, 1, pBounds.height-2, pBounds.height-2) )
			.background_( Color.white )
			.waveColors_( [ Color.black ] );

		xPos = xPos + pBounds.height + space; 	
	}
	
	makeTextDisplay {
		textDisplay = SCDragBoth( background, Rect( xPos, 1, 88, pBounds.height-2 ))
			.string_( "..." )
			.stringColor_( lightGrey )
			.font_( defaultFont )
			.align_(\center)
			// Drag stuff
			.beginDragAction_({
				node.source
			})			
			.receiveDragHandler_({
				| view |
				parent.proxyspace.use({
					node.source = view.class.currentDrag.postln;
					view.string = node.source.asString;
				});
			});
		
		xPos = xPos + textDisplay.bounds.width + space; 
	}

	refresh {
		var pwState, objState;
		
		//"np.refresh".postln;

		pwState = this.isPowered;
		if( (pwState == lastPower).not, {
			lastPower = pwState;
		});
		
		textDisplay.string = this.source.asString;
	}
}

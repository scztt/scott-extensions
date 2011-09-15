/*
	SynthDef( \line, { 
		arg out=0, target=0.7, fadeTime=10;
		var time, gate=0, cur, distance, last, sig, phase;
		gate = Slope.kr( target ).abs;
		cur = In.kr( out, 1 );
		cur = Latch.kr( cur, gate );
		phase =  EnvGen.kr( Env( [0,1], [1] ), gate, target-cur, cur, fadeTime, doneAction: 1 );
		Out.kr( out, phase );
	}).send(s).add;
*/

ControlNode {
	classvar controllerCount=0, bundleTemp;
	var server, <name, <spec, fadeSlope, <controller, <bus;
	var <>states, currentState, mappedTo, setMsgRaw, runMsgRaw, synth, fadeSlope, <>defaultFadeTime, updateRoutine;
	var envTime, envVal, uiRoutine, synthRoutine, clock;
	var parentPage, <color;
	var updating=false, nameBox, valueBox, scaleBox, scopeButton, tenvButton, sliderButton, envControl, mappingSelector;
	
	*new {
		arg server, name, spec, fadeSlope=\linear, controller=nil, bus, mapTo=[];
		^super.newCopyArgs( server ? Server.default, name, spec, fadeSlope, controller, bus, mapTo ).init;
	}
	
	*randomColor {
		^Color.hsv( 1.0.rand, 0.3 + 0.2.rand, 0.4+0.3.rand,  0.5)
	}
	
	init {
		name = name ? ("p" ++ controllerCount);
		spec = spec ? [0,1,\linear,0];
		spec = spec.asSpec;
		bus = bus ? Bus.control( server, 1 );
		mappedTo = List(0);
		defaultFadeTime = 1;
		
		synth = Synth.new( "line", [\out, bus.index, \bus, bus.index, \target, 0, \fadeTime, 0.01], server );
		
		//mapTo.do({ 
		//	arg node;
		//	node.map( this.bus );

		setMsgRaw = bus.setMsg( 0 );
		setMsgRaw.pop;
		runMsgRaw = [12, synth.nodeID , 1];
		
		states = Dictionary.new;
		
		controllerCount = controllerCount + 1;
		color = this.class.randomColor;
	}
	
	set {
		arg val, time=defaultFadeTime;
		server.sendBundle( nil, runMsgRaw, 
			[15, synth.nodeID, "out", bus.index, "target", val, "fadeTime", time] );
	}
	
	setMsg {
		arg val, time=defaultFadeTime;
		^[ runMsgRaw,
			[15, synth.nodeID, "out", bus.index, "target", val, "fadeTime", time] ];
	}
	
	get {
		arg func;
		bus.get( func );
	}
	
	moveToState {
		arg state, time;
		var val;
		val = states[state];
		val.isNil.not.if({
			set( val );
		});
		^val;
	}
	
	moveToStateMsg {
		arg state, time;
		var val;
		val = states[state];
		^val.isNil.not.if({
			setMsgRaw( val );
		},{ nil });
	}
	
	saveState {
		arg name;
		bus.get( { arg n; states.put( name, n ) } ); 
	}
	
	clearStates {
		states = Dictionary.new;
	}
	
	// name, value, lin/exp/sin, currentPreset, presets, gui stuff
	makeControlPanel {
		| page |
		parentPage = page;
		parentPage.startRow;
		page = FlowView( page, page.layRight( 800,100 ) );
		
		nameBox = SCDragSink( page, page.layRight( 80, 20 ) )
			.string_( name )
			.stringColor_( Color.gray )
			.align_( \right )
			.mouseDownAction_({
				| view |
				this.updateToggle;
			});
		valueBox = this.makeNumberBox( page, page.layRight( 60, 20 ) );
		scaleBox = SCStaticText( page, page.layRight( 60, 20 ) )
			.string_( fadeSlope.asString );
		scopeButton = SCButton( page, page.layRight( 20, 20 ) )
			.states_( [[ "S", Color.black, Color.grey ]])
			.action_({ this.scope });
		tenvButton = SCButton( page, page.layRight( 20, 20 ) )
			.states_( [
				["E", Color.black, Color.grey],
				["E", Color.grey, Color.black] ] )
			.value_( 0 )
			.action_({
				| b |
				(b.value == 1).if({
					envControl = this.makeControlEnvelope( page );
					page.resizeToFit;
					parentPage.reflowAll.resizeToFit;
				},{
					envControl.remove; envControl = nil;
					page.bounds_( page.bounds.height_(1) ).resizeToFit;
					parentPage.reflowAll.resizeToFit;
				});
			});
		mappingSelector = SCDragSource( page, page.layRight( 25, 20 ) )
			.string_("map")
			.align_(\center)
			.font_(Font("Helvetica", 10) )
			.beginDragAction_({
				this
			});
		
		page.background_(color);
		page.resizeToFit;
		^page;
	}
		
	makeNumberBox {
		| window, rect |
		var box;
		box = SCNumberBox( window, rect )
			.keyDownAction_({ |a,b,c| updateRoutine.stop; defaultKeyDownAction(a,b,c) })
			.action_({ |b| this.set(b.value); updateRoutine.reset.play; })
			.onClose_({ updateRoutine.stop; updateRoutine=nil})
			.stringColor_( Color.gray );
		updateRoutine = Routine({ 
			{
				this.get({ | val | {box.value_(val)}.defer(0) });
				0.1.yield;
			}.loop }).stop;
		this.get({ |val| {box.value_(val)}.defer(0) });
		^box;
	}
	
	makeControlEnvelope {
		| page |
		var buttons, now=0, maxValue, minValue, maxT=100, v, t, vRt, tRt, a, b, n, r, warp, synth;

		envVal = envVal ? [ 0.5, 0.5 ];
		envTime = envTime ? [ 0, 9999];
		
		if( envVal.isNil || envTime.isNil, {
			this.get({ 
				|val|
				val = spec.unmap( val );
				envVal = [ val, val ];
				envTime = [0, 9999];
			});
		});

		vRt = envVal; tRt = envTime;	
		
		//a = PageLayout.new( "envelope", 750 );
		a = FlowView( page, page.layRight( 750, 100 ) );
		a.onClose_({ uiRoutine.stop; });
		
		b = SCEnvelopeView(a, a.layRight(600, 80))
			.drawLines_(true)
			.fillColor_(Color.white)
			.drawRects_(true)
			.value_([envTime/maxT , envVal]);
		
		b.mouseDownAction = { 
			arg a,x,y; 
			var newT, newV, nowIndex, newIndex;
			newT = clock.beats + ( ( (x-b.bounds.left) / b.bounds.width)**2 * maxT);
			newV = (b.bounds.height - y + b.bounds.top) / b.bounds.height;
			nowIndex = envTime.indexInBetween( clock.beats );
			newIndex = envTime.indexInBetween( newT );
			
			newIndex = newIndex.ceil.asInt;
			if( newT < envTime[nowIndex.ceil.asInt] , { 
				envVal = envVal.insert (newIndex, newV);
				envTime = envTime.insert (newIndex, newT);
				envVal[envVal.size-1] = envVal[envVal.size-2];
				synthRoutine.stop.reset; clock.clear; synthRoutine.play(clock,0); uiRoutine.play(clock,0);
			}, {
				envVal = envVal.insert (newIndex, newV);
				envTime = envTime.insert (newIndex, newT);
				envVal[envVal.size-1] = envVal[envVal.size-2];	
			});
		};
		
		buttons = FlowView( a, a.layRight( 70, 50 ) );
		maxValue = SCNumberBox( buttons, buttons.layRight(70, 20 ) )
			.value_(spec.maxval)
			.action_( { |n| spec.maxval_( n.value ); } ); 
		
		minValue = SCNumberBox( buttons, buttons.layRight(70, 20 ) )
			.value_(spec.minval)
			.action_( { |n| spec.minval_( n.value ); } ); 
			
		a.resizeToFit;
		b.resize_(1);
		
		// r
		uiRoutine = Routine({ arg inval;
			var vNow, nowIndex, tNow, curV=0, curT=0;
			curV = envVal[0]; curT = envTime[0];
			loop {
				now = clock.beats;
				nowIndex = envTime.indexInBetween( now );
		
				vNow = [envVal.blendAt( nowIndex )] ++ envVal.copyRange( nowIndex.ceil.asInt, 9999);
				tNow = [envTime.blendAt( nowIndex )] ++ envTime.copyRange( nowIndex.ceil.asInt, 9999);
					tNow = tNow/maxT;
					tNow = tNow - tNow.first + 0.000000001;				{
					b.thumbSize_( 2 );
					tNow.size.do({
						arg i;
						b.value_( [ tNow**0.5 , vNow] );
					});
				}.defer;
				0.1.yield;		
			}
		});
		
		
		clock = clock ? TempoClock.new;
		
		// n
		if(synthRoutine.isNil or: {synthRoutine.isPlaying.not}, {
			synthRoutine = Routine ({ arg inval;	
				var now, time, index;
				now = clock.beats;
				index = envTime.indexInBetween( now ).ceil.asInt;
				time = envTime[index] - now;
				this.set( spec.map(envVal[index]), time );
				time.yield;
				
				loop {
					time = envTime[index+1] - envTime[index];
					this.set( spec.map(envVal[index+1]),  time );
					index = index + 1;
					time.yield;
				}
			});
			synthRoutine.play(clock);		
		});
		
		uiRoutine.beats_(TempoClock.beats);
		uiRoutine.play(clock);
		^a;
	}
	
	updateToggle {
		this.update( updating.not );
	}
	
	update {
		| u |
		u.not.if({
			updating = false;
			updateRoutine.stop;
			valueBox.stringColor_(Color.gray);
			nameBox.stringColor_( Color.gray );
		},{
			updating = true;
			updateRoutine.reset.play;
			valueBox.stringColor_(Color.black);
			nameBox.stringColor_( Color.black );
		})
	}
	
	randomize {
		| time=nil |
		this.set( spec.map( 1.0.rand ), time ? defaultFadeTime );
	}
	
	busPlug {
		^BusPlug.for( this.bus );
	}
	
	scope {
		bus.debugScope("",[this.name]);
	}
}

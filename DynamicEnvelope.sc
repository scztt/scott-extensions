DynamicEnvelope {
	classvar <allEnvelopes, defaultEnvelope,
		<clock, <clockBus, <>server,
		gui, <duration=4000;
	
	var <buffer, <bus, <synth, <envData,
		<duration=4000, <min=0, <max=1, server,
		view;
		
	*initClass {
		allEnvelopes = [];
		server = Server.default;
	}
	
	*initClock {
		clockBus = Bus.control( server, 1 );
		SynthDef( \dEnvClock, {
			| out=0, t_reset |
			var t = Sweep.kr( t_reset, 1);
			SharedOut.kr( out, t );
			Out.kr( out, t );
		}).play(server.defaultGroup,  [\out, clockBus.index] );
	}
	
	*getCurrentTime {
		^server.getSharedControl( this.clockBus.index );
	}
	
	*new {
		| initial, buffer, bus |
		var env;
		env = super.new.init( initial, buffer, bus );
		this.addEnvelope( env );
		^env
	}
	
	*gui {
		^(gui ? this.createGui)
	}
	
	*createGui {
		gui = DynamicEnvelopeWindow.new;
			gui.addEnvelopeAction = { this.new };
			gui.removeEnvelopeAction = { | env | this.removeEnvelope( env ) };
		allEnvelopes.do({ |env| gui.addEnvelope(env) });
		gui.container.refreshPositions();
		^gui
	}
	
	*addEnvelope {
		| env, addToGui=true |
		format( "Add it bitch! (%)", this).postln;
		allEnvelopes.postln;
		allEnvelopes =  allEnvelopes.add( env );
		allEnvelopes.postln;
		if( addToGui && gui.notNil, {  gui.addEnvelope( env )   });
	}
	
	*showEnvelope {
		| env |
		gui.addEnvelope( env )
	}
	
	*hideEnvelope {
		| env |
		gui.removeEnvelope( env )
	}
	
	*removeEnvelope {
		| env |
		gui.removeEnvelope( env );
		allEnvelopes.remove( env );
			// kill synths, dealloc busses, etc.
	}
	
	init {
		| initial, buf, busIn |
		server = this.class.server;
		
		bus = busIn ? Bus.control( server, 1 );
		
		if( clock.isNil, { this.class.initClock });

		envData = initial ? [[-0.001,0.1,0.2,0.3,1.0],[0.0,1.0,0.0,1.0,1.0]];
		Routine({
			if( buf.isNil, {
				buffer = Buffer.alloc( server, 2000, 1 );
				this.sendArrayToBuffer();
			} , {
				buffer = buf;
				this.getArrayFromBuffer();
			});		
			
			this.class.makeSynthDefs();
			server.sync;
			this.startSynth();
		}).play;
	}
	
	*makeSynthDefs {
		SynthDef( \dynEnvelope, {
			| out=0,  t_reset=0, t_position=1, t_resetFromHere=0, clockIn, min, max, buffer, curve=2 |
			var clock, reset=1;
			var position, im;
			var startLevel, endLevel, currentNodeTime, nextNodeTime;
			var level, lastLevel;
			
			//clock = Sweep.kr( t_reset, 1);
			clock = In.kr( clockIn );
			min = Lag.kr( min, 1 );
			max = Lag.kr( max, 1 );
		
			im = Impulse.kr(0.2) + t_resetFromHere;
		
			#reset, lastLevel = LocalIn.kr( 2 );
			reset = reset + t_reset;
				
			position = Demand.kr( reset+t_position, t_reset+t_position, Dseries( t_position*2, 2, inf ));
				
			startLevel = Demand.kr( reset + t_resetFromHere, 0, lastLevel*(t_reset>0).not );
			endLevel = Demand.kr( reset + t_resetFromHere, 0, Dbufrd( buffer, position ) );
			currentNodeTime = Demand.kr( reset+t_resetFromHere, t_resetFromHere, 
				Dswitch1( [ 
					if( position>0, Dbufrd( buffer, position-1), 0 ),
					clock
				], t_resetFromHere>0 ) );
			nextNodeTime = Demand.kr( reset+t_resetFromHere, 0, Dbufrd( buffer, position+1 ) );
			endLevel = if( nextNodeTime>0, endLevel, startLevel );
			nextNodeTime = if( nextNodeTime>0, nextNodeTime, 99999 );
			
			level = (( clock-currentNodeTime).max(0) / (nextNodeTime-currentNodeTime));
			level = Select.kr( curve,  [ level,
							(((level*pi)-pi).cos/2)+0.5,
							((level*2.3978952727984).exp-1)/10 ]);

			level = startLevel + ( level * (endLevel-startLevel) );
									
			LocalOut.kr( [(clock>nextNodeTime), level] );
			SharedOut.kr( 0, level  );
			Out.kr( out, min + (level*(max-min)) );
		}).send(server);
		
		SynthDef( \deClock, {
			| out=0, sharedOut, t_reset=0 |
			var t;
			t = Sweep.kr( t_reset, 1 );
			SharedOut.kr( sharedOut, t );
			Out.kr( out, t );
		}).send( server );
	}
	
	envData_ {
		| newData |
		envData = newData;
		this.sendArrayToBuffer;
	}
	
	sendArrayToBuffer {
		buffer.sendCollection( [envData[1], envData[0]*duration].flop.flatten );
		[envData[1], envData[0]*duration].flop.flatten.postln;
		synth.set( \t_resetFromHere, 1 );
	}
	
	getArrayFromBuffer {
		var data;
		data = buffer.getn( 0, buffer.numFrames, action:{ 
			|data|  
			envData = data.clump(2).flop;
			envData = [ envData[1], envData[0]/duration ];
			envData.postln;
		  });
	}
	
	addPointAt {
		| x, y |
		var insIndex = 0;
		while( { (envData[0][insIndex] < x) && (insIndex < envData[0].size) }, 
			{ insIndex = insIndex+1 });
		envData[0].insert( insIndex, x );
		envData[1].insert( insIndex, y );
//		envData[i] = v.value.collect( _.asList );
		^envData
	}
	
	startClock {
		if( clock.isNil, {
			clock = Synth.new( \deClock );
		}, {
			clock.run = true;
		})
	}

	startSynth {
		if( synth.isNil, {			
			synth = Synth.after( this.class.clock, \dynEnvelope, 
				[\out, bus.index, \clockIn, this.class.clockBus.index, \min, min, \max, max,
				\buffer, buffer.bufnum, \t_reset, 1] );	
			
		}, {
			synth.run = true;
		})
	}
	
	pauseSynth {
		if( synth.notNil, { synth.run = false });	
	}
	
	pauseClock {
		if( clock.notNil, { clock.run = false });	
	}
	
	freeSynth {
		synth.free;
		synth = nil;
	}
	
	min_ {
		| val |
		synth.set( \min, min = val );
	}
	
	max_ {
		| val |
		synth.set( \max, max = val );
	}
}

DynamicEnvelopeWindow {
	classvar <>envelopeWidth = 20000;
	var envelopes, <envelopeViews,
		<window, <container, cti, scrollView,
		scrolling=true, zoomLevel=0, position=0, <envelopeWidth,
		updateRoutine, <>addEnvelopeAction, <>removeEnvelopeAction;
	
	*new {
		^super.new.init;	
	}
	
	init {
		envelopes = List.new;
		envelopeViews = List.new;

		this.initWindow();
		scrollView = SCScrollView( window, Rect(5,5,window.bounds.width-10, window.bounds.height-10) );
			scrollView.resize = 5;
		container = VerticalStackedView( scrollView, 5@5, header:false );
		cti = SCUserView( scrollView, Rect(0,0,3,window.bounds.height) )
			.relativeOrigin_(true)
			.drawFunc_({ 
				Color.red(1,0.3).set; 
				Pen.fillRect(Rect(0, 0, 3, window.bounds.height)); 
			});
		this.initRoutine();
		window.front;
	}
	
	initWindow {
		envelopeWidth = this.class.envelopeWidth;
		window = SCWindow.new("enveloped!", Rect(100,100,700,400), scroll:false, resizable:true );
		window.view.keyDownAction_({
			|view,a,b,c|
			switch (c,
				// space
				32, { scrolling=true },
				// -
				45, { zoomLevel = zoomLevel-1 },
				// +
				61, { zoomLevel = zoomLevel+1 });
			position = window.view.visibleOrigin.x/(envelopeWidth-window.view.bounds.width);
			envelopeViews.do({ |v| 
				v.bounds = v.bounds.width_( ((1.05**zoomLevel)*20000) ) });
			window.view.visibleOrigin = Point( position*(envelopeWidth-window.view.bounds.width) );
		});
		
	}
	
	initRoutine {
		updateRoutine = Routine({
			| time |
			var t, x;
			var startX, width, maxDuration;
			startX = 10; width = envelopeWidth;
			maxDuration = DynamicEnvelope.duration;
			loop {
				t = DynamicEnvelope.getCurrentTime() / maxDuration;
				scrolling.if({
					cti.bounds = cti.bounds.left_(startX + (t*width));
					scrollView.visibleOrigin = ((t*envelopeWidth)-40).min(envelopeWidth-window.bounds.width).max(0)@scrollView.visibleOrigin.y;
				});
				x = t;
		
				0.1.yield;
			}
		}).play(AppClock);
	}
	
	addEnvelope {
		| env |
		var view;
		view = DynamicEnvelopeView( this, env );
		envelopes.add( env );
		envelopeViews.add( view );
		container.refreshPositions();
	}
	
	remove {
	}
	
	syncDataFromServer {
	}
	
	syncDataToServer {
	}
	
	
	front {
		window.front;
	}
}

DynamicEnvelopeView {
	var parent, env,
		container, <view;

	*new {
		| parent, env |
		^super.newCopyArgs(parent, env).init;	
	}
	
	init {
		container = parent.container;
		this.makeView();
		this.updateViewFromEnv();
	}

	makeView {
		var changingEnvelope=false,
			envData,
			newX, newY;		
		
		view = SCEnvelopeView( parent.container, Rect(0, 0, parent.envelopeWidth, 135) )
			.drawLines_(true)
			.editable_(false)
			.selectionColor_(Color.red)
			.drawRects_(true)
			.canFocus_(false)
//			.resize_(5)
			.action_({arg b; changingEnvelope=true })
//			.thumbSize_(5)
			.thumbHeight_(12)
			.thumbWidth_(10)
			.fillColor_(Color.white)
			.mouseDownAction_({
				|v, x, y|
			})
			.mouseUpAction_({
				| v, x, y |
				var insIndex=0, envData;
				//[v,x,y].postln;
				if( changingEnvelope, { changingEnvelope=false }, 
				{
					envData = env.envData;
					("before: " + envData).postln;
					newX = (x / v.bounds.width);
					newY = 1-( (y-v.bounds.top)/v.bounds.height );
					[newX, newY].postln;
					while( { (envData[0][insIndex] < newX) && (insIndex<envData[0].size) }, 
						{ insIndex = insIndex+1 });
					env.envData[0] = envData[0].insert( insIndex, newX );
					env.envData[1] = envData[1].insert( insIndex, newY );
					env.envData = envData;							("after: " + envData).postln;			
					//env.sendArrayToBuffer();
					//env.buffer.sendCollection( [envData[1], envData[0]*DynamicEnvelope.duration].flop.flatten );
					//env.synth.set( \t_resetFromHere, 1 );
					this.updateViewFromEnv();
				})
			});	
		parent.container.refresh();
	}
	
	updateViewFromEnv {
		view.value_([ (env.envData[0]) ++ [1], env.envData[1] ++ env.envData[1].last ]);
	}
}
// Controller classes

/*  
	Buffer Player synthdef

SynthDef( \bufferPlayer, {
	arg out=0, bufnum=0, amp=0.5, speed=1.0;
	Out.ar( out, amp*PlayBuf.ar( 1, bufnum, speed, loop: 1 ) );
}).send(s).store;

	Buffer Recorder SynthDef

SynthDef( \bufferRecorder, {
	arg in=0, bufnum=0, amp=0.5, speed=1.0;
	RecordBuf.ar( InFeedback.ar( in ), bufnum, recLevel:amp, loop:1 );
}).send(s).store;
	
*/

BufferCollection {
	var s, w, listBox, loadButton, sBox, newButton, trashButton, plotButton, buffers;
	
	*new {
		| s ... initialBuffers |
		^super.newCopyArgs(s).init( initialBuffers );
	}
	
	init {
		| initialBuffers |
		var testSink;
		
		w = PageLayout( "buffer collection", hspacer:5, vspacer:5 );
		
		listBox = SCListView( w.window, w.layDown( 190, 300 ) ).background_(Color.grey(1, 0.9))
			.font_(Font("LucidaSans", 10))
			.beginDragAction_({
				| list |
				buffers.at( list.value );
			});
		
		loadButton = ActionButton( w, "load", 
			{ File.openDialog( "Path of audio file", { |file| this.addPath(file) } )});
		
		newButton = ActionButton( w, "new", 
			{ this.newBufferDialog }, minWidth: 30 );
		
		trashButton = ActionButton( w, "free", 
			{ this.remove(listBox.value) } );
		
		plotButton = ActionButton( w, "plot", 
			{ buffers[listBox.value].plot } );
		
		
		buffers = List.new;
		
		initialBuffers.do({
			| item |
			(item.isString).if({
				this.addPath(item);
			});
			(item.isNumber).if({
				this.addNew( dur: item );
			});
			(item.class == Buffer).if({
				this.addBuffer( item );
			});
		});
		
		w.resizeToFit.front;
	}
	
	remove {
		| index |
		var i;
		buffers.removeAt(index).free.postln;
		i = listBox.items;
		i.removeAt(index);
		listBox.items = i;
	} 
	
	newBufferDialog {
		var wd, name, num, ok, close;
		wd = SCWindow.new( "Create new buffer", Rect(w.window.bounds.left, w.window.bounds.top, 200, 100 ),
			border: false, resizable: false ).alwaysOnTop_(true);
		name = SCTextField( wd, Rect( 5, 5, 190, 20 ) )
			.string_( "untitled" );
		num = SCNumberBox( wd, Rect( 5, 30, 50, 20 ) )
			.value_(10);
		ok = SCButton( wd, Rect(5, 55, 50, 20) );
		ok.states_( [["ok",Color.grey(0.5, 0.9), Color.grey(1)]] )
			.action_({
				(num.value>0 && num.value<999).if({
					this.addNew( name.string, num.value );
					wd.close;
				})
			});
		close = SCButton( wd, Rect(60, 55, 50, 20) );
		close.states_( [["cancel",Color.grey(0.5, 0.9), Color.grey(1)]] )
			.action_({
				wd.close;
			});
		wd.front;
	}
	
	addPath {
		| path |
		var buf;
		buf = Buffer.read( s, path );
		this.addBuffer( path, buf );
		^buf;
	}
	
	addNew {
		| name="untitled", dur |
		var buf;
		buf = Buffer.alloc( s, 44100*dur );
		this.addBuffer( name ++ " " ++ dur ++ "s", buf );
		^buf;
	}
	
	addBuffer {
		| name, buf |
		buffers = buffers.add( buf );
		listBox.items = listBox.items.add( "(" ++ buf.bufnum ++ ") " ++ name );
		^buf;
	}
}

BufferPlayer {
	var s, w, <buffer, amp, speed, isPlaying;
	var window, <>node;
	var playButton, deleteButton, bufferSink, outSource;
	
	*new {
		| s, w, name="untitled buffer", buffer, amp=0.5, speed=1, isPlaying=false |
		^super.newCopyArgs(s, w, buffer, amp, speed, isPlaying)
			.bufferPlayerInit( name );
	}
	
	bufferPlayerInit {
		| name |
		
		w = w ?? PageLayout( "buffer player", hspacer:5, vspacer:5 );
				
		buffer = buffer ?? Buffer.new( s, 1, 1 );
		buffer.postln;
	
		// Init synth
		node = NodeProxy.new( Server.default, \audio, 1 )
			.add( \bufferPlayer, 0, \bufnum, buffer.bufnum )
			.pause;
		
		// Init window;
		w.window.onClose = { this.delete };
		
		// In source
		SCDragSink( w.window, w.layDown( 200, 20 ) )
			.canReceiveDragHandler_({
				| obj |
				obj.class.currentDrag.class == Buffer;
			})
			.receiveDragHandler_( {
				| s |
				s.class.currentDrag.postln;
				this.buffer_(s.class.currentDrag);
				s.string_( buffer );
			})
			.string_( buffer );

		
		// Play Button
		w.startRow;
		SCButton( w.window, w.layDown( 50, 20 ) )
			.states_( [
				["play", Color.black, Color.green(1, 0.3)],
				["stop", Color.black, Color.red(1, 0.3) ] 
				] )
			.action_( {
				| but |
				(but.value == 1).if({
					this.play;
				}, {
					this.stop;
				} )
			} );
	

		// Amp
		w.startRow;
		EZSlider( w, 300@20, "Amp", ControlSpec( 0.1, 1.5, \exp ), { |v| this.amp_(v.value) }, amp );

		// Speed 
		w.startRow;
		EZSlider( w, 300@20, "Speed", ControlSpec( 0.1, 10, \exp ), { |v| this.speed_(v.value) }, speed );
		
		// Out source
		w.startRow;
		w.startRow;
		SCDragSource( w.window, w.layRight( 40, 20) )
			.background_( Color.blue( 1, 0.1 ) )
			.object_( node )
			.string_( "out" )
			.align_( \center );

		w.startRow;
		w.startRow;

		w.resizeToFit.front;
		node.set( \bufnum, buffer.bufnum );
	}
	
	stop {
		isPlaying = false;
		node.pause;
	}
	
	play {
		isPlaying = true;
		node.resume;
	}
	
	amp_ {
		| a |
		amp  = a;
		node.set( \amp, a );
	}
	
	speed_ {
		| sp |
		speed = sp;
		node.set( \speed, sp );
	}
	
	delete {
	}
	
	buffer_ {
		| b |
		buffer = b;
		node.set( \bufnum, b.bufnum );
	}
}

BufferRec {
	var s, w, <buffer, in, amp, speed, isRecording;
	var window, <>node;
	var recButton, deleteButton, bufferSink, inSink, outSource;
	
	*new {
		| s, w, name="untitled buffer", buffer, in=(-1), amp=0.5, speed=1, isRecording=false |
		^super.newCopyArgs(s, w, buffer, in, amp, speed, isRecording)
			.bufferRecInit( name );
	}
	
	bufferRecInit {
		| name |
		
		w = w ?? PageLayout( "buffer recorder", hspacer:5, vspacer:5 );
				
		buffer = buffer ?? Buffer.alloc( s, 44100*10, 1 );
		buffer.postln;
	
		// Init synth
		node = NodeProxy.new( Server.default, \audio, 1 )
			.add( \bufferRecorder, 0, \in, in, \bufnum, buffer.bufnum )
			.pause;
		
		// Init window;
		w.window.onClose = { this.delete };
		
		// Input
		inSink = SCDragSink( w.window, w.layDown( 200, 20 ) )
			.canReceiveDragHandler_({
				| obj |
				obj.class.currentDrag.class == Integer || obj.class.currentDrag.class == NodeProxy;
			})
			.receiveDragHandler_( {
				| s |
				s.class.currentDrag.postln;
				(s.class.currentDrag.class == Integer).if({
					this.input_(s.class.currentDrag);
				},{
					this.input_(s.class.currentDrag.bus.index);
				});
			})
			.string_( in );		
		

		
		// RecBuffer
		bufferSink = SCDragSink( w.window, w.layDown( 200, 20 ) )
			.canReceiveDragHandler_({
				| obj |
				obj.class.currentDrag.class == Buffer;
			})
			.receiveDragHandler_( {
				| s |
				s.class.currentDrag.postln;
				this.buffer_(s.class.currentDrag);
				s.string_( buffer );
			})
			.string_( buffer );		
		
		// Rec Button
		w.startRow;
		SCButton( w.window, w.layDown( 50, 20 ) )
			.states_( [
				["rec", Color.black, Color.green(1, 0.3)],
				["stop", Color.black, Color.red(1, 0.3) ] 
				] )
			.action_( {
				| but |
				(but.value == 1).if({
					this.rec;
				}, {
					this.stop;
				} )
			} );
	

		// Amp
		w.startRow;
		EZSlider( w, 300@20, "Amp", ControlSpec( 0.1, 1.5, \exp ), { |v| this.amp_(v.value) }, amp );
		
		// Out source
		w.startRow;
		SCDragSource( w.window, w.layRight( 40, 20) )
			.background_( Color.blue( 1, 0.1 ) )
			.object_( node )
			.string_( "out" )
			.align_( \center );

		w.startRow;
		w.startRow;

		w.resizeToFit.front;
		node.set( \bufnum, buffer.bufnum );
	}
	
	stop {
		isRecording = false;
		node.pause;
	}
	
	rec {
		isRecording = true;
		node.resume;
	}
	
	amp_ {
		| a |
		amp  = a;
		node.set( \amp, a );
	}
		
	delete {
	}
	
	buffer_ {
		| b |
		buffer = b;
		node.set( \bufnum, b.bufnum );
	}
	
	input_ {
		| i |
		in = i;
		inSink.string_( in );
		node.set( \in, in );
	}
}



/* table interface demo 
s = Server.internal.boot;
p = ProxySpace.push(s);

w = SCWindow.new( "test", Rect(100,500, 400,400 ) );
w.view.decorator = FlowLayout( w.view.bounds );

t = TabletInterface( s, w )
w.front;


s.sendMsg( \n_set, t.y.group.nodeID, \in, 0.9 )
t.pres.scope;
t.cy.map(0.5)
t.cx = [200,500, \linear, 0.001 ].asSpec;
t.cy = [200,500, \linear, 0.001 ].asSpec;

~test = {
	t.pres.kr * (
		(t.tiltx.kr * (SinOsc.ar( t.y.kr ) ) ) +
		(t.tilty.kr * (SinOsc.ar( t.x.kr ) ) )
	)
}

~test.play;
*/
TabletInterface {
	var s, <w, size;
	var <>cx, <>cy, <>cpres, <>ctiltx, <>ctilty;
	var numx, numy, numpres, numtiltx, numtilty;
	var dragFrom;

	var view;
	var <>x, <>y, <>pres, <>tiltx, <>tilty;
	var <>additionalAction;

	var defaultSpec;
	var defaultSynth;
	var defaultAction;
	var lastT;

	*new { 
		| s, w=nil, size=300,
			cx=nil, cy=nil, cpres=nil, ctiltx=nil, ctilty|
		^super.newCopyArgs( s, w, size, cx, cy, cpres, ctiltx, ctilty ).tabletInit;
	}
	
	tabletInit {		
		var defaultMouseMoveAction, defaultMouseDownAction;
		
		w = w ?? PageLayout( "tablet interface", hspacer: 5, vspacer: 5 );	
		// Defaults
		defaultSpec = nil.asSpec;
		defaultSynth = SynthDef( \defaultTablet, { | out=1, in | Out.kr( Lag.kr( out, 0.03 ), in ); } );	
		
		// Specs
		cx = cx ?? defaultSpec;
		cy = cy ?? defaultSpec;		
		ctiltx = ctiltx ?? defaultSpec;		
		ctilty = ctilty ?? defaultSpec;		
		cpres = cpres ?? defaultSpec;
		
		// Synths
		x = NodeProxy.control( s, 1 ).put(nil, defaultSynth, 0, [\in, cx.default]);
		y = NodeProxy.control( s, 1 ).put(nil, defaultSynth, 0, [\in, cy.default]);
		tiltx = NodeProxy.control( s, 1 ).put(nil, defaultSynth, 0, [\in, ctiltx.default]);
		tilty = NodeProxy.control( s, 1 ).put(nil, defaultSynth, 0, [\in, ctilty.default]);
		pres = NodeProxy.control( s, 1 ).put(nil, defaultSynth, 0, [\in, cpres.default]);
		
		// view stuff
		//w.view.decorator.nextLine;
		view = SCTabletView( w.window, w.layRight( size, size) )
			//.knobColor_(Color.black)
			.background_(Color.green( 0.1, 0.5) );
		//view = SCTabletView( w, Rect(0,0, size, size) );
		
		w.within( 90, size, {
			arg subw; 
			numx = this.makeSCNumberBox( "x", subw, x, cx );
			numy = this.makeSCNumberBox( "y", subw, y, cy );
			numtiltx = this.makeSCNumberBox( "tilt-x", subw, tiltx, ctiltx );
			numtilty = this.makeSCNumberBox( "tilt-y", subw, tilty, ctilty );
			numpres = this.makeSCNumberBox( "press.", subw, pres, cpres );
		});

		defaultAction =  {
			arg  view,inx,iny,inpres,intiltx,intilty,indeviceID, buttonNumber,clickCount,inabsZ,inrot;
			var vals;
			s.sendBundle( nil,
				[\n_set, x.group.nodeID, \in, cx.map(inx/size) ],
				[\n_set, y.group.nodeID , \in, cy.map(iny/size) ],
				[\n_set, pres.group.nodeID, \in, cpres.map(inpres) ],
				[\n_set, tiltx.group.nodeID, \in,  ctiltx.map( (intiltx+1)/2) ],
				[\n_set, tilty.group.nodeID, \in ,ctilty.map( (intilty+1)/2 ) ] );
		};		
		view.action = defaultAction;
		view.mouseDownAction = defaultAction;
		view.mouseUpAction = { s.sendBundle( nil, [\n_set, pres.group.nodeID, \in, cpres.map(0) ] ) };
		w.window.onClose = {
			this.close;
		};
		w.resizeToFit;
		w.front;
		lastT = Main.elapsedTime
	}



	
	makeSCNumberBox {
		| name, subw, synth, spec |
		var box;
		box = SCNumberBox( subw.window, subw.layRight(45,16, 5) );
		SCStaticText( subw.window, subw.layRight( 35, 16, 5) ).string_( name );

		box.mouseDownAction = {
			| view, inx, iny |
			dragFrom = inx;
			dragFrom.postln;
			view.doAction;
		};
		box.mouseMoveAction = { 
			| view, inx, iny |
			view.value = spec.map( spec.unmap( view.value ) + ((inx-dragFrom)/500) );
			dragFrom = inx;
			view.doAction;
		};
		box.value = spec.default;
		box.action = { | view | 			
			s.sendMsg( \n_set, synth.group.nodeID, \in, view.value );
		};
		^box;
	}
	
	close {
		x.clear;
		y.clear;
		tiltx.clear;
		tilty.clear;
		pres.clear;
	}
}


// Utility Controls

MSPSlider {
	var s, spec, name, val, <synth, titleView, sliderView, numberView, <>action;
	var dragFrom;
		
	*new {
		| s, spec, name="" |
		^super.newCopyArgs(s, spec, name).init;
	}
	
	init {
		spec = spec ?? nil.asSpec;
		synth = NodeProxy.control( s, 1 ).put(nil, \defaultTablet, 0, [\in, spec.default]);
		val = spec.default;
	}
		
	value {
		^val;
	}

	value_ {
		| v |
		val = spec.constrain( v );
		numberView.value = val;
		sliderView.value = spec.unmap( val );
		this.send;
	}
		
	gui {
		| w, rect |
		titleView = SCStaticText( w, Rect( rect.left, rect.top, 33, 16 ) )
			.string_(name)
			.align_(\right);
		sliderView = SCSlider( w, Rect( rect.left+35, rect.top, rect.width-80, 16 ) )
			.action_({
				| view |
				numberView.value = val = spec.map(view.value);
				this.send;
			});
		numberView = SCNumberBox( w, Rect( rect.left+rect.width-43, rect.top, 43, 16 ) )
			.action_({
				| view |
				view.value = val = spec.constrain( view.value );
				sliderView.value = spec.unmap( view.value );
				this.send;
			})
			.mouseDownAction_({
				| view, inx, iny |
				dragFrom = inx;
			})
			.mouseMoveAction_({ 
				| view, inx, iny |
				view.value = spec.map( spec.unmap( view.value ) + ((inx-dragFrom)/500) );
				dragFrom = inx;
				view.send;
			});
		
		// Defaults
		sliderView.value = spec.unmap( val );
		numberView.value = val;
	}
			
	send {
		(action.isNil).if({
			this.defaultAction.value;
		},{
			action.value;
		});
	}
	
	defaultAction {
		s.sendMsg( \n_set, synth.group.nodeID, \in, val );
	}
	
	kr {
		^synth.kr;
	}
	
	close {
		synth.clear;
	}
}

InputDrag {
	var s, name, node, param, val;
	var sink;
	
	*new {
		| s, name="", node, param=\in, default=(-1) |
		^super.newCopyArgs(s, name, node, param, default).init;
	}
	
	init {
		node.xset( param, val );
	}
		
	value {
		^val;
	}
		
	value_ {
		| v |
		val = v;
		node.xset( param, val );
		sink.isNil.not.if({ sink.string = v });		
	}
	
	gui {
		| w, rect |
				// In source
		sink = SCDragSink( w, rect )
			.canReceiveDragHandler_({
				| obj |
				obj.class.currentDrag.class == Bus;
			})
			.receiveDragHandler_( {
				| s |
				//s.class.currentDrag.postln;
				this.value_(s.class.currentDrag.index);
			})
			.align_(\center)
			.string_( val );
	}
}

OutputDrag {
	var s, name, node;
	var source;

	*new {
		| s, name="", node |
		^super.newCopyArgs(s, name, node).init;
	}
	
	init {

	}
	
	gui {
		| w, rect |
		source = SCDragSource( w, rect )
			.background_( Color.blue( 1, 0.1 ) )
			.object_( node.bus )
			.string_( node.bus.index )
			.align_( \center );
	}
	
	node_ {
		| n |
		node = n;
		source.object_( node.bus )
			.string_( node.bus );
	}
}

CollectionDrag {
	var s, name, collection;
	var <>listView, inputBox;

	*new {
		| s, name="", default |
		^super.newCopyArgs(s, name, default).init;
	}
	
	init {
		collection = collection ?? [];
	}
	
	value {
		^collection;
	}
	
	value_ {
		| c |
		collection = c;
	}
	
	gui {
		| w, rect |
		listView = SCListView( w, rect.resizeBy( 0, -20 ) )
			.items_( collection.collect( _.asString ) );
		inputBox = SCTextField( w, rect.resizeTo( rect.width, 18 ).moveBy( 0, rect.top+rect.height-20 ) )
			.action_({
				| obj |
				obj.value.postln;
				( obj.value[0] == "["[0] ).if({
					this.addToList( obj.value );
				},{
					this.addToList( "[" ++ obj.value ++ "]" );
				});
				obj.value = "";
			});
	}
	
	addToList {
		| in |
		var array;
		array = in.interpret;
		collection = collection ++ array;
		listView.items_( collection.collect( _.asString ) );
	}
}

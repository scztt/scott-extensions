LiveTimelineView {
	var timeline;
	var parentView, editView, <timeView, ctiView, timeNumberView, envEditViews, saveButton;
	var <timelinePlayer;
	var events, newEvent;
	var selectedEvent, draggingNode;
	var cSelected, cUnselected, totalTime=100, routine;
	 
	*new {
		|timeline, parentView|
		^super.newCopyArgs(timeline, parentView).init
	}
	 
	init {
		events = List.new;
		timelinePlayer = TimelinePlayer( timeline, SystemClock );
		cUnselected = Color.blue( 0.3,0.3 );
		cSelected = Color.green(0.8, 0.5 );
		draggingNode = selectedEvent = -1;
		
		this.initView();
		this.play();
	} 
	 
	initView {
		var w, h, mar;
		mar=5;
		
		parentView = parentView ? SCWindow("", Rect(10,10,700,700)).front;
		w = parentView.bounds.width;
		h = parentView.bounds.height;
		parentView.onClose_({ routine.stop; timeline.removeDependant( this ); });
		

		// CTI
		ctiView = SCSlider( parentView, Rect( 5, 5, w-10, 9 ))
			.thumbSize_( 4 );
		ctiView.resize_(2);
		
		// Simple display for event data
		editView = SCCompositeView( parentView, Rect( 5, h-60, w-10, 140) );
		//editView.resize_(8);

		timeNumberView = SCStaticText( parentView, 
			Rect( w-100, h-40, 60, 20 ) );
		timeNumberView.resize_(9);
		saveButton = RoundButton( parentView, Rect( w-100, h-20, 60, 20 ) )
			.states_([["save",Color.black, Color.grey]])
			.action_({
				var newVal;
				if( selectedEvent > -1, {
					envEditViews.keysValuesDo({
						| key, texteditview |
						newVal = nil;
						try {
							newVal = texteditview.string.interpret;
						};
						newVal.notNil.if({
							events[ selectedEvent ][2].obj[ key ] = newVal;
						})
					});
					events[ selectedEvent ][2].obj.asCompileString.postln;
				})
			});
		
		// Envelope display.
		timeView = SCEnvelopeView( parentView, Rect( 5,15,w-10,h-80 ))
			.drawLines_(false)
			.thumbSize_(9.5)
			.fillColor_( cUnselected )
			.selectionColor_( cUnselected )
			.keyUpAction_({
				| view, b,c, code |
				var values, index, event;

				switch (code)
				// delete
				{ 127 }
				{
					"delete".postln;
					values = view.value;
					index = selectedEvent;
					if( index != -1, {
						event = events.removeAt( index );
						values[0].removeAt(index);
						values[1].removeAt(index);
						view.value_( values );
						
						timelinePlayer.liveRemove( event[2] );
						timelinePlayer.postNodeOrder;
						
						if( index>0, {
							this.selectedEvent = index-1
						})
					})
				};
			})
			.action_({ 
				|view|
				"selected:%\n".postf(view.index);
				this.selectedEvent = view.index;
				draggingNode = view.index;
			})
			.mouseDownAction_({
				| view, x, y, mod |
				var bounds;
				"selected:%\n".postf(view.index);
				bounds = view.bounds;
				
				if( view.index == -1, {
					this.selectedEvent = -1;
					newEvent = [ (x-bounds.left)/bounds.width, 1-((y-bounds.top+15)/bounds.height), 
						( 
						amp: 0,
						y: 0,
						play: { 
							Synth( \ping, [\amp, ~amp.postln])
						} 
						) 
					] ;
				})
			})
			.mouseUpAction_({
				| view, x, y, mod |
				var values, newNode, index;
			
				// If an event was moved
				if( draggingNode > (-1), {
					"dragging".postln;
					values = view.value;
					index = view.index;
					events[index][0] = values[0][index];
					events[index][1] = values[1][index];
					events[index][2] = timelinePlayer.liveReschedule( events[index][2], values[0][index]*totalTime );
					events[index][2].obj.amp = (1-values[1][index]);
					
					draggingNode = -1;
				});

				if( (selectedEvent==(-1)) && newEvent.notNil, {
					newNode = TimelineNode( newEvent[2], totalTime*newEvent[0] );
					newNode.obj.amp = 1-newEvent[1];
					newNode.obj.y = 1-newNode.obj.amp;
					
					//events.add( newEvent[..1] ++ [newNode] );
					timelinePlayer.liveAdd( newNode );
					
					//view.value_( events.flop[..1] );
					newEvent = nil;
				});
				timelinePlayer.postNodeOrder;
			});
		timeView.resize_(2);
		
		timeline.addDependant( this );
//		timeline.

	}
		
	selectedEvent_{ 
		| eventIndex |
		if( (eventIndex > (-1)), {
			if(  eventIndex != selectedEvent, {
				try {
					timeView.setFillColor( selectedEvent, cUnselected );
				};
				selectedEvent = eventIndex;
				timeView.setFillColor( selectedEvent, cSelected );
				this.updateEditViewForEvent( events[selectedEvent][2].obj );
				//editView.string = events[eventIndex].asString;
			})
		},{
			if( selectedEvent.notNil, {
				selectedEvent = eventIndex;
				timeView.setFillColor( selectedEvent, cUnselected );
				//editView.string = "";
				this.updateEditViewForEvent( nil );
			})
		})
	}
	
	updateEditViewForEvent {
		| obj |
		var x=0, y=0, string, lines, textView;
		editView.children.do({ |child| child.remove });
		envEditViews = IdentityDictionary.new;
		if( obj.notNil, {
			obj.keysValuesDo({
				| key, value |
				SCStaticText( editView, Rect( x, y, 50, 18 ) )
					.string_( key );
				x = x + 50;
				string = value.asCompileString();
				string = string.replace("\t","");
				lines = (string.size/20).ceil;
				envEditViews[key] = SCTextView( editView, Rect( x, y, 500, (lines*18)-2 ))
					.string_( string );
				x = 0;
				y = y + (lines*18);
			});
		});
		y = y + 20;
		editView.bounds = editView.bounds.height_(y);
		parentView.bounds = parentView.bounds.height_( editView.bounds.top + editView.bounds.height + 5 );
	}
	
	play {
		routine = Routine({
			inf.do({
				|i|
				{ 
					ctiView.value = timelinePlayer.time/100.0;
					timeNumberView.string = timelinePlayer.time;
				}.defer;
				0.1.yield;
			})
		});
		routine.play( SystemClock )
	}
	
	add {
		| node |
		events.add( [ node.time/totalTime, node.obj.y, node ].postln );
		{ 
			timeView.value_( events.flop[..1] );
			this.selectedEvent_( events.size-1 );
		}.defer;
	}
	
	remove {
		| node |
		"removing".postln;
		events.do({
			| event, i |
			if( event[2] == node, {
				^events.removeAt( i );
			})
		});
		
	}
	
	update {
		| who, what ...args |
		if( who == timeline, {
			switch (what)
				{ \addedNode } { this.add( args[0] ) }
				{ \removedNode } { this.remove( args[0] ) };
		})
	}
}
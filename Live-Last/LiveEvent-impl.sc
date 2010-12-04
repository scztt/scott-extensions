LiveEventSequence : LiveEvent {
	var playhead, <>parentPlayhead, <seq, prepareAll=false, <>repeats=1;
	
	init {
		| inPlayhead |
		seq = LinkedObjectList.new;
		^super.init()
	}
	
	initRoutines {
		var playhead;
		var prepConditionList = List.new;
		
		/////////////////////////////////////////////////////////////////////////////////////////////////////
		// prepare
		prepareRoutine = Routine({
			seq.head.notNil.if({
				this.state = \preparing;
				
				if( prepareAll, {
					seq.do({
						| item |
						item.doPrepare();
						prepConditionList.add( item.prepHasFinished );
					});
					prepConditionList.do( _.wait );
					prepConditionList.clear;
				},{
					seq.head.doPrepare();
					seq.head.prepHasFinished.wait();
				});
				
				this.state = \prepared;
				prepHasFinished.test_(true).signal;
			})
		});		
	}
	
	seq_{
		| inSeq |
		if( seq.isEmpty, {
			seq = inSeq;
		})
	}
	
	add {
		| item |
		seq = seq.add( item );
	}
	
	doInitialize {
		var initConditionList = List.new;
		this.do({
			| item |
			item.doInitialize();
			initConditionList.add( item.initHasFinished );
		});
		
		fork {
			initConditionList.do(_.wait);		
			this.state = \initialized;
			initializeHasFinished.test_(true).signal;
		}
	}
	
	doPlay {
		| inPlayhead, count=1 |
		var iter=0;
		playhead = LiveEventPlayHead();
		playhead.startTimeHint = startTimeHint;
		inPlayhead.addDependant(playhead);
		
		this.state = \playing;
		while( iter<count, {
			protect {	
				playhead.play();
				playhead.playFinished.wait;
				endTimeHint = playhead.endTimeHint ? clock.seconds;
			} {
				playHasFinished.test_(true).signal;
				inPlayhead.removeDependant( playhead );
				this.state = \initialized;
			};
		});
	}
	
	color {
		^Color.blue(0.7);
	}
	
	hasDetailView {
		^true
	}
	
	makeDetailView {
		| parent, bounds, selectionManager, dragHandler |
		var view = LiveNodeListView( parent, bounds );
		view.selectionManager = selectionManager;
		view.dragHandler = dragHandler;
		view.data = this;
		view.updateArrangement();
		^view;
	}
	
	destroyDetailView {
	}
}


EnvirLiveEvent : LiveEvent {
	var <envir;
	
	init {		
		| inEnvir |
		this.initEnvir( inEnvir );
		^super.init();
	}
	
	initRoutines {
		prepareRoutine = Routine({
			this.state = \preparing;
			
			envir.use( prepareAction );
			prServer.sync();
			
			this.state = \prepared;
			prepHasFinished.test_(true).signal;
		});
		
		playRoutine = Routine({
			prepHasFinished.wait;			
			this.state = \playing;
			
			startTimeHint = startTimeHint ? thisThread.clock.seconds;
			
			duration.notNil.if({
				thisThread.clock.schedAbs( startTimeHint + duration, 
					{ playHasFinished.test_(true).signal });
			});
			
			envir.use( playAction );
			
			duration.isNil.if({ 
				playHasFinished.test_(true).signal;
			});
			
			this.state = \donePlaying;
			if( prAutofree, { freeRoutine.play(clock) });
		});
		
		freeRoutine = Routine({
			playHasFinished.wait;
			envir.use( freeAction );
			{ this.prReset() }.defer;		
		});
	}
	
	initEnvir {
		| e |
		envir = e ? envir;
		if( envir.notNil, {
			if( envir.includesKey(\duration),	{ duration = envir[\duration].value });
			if( envir.includesKey(\initialize), 	{ initAction = envir[\initialize] },
										{
											this.state = \initialized;
											initializeHasFinished.test = true;
										});
			if( envir.includesKey(\prepare), 	{ prepareAction = envir[\prepare] },
										{ 	
											prepHasFinished.test = true; 
										});
			if( envir.includesKey(\play), 	{ playAction = envir[\play] });
			if( envir.includesKey(\free), 		{ freeAction = envir[\free] });
			
			envir[\release] = { 
				playHasFinished.test_(true).signal;
			};
		})
	}
	
	envir_{
		| e |
		if( e != envir, {
			this.initEnvir( e );
		})
	}
	
	doInitialize {
		var item;
		if( state.isNil, {
			prServer.doWhenBooted({
				// Automatically define synthdefs
				if( envir.includesKey(\synthDef), 
					{ envir[\synthDef].send(prServer) });
				if( envir.includesKey(\synthDefs), 
					{ envir[\synthDefs].do( _.send(prServer) ) });
					
				envir.use( initAction );
				state = \initialized;
				this.changed(\stateChanged, state);
				initializeHasFinished.test_(true).signal;
			})
		})
	}
	
	color {
		envir.notNil.if({ 
			^envir[\color] ?? { ^super.color }
		})
	}
}

+Event {
	le {
		var env;
		env = Environment.new;
		env.know_(true);
		env.putAll( this ); 
		^env
	}
}
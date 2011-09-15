LiveEventSequence : LiveEvent {
	var playhead, <>parentPlayhead, <seq, prepareAll=false, <>repeats=1, 
		<>name;
	
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
		"%.doPlay\n".postf(this);
		playhead = LiveEventPlayHead(seq.head);
		playhead.startTimeHint = startTimeHint;
		inPlayhead.addDependant(playhead);
		
		this.state = \playing;
		while({ (iter.postln < count.postln).postln }, {
			"starting loop".postln;
			protect {	
				"in protect loop".postln;
				playhead.play();
				"after play".postln;
				playhead.playFinished.wait;
				"after playfinished".postln;
				endTimeHint = playhead.endTimeHint ? clock.seconds;
				"after hint".postln;
				playhead.reset();
				"after reset".postln;
			} {
				iter = iter+1;
			};
		});
		playHasFinished.test_(true).signal;
		this.state = \initialized;
		inPlayhead.removeDependant( playhead );
	}
	
	color {
		^Color.grey(0.8);
	}
	
	hasDetailView {
		^true
	}
	
	makeDetailView {
		| parent, bounds, selectionManager, dragHandler |
		var view = LiveNodeListView( parent, bounds );
		view.drawBorders = false;
		view.selectionManager = selectionManager;
//		view.dragHandler = dragHandler;
		view.data = this;
		view.updateArrangement();
		^view;
	}
	
	destroyDetailView {
	}
	
	detailAcceptsDrag {
		^true
	}
	
	printOn {
		| stream |
		stream << "LiveEventSequence([\n";
		seq.do({
			| item, i |
			stream << "\t";
			item.printOn(stream);
			stream << "\n";
			if( i>500, {
				"Woa! Too large!".warn;
				^nil;
			})
		});
		stream << "])";
	}	
}


EnvirLiveEvent : LiveEvent {
	var <envir;
	
	init {		
		| inEnvir |
		super.init();
		this.initEnvir( inEnvir );
		^this;
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
			"entered playroutine".postln;
			this.identityHash.postln;
			prepHasFinished.postln; prepHasFinished.test.postln;
			prepHasFinished.wait;			
			"done waiting for prep".postln;
			this.state = \playing;
			
			startTimeHint = startTimeHint ? thisThread.clock.seconds;
			
			duration.notNil.if({
				thisThread.clock.schedAbs( startTimeHint + duration, 
					{ playHasFinished.test_(true).signal });
			});
			
			"playing".postln;
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
		"% - initEnvir\n".postf( this.identityHash.postln );
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
		});
		prepHasFinished.test.postln;
	}
	
	envir_{
		| e |
		if( e != envir, {
			this.initEnvir( e );
		})
	}
	
	prReset {
		super.prReset();
		this.initEnvir();
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
			^envir[\color].value ?? { ^super.color }
		})
	}
	
	name {
		envir.notNil.if({ 
			^envir[\name].value ?? { ^nil }
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
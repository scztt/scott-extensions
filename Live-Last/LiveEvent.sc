//LiveEvent public interface:

LiveEvent : LinkedObjectListNode {
	// from Node
	// var <>next, <>prev;
	// Public
	var 	<state, 
		<initializeHasFinished, <prepHasFinished, <playHasStarted, <playHasFinished, <eventHasFreed,
		>startTimeHint, <endTimeHint, duration, wasReleased=false,
		prAutofree=true, prServer, blockUpdates=false;
	// Private
	var 	initAction, prepareAction, playAction, freeAction,
		prepareRoutine, playRoutine, freeRoutine, clock,
		startTimeHint, endTimeHint, duration
		;
	
	*new {
		| ...args |
		^super.new.init( *args )
	}
	
	state_{
		| inState |
		state = inState;
		if( blockUpdates.not, {
			this.changed(\stateChanged, state);
		})
	}
	
	isPlaying { ^state==\playing }
	isPrepared { ^state==\prepared }
	isFree { ^state==\initialized }

	init {
		clock = SystemClock;
		initializeHasFinished = Condition(false);
		prepHasFinished = Condition(false);
		playHasStarted = Condition(false);
		playHasFinished = Condition(false);
		eventHasFreed = Condition(false);
			
		prServer = Server.default;

		this.initRoutines();
		this.prReset();
		CmdPeriod.add(this);
	}
	
	initRoutines {
		prepareRoutine = Routine({
			this.state = \preparing;
			
			prepareAction.value();
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
			
			playAction.value();
			
			 duration.isNil.if({ 
				playHasFinished.test_(true).signal;
			 });
			
			this.state = \donePlaying;
			if( prAutofree, { freeRoutine.play(clock) });
		});
		
		freeRoutine = Routine({
			playHasFinished.wait;
			freeAction.value();
			{ this.prReset() }.defer;		
		});
	}

	doInitialize {
		this.state = \initialized;
	}
		
	doPrepare {
		if( state==\initialized, {
			prepareRoutine.play(clock)
		})
	}
	
	doPlay {
		if( (state==\initialized) && prepareAction.notNil, {
			prepareRoutine.play(SystemClock)
		});
		
		playRoutine.play(clock);
		playHasFinished.wait();
		
		if( duration.notNil && wasReleased.not, {
			endTimeHint = startTimeHint+duration 
		},{
			endTimeHint = thisThread.clock.seconds;
		});
	}
	
	doRelease {
		wasReleased = true;
		playHasFinished.unhang;
		freeRoutine.play(clock);
	}
	
	prReset {
		prepareRoutine.stop.reset;
		playRoutine.stop.reset;
		freeRoutine.stop.reset;
		
		startTimeHint = duration = nil;
		wasReleased = false;

		initializeHasFinished = Condition(false);
		prepHasFinished = Condition(false);
		playHasStarted = Condition(false);
		playHasFinished = Condition(false);
		eventHasFreed = Condition(false);
		
		if( state.notNil, { 
			initializeHasFinished.test = true;
			this.state = \initialized;
		});		
	}
		
	color { ^Color.blue(0.7) }

	hasDetailView { ^false }
	makeDetailView { }
	destroyDetailView { }

	hasGui { ^false }
	makeGui {}
	destroyGui {}
	
	cmdPeriod {
		this.prReset();
	}
}

+Nil {
	use {
		| func |
		^func.value
	}

	useArgs {
		| func ...args |
		^func.value(*args)
	}
}

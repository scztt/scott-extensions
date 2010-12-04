LiveEventPlayHead {
	var startEvent, currentEvent, <>startTimeHint, <>endTimeHint;
	var playing=false, <playFinished, playRoutine;
	
	*new {
		| startEvent |
		^super.newCopyArgs(startEvent).init;
	}
	
	init {
		CmdPeriod.add(this);
	}
		
	startEvent_{
		| event |
		startEvent = event;
	}
	
	play {
		var i=0;
		if( playing.not, {
			playing = true;
			playFinished = Condition(false);
			playRoutine = Routine({
				currentEvent = startEvent;
				while({currentEvent.notNil}, {
					currentEvent.startTimeHint = startTimeHint;
					
					currentEvent.doPlay(this);
					startTimeHint = currentEvent.endTimeHint;
					
					currentEvent = currentEvent.next;
				});
				playFinished.test_(true).signal;
				playing = false;
			}).play;
		})
	}

	reset {
		playRoutine.notNil.if({ playRoutine.stop.reset() });
		playRoutine = nil;
		currentEvent = nil;
		playing = false;
		playFinished.notNil.if({ playFinished.unhang });
		playFinished = nil;		
	}
	
	cmdPeriod {
		this.reset();
	}
}
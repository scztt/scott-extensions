
AsyncCondition {
	var <>test, waitingThreads, <>object, <>action;
	
	*new { arg object, test=false;
		^super.newCopyArgs(test, Array(8), object)
	}
	
	wait {
		| completionAction |
		action = completionAction;
		if (test.value.not, {
			waitingThreads = waitingThreads.add(thisThread);
			nil.yield;
		});
		^object
	}
	
	dontWait {
		| completionAction |
		action = completionAction;
		^object
	}

	signal {
		var tempWaitingThreads, time;
		if (test.value, {
			time = thisThread.seconds;
			tempWaitingThreads = waitingThreads;
			waitingThreads = nil;
			action.value( object );
			tempWaitingThreads.do({ arg thread; 
				thread.clock.sched(0, thread);
			});
		});
	}
	
	unhang {
		var tempWaitingThreads, time;
		// ignore the test, just resume all waiting threads
		time = thisThread.seconds;
		tempWaitingThreads = waitingThreads;
		waitingThreads = nil;
		action.value( object );
		tempWaitingThreads.do({ arg thread;
			thread.clock.sched(0, thread);
		});	
	}
}

+ArrayedCollection {
	wait {
		this.do( _.wait );
	}
}

// Buffer extensions
+Buffer {
	*readAsync {
		| server, path, startFrame, numFrames, bufnum |
		var asyncCondition, buf;
		asyncCondition = AsyncCondition( buf = Buffer.read( server, path, startFrame, numFrames, nil, bufnum ) );
		buf.doOnInfo = { asyncCondition.test = true; asyncCondition.signal; };
		^asyncCondition
	}
}

+SynthDef {
	sendAsync {
		| server |
		var asyncCondition, id;
		asyncCondition = AsyncCondition( this );
		id = server.addr.makeSyncResponder( asyncCondition );
		this.send( server, ["/sync",id] );
		^asyncCondition
	}
}
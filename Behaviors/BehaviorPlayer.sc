BehaviorPlayer {
	var <server, <type, <params, <size, <group, <synthStack, 
		behaviorsLib, <behaviorStack, <bus, <running=false,
		autoSend=true, clock, publishControls=true, <nodeWatcher, 
		removed, <>defaultValues, <baseSynth;
	
	*new {
		| server, type, params |
		^super.newCopyArgs( server, type, params).init;
	}
	
	// Build a behaviorplayer using params based on controls from a synthdef.
	// type == aSynthDef.name
	*newFromSynthDef {
		| server, synthDef |
		^super.newCopyArgs( server, synthDef.name, synthDef.allControlNames.collect( _.name ) )
			.defaultValues_( synthDef.controls )
			.init;
	}
	
	init {
		behaviorsLib = SynthBehavior.library( type );
		if( behaviorsLib[\_params].isNil && params.notNil, {
			behaviorsLib[\_params] = params;
		},{
			params = OrderedIdentitySet.new.addAll( behaviorsLib[\_params] ? params );
		});
		
		size = params.size;
		behaviorStack = List.new;
		synthStack = List.new;
		clock = TempoClock.new;
		removed = IdentityDictionary.new;
		
		if( autoSend && server.serverRunning, { this.send });
	}
	
	send {
		| target, addAction=\addToTail |
		target = target ? server.defaultGroup;
		if( running.not, {
			bus = Bus.alloc( \control, server, size );
			group = Group( target, addAction );
			baseSynth = {  (defaultValues ? (0!this.size) ).postln.collect( DC.kr(_) )    }.play( group, bus, addAction:\addToHead )
		});
	}
	
	// @bug: free will cause synth destruction notification events to not be sent.
	free {
		if( running, {
			bus.free;
			group.freeAll;
			synthStack.clear;
			behaviorStack.clear;
		});
	}
	
	add {
		| inBehavior, fadetime=10 |
		var behavior, toRemove, usedParams, usedParamCount, synth;
		
		fork {
			// Do our best to turn behavior arg into a SynthBehavior
			if( inBehavior.isKindOf( Symbol ), { 
				behavior = behaviorsLib[ inBehavior ] 
			},{
				if( inBehavior.isKindOf( SynthBehavior ).not, {
					// not a behavior or a symbol, so generate a temp behavior and apply it
					behavior = SynthBehavior.addBehavior( type, ("_tmp"++UniqueID.next).asSymbol, inBehavior ).send.wait;
				})
			});
			
			if( behavior.isNil, { BehaviorError(inBehavior).throw });
			
			// If behavior takes parameters, publish them.
			if( publishControls && behavior.controlSpecs.notNil, {
				this.publishControls( behavior );
			});
			
			// if behavior is already playing, remove it and fade to new one
			if( behaviorStack.indexOf( behavior ).notNil, {
				// Not implemented. Nothing is needed here?
			});
			
			// Play synth and register in appropriate places.
			synth = this.prPlay( behavior, fadetime, group, bus );
			synthStack = synthStack.add( synth );
			NodeWatcher.register( synth, true );
			synth.addDependant( this );
			behaviorStack = behaviorStack.add( behavior );
			
			// Look for overlapping
			usedParams = IdentitySet.new;
			toRemove = List.new(16);
			behaviorStack.reverseDo({
				| stackItem, i |
				if( i != 0, {
					usedParamCount = stackItem.params.size;
					stackItem.params.do({
						| p |
						usedParams.addFail( p, { usedParamCount = usedParamCount-1 })
					});
					if( usedParamCount<=0, { toRemove.add( behaviorStack.size-i-1 ) })
				});
			});
			
			// Set fadetimes for whole stack.
			group.set( \fadeWrapped, fadetime );
			
			// Ungate and remove all toRemove synths
			toRemove.do( this.remove(_, fadetime) );
		}
	}
	
	touch {
		| inBehavior, duration=10, fadetime=0.1 |
		var behavior, synth, endTime;
		fadetime = min( fadetime ? 0, duration/2 );
		
		// From here, similar to add(), except we automatically remove after duration, and we don't check for overlaps
		fork {
			endTime = thisThread.seconds + duration - fadetime;
			
			// Do our best to turn behavior arg into a SynthBehavior
			if( inBehavior.isKindOf( Symbol ), { 
				behavior = behaviorsLib[ inBehavior ] 
			},{
				if( inBehavior.isKindOf( SynthBehavior ).not, {
					// not a behavior or a symbol, so generate a temp behavior and apply it
					behavior = SynthBehavior.addBehavior( type, ("_tmp"++UniqueID.next).asSymbol, inBehavior ).send.wait;
				})
			});

			if( behavior.isNil, { BehaviorError(inBehavior).throw });
			
			// If behavior takes parameters, publish them.
			if( publishControls && behavior.controlSpecs.notNil, {
				this.publishControls( behavior );
			});
			
			// Play synth and register in appropriate places.
			synth = this.prPlay( behavior, fadetime, group, bus, 0 );
			NodeWatcher.register( synth, true );
			synth.addDependant( this );
			//behaviorStack = behaviorStack.add( behavior );
			
			// Now, wait until endtime and remove the synth
			(endTime-thisThread.seconds).yield;
			synth.set( \gateWrapped, 0, \fadeWrapped, fadetime );
		}		
	}
	
	remove {
		| index, fadetime |
		removed.put( 
			synthStack.removeAt( index )
				.set( \gateWrapped, 0, \fadeWrapped, fadetime ),
				
			behaviorStack.removeAt( index )
		);
	}
	
	set {
		| ...args |
		group.set( *args );
	}
	
	prPlay {
		| behavior, fadetime, group, bus, fadeOutTo=1 |
		^Synth( behavior.defSymbol, 
			[ \i_outWrapped, bus.index, \gateWrapped, 1, \fadeWrapped, fadetime, \i_fadeOutTo, fadeOutTo ],
			group, \addToTail );
	}
	
	mapTo {
		| inSynth |
		var desc, msg;
		msg = Array(64).addAll([ "/n_map", inSynth.nodeID ]);
		if( params.notNil, {
			params.do({
				| name, i |
				msg.add( name );
				msg.add( bus.index + i );
			})
		});
		server.sendBundle( nil, msg );
	}
	
	publishControls {
		| behavior |
		var behavSymbol, controls;
		behavSymbol = behavior.name;	
		controls = behavior.controlSpecs;
		
		this.changed( \controlsAdded, behavSymbol, controls );
	}	
	
	unpublishControls {
		| synth |
		var behavior, behavSymbol, controls;
		behavior = removed.removeAt( synth );
		if( behavior.notNil, {
			behavSymbol = behavior.name;	
			controls = behavior.controlSpecs;
		
			this.changed( \controlsRemoved, behavSymbol, controls );
		});
	}
	
	update {
		| who, msg ... args |
		switch( msg,
			\n_end, { this.unpublishControls( who ) }
		);
	}
			
	saveCurrentBehavior {
		| name |
		behaviorStack.last.name = name;
		SynthBehavior.addBehavior( type, name, behaviorStack.last );
		SynthBehavior.saveLibrary();
		"Saved to %, %: %\n".postf( "\\" ++ type, "\\" ++ name, behaviorStack.last );
	}
	
	postValues {
		bus.getn(bus.numChannels, { 
			| vals | 
			"Bus %; [ % ]\n".postf( bus.index,
				params.collectAs({
					| param,i |
					format( "%=%", param, vals[i] );
				}, List).join(", ")			
			);
		})
	}
	
	debugScope {
		| title |
		title = title ? "%, bus %".format( type, bus.index );
		bus.debugScope( title, params.asArray );
	}
}

BehaviorError : Error {
	errorString {
		^if( what.isKindOf( Symbol ), {
			"Behavior \% does not exist.".format( what );
		},{
			"Behavior could not be compiled.";
		})
	}
	
	reportError {
		this.errorString.postln;
	}
}
	
+Set {
	addFail {
		| item, func |
		var index;
		if (item.isNil, { Error("A Set cannot contain nil.\n").throw });
		index = this.scanFor(item);
		if ( array.at(index).isNil, { 
			this.putCheck(index, item) 
		},{
			func.value;
		});		
	}
}


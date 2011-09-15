SynthBehavior {
	classvar 	currentLibrary,
			<>libraries, <>server, delayedSave, <>archivePath, <archive;
			
	var <function, <>type, <>name, <>params,
		<def, <>params, <>typeParams, <>controlSpecs, func, <defSymbol, fadeTime, 
		autoSend=true, sentCondition, <>server, delayedSave;
			
	*initClass {
		Class.initClassTree( Archive );
		Class.initClassTree(String);

		archivePath = Platform.userAppSupportDir +/+ "SynthBehaviors.sctxar";

		server = Server.default;
		libraries = IdentityDictionary.new;
		SynthBehavior.loadLibrary();
		delayedSave = Collapse.new({ this.saveLibrary() }, 0.1 );
	}
	
	*library {
		| lib |
		var newTmp;
		^ libraries.atFail(lib.asSymbol, { libraries.put( lib.asSymbol, newTmp = IdentityDictionary.new ); newTmp })
	}
	
	*get {
		| lib, name |
		if( name.notNil, {
			^this.library( lib )[ name ]
		},{
			^this.library( lib )
		})
	}
	
	*addBehavior {
		| lib, name, behavior |
		if( behavior.isKindOf( SynthBehavior ).not, {
			behavior = SynthBehavior( behavior, lib.asSymbol, name.asSymbol )
		});
		this.library(lib)[ name.asSymbol ] = behavior;
		//delayedSave.defer();
		^behavior;
	}

	*addBehaviors {
		| lib, behaviorDict |
		behaviorDict.do({
			| name, behavior |
			this.addBehavior( lib, name, behavior );
		})
	}
		
	*loadLibrary {
		archive = Object.readArchive( archivePath );
		
		libraries = archive.at( \libraries ) ? libraries;
	}
	
	*saveLibrary {
		archive.put( \libraries, libraries );
		archive.writeArchive( archivePath );
	}
	
	*cleanLibrary {
		var toRemove = List.new(32);
		var tmpPruned=0, emptyPruned=0;
		
		// Clean out all temp behaviors
		libraries.do({
			| lib |
			lib.keysValuesDo({
				| key, val |
				if( key.asString[0..3] == "_tmp", {
					toRemove.add( key );
				})					
			});
			tmpPruned = tmpPruned + toRemove.size;
			toRemove.do( lib.removeAt(_) );
			toRemove.clear;
		});
		
		// Now clean all empty libraries
		libraries.keysValuesDo({
			| key, val |
			if( val.isEmpty, { toRemove.add( key ) });
		});
		emptyPruned = emptyPruned + toRemove.size;
		toRemove.do( libraries.removeAt(_) );
		
		if( (emptyPruned+tmpPruned) > 0, {
			this.saveLibrary();
			"Pruned % tmp behaviors and % empty libraries.\n".postf( tmpPruned, emptyPruned );
		})
	}
	
	*setParams {
		| lib, params |
		this.library( lib )[\_params] = params;
	}
	
	*sendSynths {	
		| server |
		libraries.do({
			| lib |
			lib.keysValuesDo({
				| key, def |
				if( key != \_params, {
					key.postln;
					def.send();
				});
			})
		})
	}
	
	*new {
		| func, type, name |
		var new = super.newCopyArgs( func, type, name );
		new.init();
		^new
	}
	
	init {
		var ignoredControls;
		
		defSymbol = ["bh", type.asString, name.asString].join("_").asSymbol;
		typeParams = List.new(16).addAll( this.class.library( type )[\_params] ? [] );
		controlSpecs = IdentityDictionary.new;
		params = List.new(16);
		server = this.class.server;
		sentCondition = AsyncCondition( this );
		ignoredControls = IdentitySet.new.addAll([ \i_outWrapped, \gateWrapped, \fadeWrapped, \i_fadeOutTo ]);

		// Build SynthDef
		def = SynthDef( defSymbol, {
			| i_outWrapped=0, i_fadeOutTo=1, gateWrapped=0, fadeWrapped=0.001 |
			var env, hasNils=false, wrapped, newWrapped, r;
			
			// Wrap. If input isn't a function, turn it into one.
			wrapped = SynthDef.wrap( 
					if( function.isKindOf( Function ), { function }, { {function} })
			);
			
			// If it's a dictionary, remember the params it's using, and order them properly.
			wrapped.isKindOf( Dictionary ).if({ 
				this.params = List.newClear( typeParams.size );
				newWrapped = List.newClear( typeParams.size );
				typeParams.do({
					| param, i |
					newWrapped = newWrapped.put( i, wrapped[ param ] );
					this.params = this.params.add( param );
				});
				wrapped = newWrapped;
			});
			
			// If it's a sequence, get params from type and build param property based on that.
			wrapped.isKindOf( SequenceableCollection ).if({
				this.params = List.new(16);
				wrapped.do({
					| ugen, i |
					ugen.notNil.if({
						this.params = this.params.add( typeParams[i] );
					});
				})
			});
			
			// Do special case conversions and check for nils
			wrapped = wrapped.collect({
				| ugen, i |
				case  
				{ ugen.isKindOf( Number ) }
					{ DC.kr( ugen ) }
				{ ugen.isKindOf( Symbol ) }
					{ this.class.get( type, SynthDef.wrap( ugen ) ) }
				{ ugen.isNil }
					{ hasNils = true; ugen; }
				{ ugen.isKindOf( UGen ) }
					{ ugen; }
				{ true }
					{ ugen }
			});

			// Env to control crossfading between behaviors
//			env =  EnvGen.kr( Env([0,1,i_fadeOutTo],[1,1], releaseNode:1 ), gateWrapped, timeScale:fadeWrapped, doneAction:2 );
			env =  EnvGen.kr( Env([0,1,i_fadeOutTo],[fadeWrapped,fadeWrapped], releaseNode:1 ), gateWrapped, doneAction:2 );
 						
 			// If there are nils, we can't do ReplaceOut as a block, so loop through and make individuals
 			if( hasNils, {
 				wrapped.do({
 					| ugen, i |
 					ugen.notNil.if({
 						r = ReplaceOut.kr( i_outWrapped+i, 
 							 (env * ugen) + 
		 					((1-env) * In.kr( i_outWrapped+i, 1 ))
 						);
 					})
 				})
 			},{	// No nils, write as a block
 				r = ReplaceOut.kr( i_outWrapped, 
 					(env * wrapped) + 
 					((1-env) * In.kr( i_outWrapped, wrapped.size ))
 				);
 			})
		});
		
		// Create collection of default ControlSpecs. Try to use control name (i.e. /freq) to create
		def.allControlNames.do({
			| controlName |
			if( ignoredControls.includes( controlName.name ).not, {
				controlSpecs[ controlName.name ] = controlName.name.asSpec ? 
					ControlSpec( 0, 1.max( controlName.defaultValue ), \linear, 0, 1.max( controlName.defaultValue ) );
			})
		});
		
		// Auto-send
		if( autoSend && server.serverRunning, {
			this.send();
		});
	}
	
	send {
		var id;
		fork {
			def.send( server ).store();
			id = server.addr.makeSyncResponder( sentCondition );
			server.sendBundle(nil, [ "/sync", id ]);
		}
	}
	
	wait {
		| func |
		sentCondition.wait( func );
	}
	
	sent { 
		^sentCondition.test
	}
	
	size {
		^params.size
	}
}	
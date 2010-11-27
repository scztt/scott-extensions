SynthDefHistory {
	classvar historyPath, <archives, <historyChanged;
	
	*initClass {
		var files;
		Class.initClassTree(File);
		historyPath = Platform.userAppSupportDir +/+ "SynthDefHistory";
		if (PathName(historyPath).isFolder.not, {
			format("mkdir '%'", historyPath).postln.unixCmd;
		});
		archives = IdentityDictionary.new;
	}
	
	*load {
		| names |
		var paths;
		if( names.isString, { 
			names = [names];
		});
		if( names.isNil || names == "*" || names[0] == "*", {
			paths = (historyPath +/+ "*" ++ ".synthdefhistory").pathMatch;
			names = paths.collect({
				|path|
				PathName(path).fileNameWithoutExtension;
			})
		});
		names.asCollection.do({
			| name |
			this.archiveAt( name.asSymbol );
		})
	}
	
	*archiveAt {
		| symbol |
		var path, arch;
		archives[symbol].isNil.if({
			path = (historyPath +/+ symbol ++ ".synthdefhistory").pathMatch;
			if( path.size>0, {
				try({
					archives[symbol] = path[0].load;
				}, {
					"Error loading synth history archive from % \n".postf(path);
					"cp % %".format(path[0], path[0]++".loadFailed").unixCmd;
					archives[symbol] = this.newArchive();
				})
			},{
				archives[symbol] = this.newArchive();
			});
			this.changed(\defLoaded, symbol, archives[symbol]);
		});
		^archives[symbol]
	}

	*add {
		| name, ugenGraphFunc, rates, prependArgs, variants, metadata |
		var archive = this.archiveAt(name.asSymbol);
		var archivedSynthDef = IdentityDictionary.new;
		var hash = ugenGraphFunc.asCompileString.hash;
		var detected = archive.detect({ |item|  item[\hash]==hash  });
		if( detected.isNil, {
			archivedSynthDef[\name] = name.asSymbol;
			archivedSynthDef[\funcString] = ugenGraphFunc.asCompileString;
			archivedSynthDef[\func] = ugenGraphFunc;
			archivedSynthDef[\rates] = rates.asCompileString;
			archivedSynthDef[\prependArgs] = prependArgs.asCompileString;
			archivedSynthDef[\date] = Date.getDate();
			archivedSynthDef[\description] = nil; //archivedSynthDef[\date].asString;
			archivedSynthDef[\comments];
			archivedSynthDef[\rating] = 0;
			archivedSynthDef[\hash] = hash;
			
			archive.add(archivedSynthDef);
			this.save(name.asSymbol);
			this.changed(\defAdded, name.asSymbol, archivedSynthDef);		},{
			detected[\date] = Date.getDate();
			archive.sort();
			this.changed(\defAdded, name.asSymbol, detected);
		})
	}
	
	*delete {
		| name, archivedSynthDef |
		if( this.archiveAt(name.asSymbol).remove(archivedSynthDef).notNil, {
			this.save(name.asSymbol);
			this.changed(\defRemoved, name.asSymbol, archivedSynthDef);
		});
	}
	
	*newArchive {
		^SortedList(10, {
			|a,b| 
			a[\date].rawSeconds > b[\date].rawSeconds 
		})
	}
	
	*save {
		| symbol |
		archives[symbol].writeTextArchive( historyPath +/+ symbol ++ ".synthdefhistory");
	}
	
	*gui {
		SynthDefHistoryWindow.new;
	}
}

+ SynthDef {
	*newTrack {
		arg name, ugenGraphFunc, rates, prependArgs, variants, metadata;
		SynthDefHistory.add( name, ugenGraphFunc, rates, prependArgs, variants, metadata);
		^this.new( name, ugenGraphFunc, rates, prependArgs, variants, metadata );
	}
}
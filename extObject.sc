+Object {
	asTextArchiveUsingEnvir {
		| envir |
		var objects, list, stream, key, skipSlots, requiredObjects, firsttime = true;
		
		envir = envir ? Environment.new;
		skipSlots = IdentityDictionary.new;
		requiredObjects = IdentityDictionary.new;
		
		if (this.archiveAsCompileString) {
			this.checkCanArchive;
			^this.asCompileString ++ "\n"
		};
		
		objects = IdentityDictionary.new;

		this.getContainedObjectsUsingEnvir(objects, envir, this);
		stream = CollStream.new;
		stream << "var o, p;\n";

		list = List.newClear(objects.size);
		objects.keysValuesDo {|obj, index| list[index] = obj };
		
		stream << "o = [";
		list.do {|obj, i|
			var size;
			if (i != 0) { stream << ",  "; };
			if ((i & 3) == 0) { stream << "\n\t" };
			obj.checkCanArchive;
			key = envir.findKeyForValue( obj );
			if( key.notNil && (obj != this), {
				stream << "~" << key.asString;
				skipSlots.put( i, 0 );
			}, {
				if (obj.archiveAsCompileString) {
					stream << obj.asCompileString;
				}{
					size = obj.indexedSize;
					stream << obj.class.name << ".prNew";
					if (size > 0) {
						stream << "(" << size << ")"
					};
				};
			});
		};
		stream << "\n];\np = [";
		// put in slots
		firsttime = true;
		list.do {|obj, i|
			var slots;
			key = envir.findKeyForValue( obj );
			if (obj.archiveAsCompileString.not && (key.isNil || (obj == this) )  ) {
				slots = obj.getSlots;
				if (slots.size > 0) {
					if (firsttime.not) { stream << ",  "; };
					firsttime = false;
					stream << "\n\t// " << obj.class.name;
					stream << "\n\t";
					stream << i << ", [ ";
					if (obj.isKindOf(ArrayedCollection)) {
						slots.do {|slot, j|
							var index;
							if (j != 0) { stream << ",  "; };
							if ((j != 0) && ((j & 3) == 0)) { stream << "\n\t\t" };
							index = objects[slot];
							if (index.isNil) {
								stream << slot.asCompileString;
							}{
								stream << "o[" << index << "]";
							};
						};
					}{
						slots.pairsDo {|key, slot, j|
							var index;
							if (j != 0) { stream << ",  "; };
							if ((j != 0) && ((j & 3) == 0)) { stream << "\n\t\t" };
							stream << key << ": ";
							index = objects[slot];
							if (index.isNil) {
								stream << slot.asCompileString;
							}{
								stream << "o[" << index << "]";
							};
						};
					};
					stream << " ]";
				};
			};
		};
		stream << "\n];\n";
		
		stream << "prUnarchive(o,p);\n";
		^stream.contents
	}

	getContainedObjectsUsingEnvir { arg objects, envir, relativeTo;
		if (objects[this].notNil) {^this};
		objects[this] = objects.size;
		
		if (this.archiveAsCompileString.not) {
			this.slotsDo {|key, slot|
				if (slot.archiveAsObject) {
					slot.getContainedObjectsUsingEnvir(objects, envir, relativeTo);
				};
			};
		};
	}	
}

+ Object {
	envArchiveForOSC {
		| env |
		^this.asTextArchiveUsingEnvir( env?currentEnvironment ).asSymbol
	}
	decodeArchiveFromOSC {
		^this
	}
}

+ SimpleNumber {
	envArchiveForOSC {
		^this
	}
}

+ RawArray {
	envArchiveForOSC {
		^this	// this is a trick. the dispatcher sends the array flat.
				// currently, sc cannot send arrays via OSC
	}
}
+ Symbol {
	decodeArchiveFromOSC {
		| env |
		^( env ? currentEnvironment ).use({
			this.asString.interpret;
		});
	}
}

+ String {
	envArchiveForOSC {
		| env |
		^this.asTextArchiveUsingEnvir( env?currentEnvironment ).asSymbol // otherwise string is indistinguishable
	}
	decodeArchiveFromOSC {
		| env |
		^( env ? currentEnvironment ).use({
			this.interpret;
		});
	}
}
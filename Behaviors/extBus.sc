+Bus {
	asMapM {
		var mapSymbol;
		if(index.isNil) { MethodError("bus not allocated.", this).throw };
		mapSymbol = if(rate == \control) { "c" } { "a" };
		mapSymbol = (mapSymbol ++ index).asSymbol;
		^mapSymbol;
	}
}

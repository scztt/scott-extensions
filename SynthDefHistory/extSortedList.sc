+SortedList {
	*copyInstance {
		arg aList;
		var copy;
		"making copy".postln; 
		copy = SortedList.new(aList.size, aList.function);
		aList.function.asCompileString.postln;
		copy.addAll( aList );
		^copy
	}
}
+ SCListView {
	add {
		| item |
		^this.items_(items ++ [item]);
	}
}
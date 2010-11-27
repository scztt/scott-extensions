+ Object {
	gtrace {
		| name=nil |
		name = name ? this.getBackTrace.address;
		^TraceWindow.guiTrace( name, this.value )
	}
}
